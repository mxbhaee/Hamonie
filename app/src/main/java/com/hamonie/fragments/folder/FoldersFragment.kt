package com.hamonie.fragments.folder

import android.app.Dialog
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hamonie.appthemehelper.ThemeStore.Companion.accentColor
import com.hamonie.appthemehelper.common.ATHToolbarActivity
import com.hamonie.appthemehelper.util.ATHUtil.resolveColor
import com.hamonie.appthemehelper.util.ToolbarContentTintHelper
import com.hamonie.App
import com.hamonie.R
import com.hamonie.adapter.SongFileAdapter
import com.hamonie.adapter.Storage
import com.hamonie.adapter.StorageAdapter
import com.hamonie.adapter.StorageClickListener
import com.hamonie.databinding.FragmentFolderBinding
import com.hamonie.extensions.drawNextToNavbar
import com.hamonie.extensions.navigate
import com.hamonie.extensions.surfaceColor
import com.hamonie.fragments.base.AbsMainActivityFragment
import com.hamonie.fragments.folder.FoldersFragment.ListPathsAsyncTask.OnPathsListedCallback
import com.hamonie.fragments.folder.FoldersFragment.ListSongsAsyncTask.OnSongsListedCallback
import com.hamonie.helper.MusicPlayerRemote.openQueue
import com.hamonie.helper.MusicPlayerRemote.playingQueue
import com.hamonie.helper.menu.SongMenuHelper.handleMenuClick
import com.hamonie.helper.menu.SongsMenuHelper
import com.hamonie.interfaces.ICabCallback
import com.hamonie.interfaces.ICabHolder
import com.hamonie.interfaces.ICallbacks
import com.hamonie.interfaces.IMainActivityFragmentCallbacks
import com.hamonie.misc.DialogAsyncTask
import com.hamonie.misc.UpdateToastMediaScannerCompletionListener
import com.hamonie.misc.WrappedAsyncTaskLoader
import com.hamonie.model.Song
import com.hamonie.providers.BlacklistStore
import com.hamonie.util.*
import com.hamonie.util.DensityUtil.dip2px
import com.hamonie.util.PreferenceUtil.startDirectory
import com.hamonie.util.ThemedFastScroller.create
import com.hamonie.views.BreadCrumbLayout.Crumb
import com.hamonie.views.BreadCrumbLayout.SelectionCallback
import com.hamonie.views.ScrollingViewOnApplyWindowInsetsListener
import com.afollestad.materialcab.attached.AttachedCab
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialcab.attached.isActive
import com.afollestad.materialcab.createCab
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import java.io.*
import java.lang.ref.WeakReference
import java.util.*

class FoldersFragment : AbsMainActivityFragment(R.layout.fragment_folder),
    IMainActivityFragmentCallbacks, ICabHolder, SelectionCallback, ICallbacks,
    LoaderManager.LoaderCallbacks<List<File>>, StorageClickListener {
    private var _binding: FragmentFolderBinding? = null
    private val binding get() = _binding!!
    private var adapter: SongFileAdapter? = null
    private var storageAdapter: StorageAdapter? = null
    private var cab: AttachedCab? = null
    private val fileComparator = Comparator { lhs: File, rhs: File ->
        if (lhs.isDirectory && !rhs.isDirectory) {
            return@Comparator -1
        } else if (!lhs.isDirectory && rhs.isDirectory) {
            return@Comparator 1
        } else {
            return@Comparator lhs.name.compareTo(rhs.name, ignoreCase = true)
        }
    }
    private var storageItems = ArrayList<Storage>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFolderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainActivity.addMusicServiceEventListener(libraryViewModel)
        mainActivity.setSupportActionBar(binding.toolbar)
        mainActivity.supportActionBar?.title = null
        enterTransition = MaterialFadeThrough().apply {
            addTarget(binding.recyclerView)
        }
        setUpBreadCrumbs()
        setUpRecyclerView()
        setUpAdapter()
        setUpTitle()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!handleBackPress()) {
                        remove()
                        mainActivity.finish()
                    }
                }
            })
        binding.toolbarContainer.drawNextToNavbar()
        binding.appBarLayout.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(requireContext())
    }

    private fun setUpTitle() {
        binding.toolbar.setNavigationOnClickListener { v: View? ->
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).setDuration(300)
            reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).setDuration(300)
            findNavController(v!!).navigate(R.id.searchFragment, null, navOptions)
        }
        binding.appNameText.text = resources.getString(R.string.folders)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        if (savedInstanceState == null) {
            switchToFileAdapter()
            setCrumb(
                Crumb(
                    FileUtil.safeGetCanonicalFile(startDirectory)
                ),
                true
            )
        } else {
            binding.breadCrumbs.restoreFromStateWrapper(savedInstanceState.getParcelable(CRUMBS))
            LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this)
        }
    }

    override fun onPause() {
        super.onPause()
        saveScrollPosition()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (_binding != null) {
            outState.putParcelable(CRUMBS, binding.breadCrumbs.stateWrapper)
        }
    }

    override fun handleBackPress(): Boolean {
        if (cab != null && cab!!.isActive()) {
            cab?.destroy()
            return true
        }
        if (binding.breadCrumbs.popHistory()) {
            setCrumb(binding.breadCrumbs.lastHistory(), false)
            return true
        }
        return false
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<File>> {
        return AsyncFileLoader(this)
    }

    override fun onCrumbSelection(crumb: Crumb, index: Int) {
        setCrumb(crumb, true)
    }

    override fun onFileMenuClicked(file: File, view: View) {
        val popupMenu = PopupMenu(activity, view)
        if (file.isDirectory) {
            popupMenu.inflate(R.menu.menu_item_directory)
            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                when (val itemId = item.itemId) {
                    R.id.action_play_next, R.id.action_add_to_current_playing, R.id.action_add_to_playlist, R.id.action_delete_from_device -> {
                        ListSongsAsyncTask(
                            activity,
                            null,
                            object : OnSongsListedCallback {
                                override fun onSongsListed(songs: List<Song>, extra: Any?) {
                                    if (songs.isNotEmpty()) {
                                        SongsMenuHelper.handleMenuClick(
                                            requireActivity(), songs, itemId
                                        )
                                    }
                                }
                            })
                            .execute(
                                ListSongsAsyncTask.LoadingInfo(
                                    toList(file), AUDIO_FILE_FILTER, fileComparator
                                )
                            )
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_add_to_blacklist -> {
                        BlacklistStore.getInstance(App.getContext()).addPath(file)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_set_as_start_directory -> {
                        startDirectory = file
                        Toast.makeText(
                            activity,
                            String.format(getString(R.string.new_start_directory), file.path),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_scan -> {
                        ListPathsAsyncTask(
                            activity,
                            object : OnPathsListedCallback {
                                override fun onPathsListed(paths: Array<String?>) {
                                    scanPaths(paths)
                                }
                            })
                            .execute(ListPathsAsyncTask.LoadingInfo(file, AUDIO_FILE_FILTER))
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }
        } else {
            popupMenu.inflate(R.menu.menu_item_file)
            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                when (val itemId = item.itemId) {
                    R.id.action_play_next, R.id.action_add_to_current_playing, R.id.action_add_to_playlist, R.id.action_go_to_album, R.id.action_go_to_artist, R.id.action_share, R.id.action_tag_editor, R.id.action_details, R.id.action_set_as_ringtone, R.id.action_delete_from_device -> {
                        ListSongsAsyncTask(
                            activity,
                            null,
                            object : OnSongsListedCallback {
                                override fun onSongsListed(songs: List<Song>, extra: Any?) {
                                    handleMenuClick(
                                        requireActivity(), songs[0], itemId
                                    )
                                }
                            })
                            .execute(
                                ListSongsAsyncTask.LoadingInfo(
                                    toList(file), AUDIO_FILE_FILTER, fileComparator
                                )
                            )
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_scan -> {
                        ListPathsAsyncTask(
                            activity,
                            object : OnPathsListedCallback {
                                override fun onPathsListed(paths: Array<String?>) {
                                    scanPaths(paths)
                                }
                            })
                            .execute(ListPathsAsyncTask.LoadingInfo(file, AUDIO_FILE_FILTER))
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }
        }
        popupMenu.show()
    }

    override fun onFileSelected(file: File) {
        var mFile = file
        mFile = tryGetCanonicalFile(mFile) // important as we compare the path value later
        if (mFile.isDirectory) {
            setCrumb(Crumb(mFile), true)
        } else {
            val fileFilter = FileFilter { pathname: File ->
                !pathname.isDirectory && AUDIO_FILE_FILTER.accept(pathname)
            }
            ListSongsAsyncTask(
                activity,
                mFile,
                object : OnSongsListedCallback {
                    override fun onSongsListed(songs: List<Song>, extra: Any?) {
                        val file1 = extra as File
                        var startIndex = -1
                        for (i in songs.indices) {
                            if (file1
                                    .path
                                == songs[i].data
                            ) { // path is already canonical here
                                startIndex = i
                                break
                            }
                        }
                        if (startIndex > -1) {
                            openQueue(songs, startIndex, true)
                        } else {
                            Snackbar.make(
                                binding.root,
                                Html.fromHtml(
                                    String.format(
                                        getString(R.string.not_listed_in_media_store), file1.name
                                    )
                                ),
                                Snackbar.LENGTH_LONG
                            )
                                .setAction(
                                    R.string.action_scan
                                ) {
                                    ListPathsAsyncTask(
                                        requireActivity(),
                                        object : OnPathsListedCallback {
                                            override fun onPathsListed(paths: Array<String?>) {
                                                scanPaths(paths)
                                            }
                                        })
                                        .execute(
                                            ListPathsAsyncTask.LoadingInfo(
                                                file1, AUDIO_FILE_FILTER
                                            )
                                        )
                                }
                                .setActionTextColor(accentColor(requireActivity()))
                                .show()
                        }
                    }
                })
                .execute(
                    ListSongsAsyncTask.LoadingInfo(
                        toList(mFile.parentFile), fileFilter, fileComparator
                    )
                )
        }
    }

    override fun onLoadFinished(loader: Loader<List<File>>, data: List<File>) {
        updateAdapter(data)
    }

    override fun onLoaderReset(loader: Loader<List<File>>) {
        updateAdapter(LinkedList())
    }

    override fun onMultipleItemAction(item: MenuItem, files: ArrayList<File>) {
        val itemId = item.itemId
        ListSongsAsyncTask(
            activity,
            null,
            object : OnSongsListedCallback {
                override fun onSongsListed(songs: List<Song>, extra: Any?) {
                    SongsMenuHelper.handleMenuClick(
                        requireActivity(),
                        songs,
                        itemId
                    )
                }
            })
            .execute(ListSongsAsyncTask.LoadingInfo(files, AUDIO_FILE_FILTER, fileComparator))
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        ToolbarContentTintHelper.handleOnPrepareOptionsMenu(requireActivity(), binding.toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.add(0, R.id.action_scan, 0, R.string.scan_media)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(0, R.id.action_go_to_start_directory, 1, R.string.action_go_to_start_directory)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.removeItem(R.id.action_grid_size)
        menu.removeItem(R.id.action_layout_type)
        menu.removeItem(R.id.action_sort_order)
        ToolbarContentTintHelper.handleOnCreateOptionsMenu(
            requireContext(), binding.toolbar, menu, ATHToolbarActivity.getToolbarBackgroundColor(
                binding.toolbar
            )
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_go_to_start_directory -> {
                setCrumb(
                    Crumb(
                        tryGetCanonicalFile(startDirectory)
                    ),
                    true
                )
                return true
            }
            R.id.action_scan -> {
                val crumb = activeCrumb
                if (crumb != null) {
                    ListPathsAsyncTask(
                        activity,
                        object : OnPathsListedCallback {
                            override fun onPathsListed(paths: Array<String?>) {
                                scanPaths(paths)
                            }
                        })
                        .execute(ListPathsAsyncTask.LoadingInfo(crumb.file, AUDIO_FILE_FILTER))
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onQueueChanged() {
        super.onQueueChanged()
        checkForPadding()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        checkForPadding()
    }

    override fun openCab(menuRes: Int, callback: ICabCallback): AttachedCab {
        if (cab != null && cab!!.isActive()) {
            cab?.destroy()
        }
        cab = createCab(R.id.toolbar_container) {
            menu(menuRes)
            closeDrawable(R.drawable.ic_close)
            backgroundColor(literal = RetroColorUtil.shiftBackgroundColor(surfaceColor()))
            slideDown()
            onCreate { cab, menu -> callback.onCabCreated(cab, menu) }
            onSelection {
                callback.onCabItemClicked(it)
            }
            onDestroy { callback.onCabFinished(it) }
        }
        return cab as AttachedCab
    }

    private fun checkForPadding() {
        val count = adapter?.itemCount ?: 0
        if (_binding != null) {
            val params = binding.root.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = if (count > 0 && playingQueue.isNotEmpty()) dip2px(
                requireContext(),
                104f
            ) else dip2px(requireContext(), 54f)
            binding.root.layoutParams = params
        }
    }

    private fun checkIsEmpty() {
        if (_binding != null) {
            binding.emptyEmoji.text = getEmojiByUnicode(0x1F631)
            binding.empty.visibility =
                if (adapter == null || adapter!!.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    private val activeCrumb: Crumb?
        get() = if (_binding != null) {
            if (binding.breadCrumbs.size() > 0) binding.breadCrumbs.getCrumb(binding.breadCrumbs.activeIndex) else null
        } else null

    private fun getEmojiByUnicode(unicode: Int): String {
        return String(Character.toChars(unicode))
    }

    private fun saveScrollPosition() {
        val crumb = activeCrumb
        if (crumb != null) {
            crumb.scrollPosition =
                (binding.recyclerView.layoutManager as LinearLayoutManager?)!!.findFirstVisibleItemPosition()
        }
    }

    private fun scanPaths(toBeScanned: Array<String?>) {
        if (activity == null) {
            return
        }
        if (toBeScanned.isEmpty()) {
            Toast.makeText(activity, R.string.nothing_to_scan, Toast.LENGTH_SHORT).show()
        } else {
            MediaScannerConnection.scanFile(
                requireContext(),
                toBeScanned,
                null,
                UpdateToastMediaScannerCompletionListener(activity, listOf(*toBeScanned))
            )
        }
    }

    private fun setCrumb(crumb: Crumb?, addToHistory: Boolean) {
        if (crumb == null) {
            return
        }
        val path = crumb.file.path
        if (path == "/" || path == "/storage" || path == "/storage/emulated") {
            switchToStorageAdapter()
        } else {
            saveScrollPosition()
            binding.breadCrumbs.setActiveOrAdd(crumb, false)
            if (addToHistory) {
                binding.breadCrumbs.addHistory(crumb)
            }
            LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this)
        }
    }

    private fun setUpAdapter() {
        switchToFileAdapter()
    }

    private fun setUpBreadCrumbs() {
        binding.breadCrumbs.setActivatedContentColor(
            resolveColor(requireContext(), android.R.attr.textColorPrimary)
        )
        binding.breadCrumbs.setDeactivatedContentColor(
            resolveColor(requireContext(), android.R.attr.textColorSecondary)
        )
        binding.breadCrumbs.setCallback(this)
    }

    private fun setUpRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(
            activity
        )
        val fastScroller = create(
            binding.recyclerView
        )
        binding.recyclerView.setOnApplyWindowInsetsListener(
            ScrollingViewOnApplyWindowInsetsListener(binding.recyclerView, fastScroller)
        )
    }

    private fun toList(file: File): ArrayList<File> {
        val files = ArrayList<File>(1)
        files.add(file)
        return files
    }

    private fun updateAdapter(files: List<File>) {
        adapter?.swapDataSet(files)
        val crumb = activeCrumb
        if (crumb != null) {
            (binding.recyclerView.layoutManager as LinearLayoutManager?)
                ?.scrollToPositionWithOffset(crumb.scrollPosition, 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class ListPathsAsyncTask(context: Context?, callback: OnPathsListedCallback) :
        ListingFilesDialogAsyncTask<ListPathsAsyncTask.LoadingInfo, String?, Array<String?>>(
            context
        ) {
        private val onPathsListedCallbackWeakReference: WeakReference<OnPathsListedCallback> =
            WeakReference(callback)

        override fun doInBackground(vararg params: LoadingInfo): Array<String?> {
            return try {
                if (isCancelled || checkCallbackReference() == null) {
                    return arrayOf()
                }
                val info = params[0]
                val paths: Array<String?>
                if (info.file.isDirectory) {
                    val files = FileUtil.listFilesDeep(info.file, info.fileFilter)
                    if (isCancelled || checkCallbackReference() == null) {
                        return arrayOf()
                    }
                    paths = arrayOfNulls(files.size)
                    for (i in files.indices) {
                        val f = files[i]
                        paths[i] = FileUtil.safeGetCanonicalPath(f)
                        if (isCancelled || checkCallbackReference() == null) {
                            return arrayOf()
                        }
                    }
                } else {
                    paths = arrayOfNulls(1)
                    paths[0] = info.file.path
                }
                paths
            } catch (e: Exception) {
                e.printStackTrace()
                cancel(false)
                arrayOf()
            }
        }

        override fun onPostExecute(paths: Array<String?>) {
            super.onPostExecute(paths)
            checkCallbackReference()?.onPathsListed(paths)
        }

        override fun onPreExecute() {
            super.onPreExecute()
            checkCallbackReference()
        }

        private fun checkCallbackReference(): OnPathsListedCallback? {
            val callback = onPathsListedCallbackWeakReference.get()
            if (callback == null) {
                cancel(false)
            }
            return callback
        }

        interface OnPathsListedCallback {
            fun onPathsListed(paths: Array<String?>)
        }

        class LoadingInfo(val file: File, val fileFilter: FileFilter)

    }

    private class AsyncFileLoader(foldersFragment: FoldersFragment) :
        WrappedAsyncTaskLoader<List<File>>(foldersFragment.requireActivity()) {
        private val fragmentWeakReference: WeakReference<FoldersFragment> =
            WeakReference(foldersFragment)

        override fun loadInBackground(): List<File> {
            val foldersFragment = fragmentWeakReference.get()
            var directory: File? = null
            if (foldersFragment != null) {
                val crumb = foldersFragment.activeCrumb
                if (crumb != null) {
                    directory = crumb.file
                }
            }
            return if (directory != null) {
                val files = FileUtil.listFiles(
                    directory,
                    AUDIO_FILE_FILTER
                )
                Collections.sort(files, foldersFragment!!.fileComparator)
                files
            } else {
                LinkedList()
            }
        }

    }

    private open class ListSongsAsyncTask(
        context: Context?,
        private val extra: Any?,
        callback: OnSongsListedCallback
    ) : ListingFilesDialogAsyncTask<ListSongsAsyncTask.LoadingInfo, Void, List<Song>>(context) {
        private val callbackWeakReference = WeakReference(callback)
        private val contextWeakReference = WeakReference(context)
        override fun doInBackground(vararg params: LoadingInfo): List<Song> {
            return try {
                val info = params[0]
                val files = FileUtil.listFilesDeep(info.files, info.fileFilter)
                if (isCancelled || checkContextReference() == null || checkCallbackReference() == null) {
                    return emptyList()
                }
                Collections.sort(files, info.fileComparator)
                val context = checkContextReference()
                if (isCancelled || context == null || checkCallbackReference() == null) {
                    emptyList()
                } else FileUtil.matchFilesWithMediaStore(context, files)
            } catch (e: Exception) {
                e.printStackTrace()
                cancel(false)
                emptyList()
            }
        }

        override fun onPostExecute(songs: List<Song>) {
            super.onPostExecute(songs)
            checkCallbackReference()?.onSongsListed(songs, extra)
        }

        override fun onPreExecute() {
            super.onPreExecute()
            checkCallbackReference()
            checkContextReference()
        }

        private fun checkCallbackReference(): OnSongsListedCallback? {
            val callback = callbackWeakReference.get()
            if (callback == null) {
                cancel(false)
            }
            return callback
        }

        private fun checkContextReference(): Context? {
            val context = contextWeakReference.get()
            if (context == null) {
                cancel(false)
            }
            return context
        }

        interface OnSongsListedCallback {
            fun onSongsListed(songs: List<Song>, extra: Any?)
        }

        class LoadingInfo(
            val files: List<File>,
            val fileFilter: FileFilter,
            val fileComparator: Comparator<File>
        )

    }

    abstract class ListingFilesDialogAsyncTask<Params, Progress, Result> :
        DialogAsyncTask<Params, Progress, Result> {
        internal constructor(context: Context?) : super(context)

        override fun createDialog(context: Context): Dialog {
            return MaterialAlertDialogBuilder(context)
                .setTitle(R.string.listing_files)
                .setCancelable(false)
                .setView(R.layout.loading)
                .setOnCancelListener { cancel(false) }
                .setOnDismissListener { cancel(false) }
                .create()
        }
    }

    override fun onStorageClicked(storage: Storage) {
        switchToFileAdapter()
        setCrumb(
            Crumb(
                FileUtil.safeGetCanonicalFile(storage.file)
            ),
            true
        )
    }

    private fun switchToFileAdapter() {
        adapter = SongFileAdapter(mainActivity, LinkedList(), R.layout.item_list, this, this)
        adapter!!.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    super.onChanged()
                    checkIsEmpty()
                    checkForPadding()
                }
            })
        binding.recyclerView.adapter = adapter
        checkIsEmpty()
    }

    private fun switchToStorageAdapter() {
        storageItems = FileUtil.listRoots()
        storageAdapter = StorageAdapter(storageItems, this)
        binding.recyclerView.adapter = storageAdapter
        binding.breadCrumbs.clearCrumbs()
    }

    companion object {
        val TAG: String = FoldersFragment::class.java.simpleName
        val AUDIO_FILE_FILTER = FileFilter { file: File ->
            (!file.isHidden
                    && (file.isDirectory
                    || FileUtil.fileIsMimeType(file, "audio/*", MimeTypeMap.getSingleton())
                    || FileUtil.fileIsMimeType(file, "application/opus", MimeTypeMap.getSingleton())
                    || FileUtil.fileIsMimeType(
                file,
                "application/ogg",
                MimeTypeMap.getSingleton()
            )))
        }
        private const val CRUMBS = "crumbs"
        private const val LOADER_ID = 5

        // root
        val defaultStartDirectory: File
            get() {
                val musicDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val startFolder = if (musicDir.exists() && musicDir.isDirectory) {
                    musicDir
                } else {
                    val externalStorage = Environment.getExternalStorageDirectory()
                    if (externalStorage.exists() && externalStorage.isDirectory) {
                        externalStorage
                    } else {
                        File("/") // root
                    }
                }
                return startFolder
            }

        private fun tryGetCanonicalFile(file: File): File {
            return try {
                file.canonicalFile
            } catch (e: IOException) {
                e.printStackTrace()
                file
            }
        }
    }
}