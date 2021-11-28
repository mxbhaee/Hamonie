package com.hamonie.fragments.albums

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hamonie.appthemehelper.common.ATHToolbarActivity.getToolbarBackgroundColor
import com.hamonie.appthemehelper.util.ToolbarContentTintHelper
import com.hamonie.EXTRA_ALBUM_ID
import com.hamonie.EXTRA_ARTIST_ID
import com.hamonie.EXTRA_ARTIST_NAME
import com.hamonie.R
import com.hamonie.activities.tageditor.AbsTagEditorActivity
import com.hamonie.activities.tageditor.AlbumTagEditorActivity
import com.hamonie.adapter.album.HorizontalAlbumAdapter
import com.hamonie.adapter.song.SimpleSongAdapter
import com.hamonie.databinding.FragmentAlbumDetailsBinding
import com.hamonie.dialogs.AddToPlaylistDialog
import com.hamonie.dialogs.DeleteSongsDialog
import com.hamonie.extensions.*
import com.hamonie.fragments.base.AbsMainActivityFragment
import com.hamonie.glide.GlideApp
import com.hamonie.glide.RetroGlideExtension
import com.hamonie.glide.RetroMusicColoredTarget
import com.hamonie.glide.SingleColorTarget
import com.hamonie.helper.MusicPlayerRemote
import com.hamonie.helper.SortOrder.AlbumSongSortOrder.Companion.SONG_A_Z
import com.hamonie.helper.SortOrder.AlbumSongSortOrder.Companion.SONG_DURATION
import com.hamonie.helper.SortOrder.AlbumSongSortOrder.Companion.SONG_TRACK_LIST
import com.hamonie.helper.SortOrder.AlbumSongSortOrder.Companion.SONG_Z_A
import com.hamonie.interfaces.IAlbumClickListener
import com.hamonie.interfaces.ICabCallback
import com.hamonie.interfaces.ICabHolder
import com.hamonie.model.Album
import com.hamonie.model.Artist
import com.hamonie.network.Result
import com.hamonie.network.model.LastFmAlbum
import com.hamonie.repository.RealRepository
import com.hamonie.util.MusicUtil
import com.hamonie.util.PreferenceUtil
import com.hamonie.util.RetroColorUtil
import com.hamonie.util.RetroUtil
import com.hamonie.util.color.MediaNotificationProcessor
import com.afollestad.materialcab.attached.AttachedCab
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialcab.attached.isActive
import com.afollestad.materialcab.createCab
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.util.*

class AlbumDetailsFragment : AbsMainActivityFragment(R.layout.fragment_album_details),
    IAlbumClickListener, ICabHolder {

    private var _binding: FragmentAlbumDetailsBinding? = null
    private val binding get() = _binding!!

    private val arguments by navArgs<AlbumDetailsFragmentArgs>()
    private val detailsViewModel by viewModel<AlbumDetailsViewModel> {
        parametersOf(arguments.extraAlbumId)
    }

    private lateinit var simpleSongAdapter: SimpleSongAdapter
    private lateinit var album: Album
    private var albumArtistExists = false

    private val savedSortOrder: String
        get() = PreferenceUtil.albumDetailSongSortOrder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.fragment_container
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(requireContext().resolveColor(R.attr.colorSurface))
            setPathMotion(MaterialArcMotion())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAlbumDetailsBinding.bind(view)
        setHasOptionsMenu(true)
        mainActivity.addMusicServiceEventListener(detailsViewModel)
        mainActivity.setSupportActionBar(binding.toolbar)

        binding.toolbar.title = " "
        ViewCompat.setTransitionName(binding.albumCoverContainer, arguments.extraAlbumId.toString())
        postponeEnterTransition()
        detailsViewModel.getAlbum().observe(viewLifecycleOwner, {
            requireView().doOnPreDraw {
                startPostponedEnterTransition()
            }
            albumArtistExists = !it.albumArtist.isNullOrEmpty()
            showAlbum(it)
            if (albumArtistExists) {
                ViewCompat.setTransitionName(binding.artistImage, album.albumArtist)
            } else {
                ViewCompat.setTransitionName(binding.artistImage, album.artistId.toString())
            }
        })

        setupRecyclerView()
        binding.artistImage.setOnClickListener { artistView ->
            if (albumArtistExists) {
                findActivityNavController(R.id.fragment_container)
                    .navigate(
                        R.id.albumArtistDetailsFragment,
                        bundleOf(EXTRA_ARTIST_NAME to album.albumArtist),
                        null,
                        FragmentNavigatorExtras(artistView to album.albumArtist.toString())
                    )
            } else {
                findActivityNavController(R.id.fragment_container)
                    .navigate(
                        R.id.artistDetailsFragment,
                        bundleOf(EXTRA_ARTIST_ID to album.artistId),
                        null,
                        FragmentNavigatorExtras(artistView to album.artistId.toString())
                    )
            }

        }
        binding.fragmentAlbumContent.playAction.setOnClickListener {
            MusicPlayerRemote.openQueue(album.songs, 0, true)
        }
        binding.fragmentAlbumContent.shuffleAction.setOnClickListener {
            MusicPlayerRemote.openAndShuffleQueue(
                album.songs,
                true
            )
        }

        binding.fragmentAlbumContent.aboutAlbumText.setOnClickListener {
            if (binding.fragmentAlbumContent.aboutAlbumText.maxLines == 4) {
                binding.fragmentAlbumContent.aboutAlbumText.maxLines = Integer.MAX_VALUE
            } else {
                binding.fragmentAlbumContent.aboutAlbumText.maxLines = 4
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (!handleBackPress()) {
                remove()
                requireActivity().onBackPressed()
            }
        }
        binding.appBarLayout?.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(requireContext())
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceActivity?.removeMusicServiceEventListener(detailsViewModel)
    }

    private fun setupRecyclerView() {
        simpleSongAdapter = SimpleSongAdapter(
            requireActivity() as AppCompatActivity,
            ArrayList(),
            R.layout.item_song,
            this
        )
        binding.fragmentAlbumContent.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = DefaultItemAnimator()
            isNestedScrollingEnabled = false
            adapter = simpleSongAdapter
        }
    }

    private fun showAlbum(album: Album) {
        if (album.songs.isEmpty()) {
            return
        }
        this.album = album

        binding.albumTitle.text = album.title
        val songText = resources.getQuantityString(
            R.plurals.albumSongs,
            album.songCount,
            album.songCount
        )
        binding.fragmentAlbumContent.songTitle.text = songText
        if (MusicUtil.getYearString(album.year) == "-") {
            binding.albumText.text = String.format(
                "%s • %s",
                if (albumArtistExists) album.albumArtist else album.artistName,
                MusicUtil.getReadableDurationString(MusicUtil.getTotalDuration(album.songs))
            )
        } else {
            binding.albumText.text = String.format(
                "%s • %s • %s",
                album.artistName,
                MusicUtil.getYearString(album.year),
                MusicUtil.getReadableDurationString(MusicUtil.getTotalDuration(album.songs))
            )
        }
        loadAlbumCover(album)
        simpleSongAdapter.swapDataSet(album.songs)
        if (albumArtistExists) {
            detailsViewModel.getAlbumArtist(album.albumArtist.toString())
                .observe(viewLifecycleOwner, {
                    loadArtistImage(it)
                })
        } else {
            detailsViewModel.getArtist(album.artistId).observe(viewLifecycleOwner, {
                loadArtistImage(it)
            })
        }


        detailsViewModel.getAlbumInfo(album).observe(viewLifecycleOwner, { result ->
            when (result) {
                is Result.Loading -> {
                    println("Loading")
                }
                is Result.Error -> {
                    println("Error")
                }
                is Result.Success -> {
                    aboutAlbum(result.data)
                }
            }
        })
    }

    private fun moreAlbums(albums: List<Album>) {
        binding.fragmentAlbumContent.moreTitle.show()
        binding.fragmentAlbumContent.moreRecyclerView.show()
        binding.fragmentAlbumContent.moreTitle.text =
            String.format(getString(R.string.label_more_from), album.artistName)

        val albumAdapter =
            HorizontalAlbumAdapter(requireActivity() as AppCompatActivity, albums, this, this)
        binding.fragmentAlbumContent.moreRecyclerView.layoutManager = GridLayoutManager(
            requireContext(),
            1,
            GridLayoutManager.HORIZONTAL,
            false
        )
        binding.fragmentAlbumContent.moreRecyclerView.adapter = albumAdapter
    }

    private fun aboutAlbum(lastFmAlbum: LastFmAlbum) {
        if (lastFmAlbum.album != null) {
            if (lastFmAlbum.album.wiki != null) {
                binding.fragmentAlbumContent.aboutAlbumText.show()
                binding.fragmentAlbumContent.aboutAlbumTitle.show()
                binding.fragmentAlbumContent.aboutAlbumTitle.text =
                    String.format(getString(R.string.about_album_label), lastFmAlbum.album.name)
                binding.fragmentAlbumContent.aboutAlbumText.text = HtmlCompat.fromHtml(
                    lastFmAlbum.album.wiki.content,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            }
            if (lastFmAlbum.album.listeners.isNotEmpty()) {
                binding.fragmentAlbumContent.listeners.show()
                binding.fragmentAlbumContent.listenersLabel.show()
                binding.fragmentAlbumContent.scrobbles.show()
                binding.fragmentAlbumContent.scrobblesLabel.show()

                binding.fragmentAlbumContent.listeners.text =
                    RetroUtil.formatValue(lastFmAlbum.album.listeners.toFloat())
                binding.fragmentAlbumContent.scrobbles.text =
                    RetroUtil.formatValue(lastFmAlbum.album.playcount.toFloat())
            }
        }
    }

    private fun loadArtistImage(artist: Artist) {
        detailsViewModel.getMoreAlbums(artist).observe(viewLifecycleOwner, {
            moreAlbums(it)
        })
        GlideApp.with(requireContext()).asBitmapPalette().artistImageOptions(artist)
            //.forceDownload(PreferenceUtil.isAllowedToDownloadMetadata())
            .load(
                RetroGlideExtension.getArtistModel(
                    artist,
                    PreferenceUtil.isAllowedToDownloadMetadata()
                )
            )
            .dontAnimate()
            .dontTransform()
            .into(object : RetroMusicColoredTarget(binding.artistImage) {
                override fun onColorReady(colors: MediaNotificationProcessor) {
                }
            })
    }

    private fun loadAlbumCover(album: Album) {
        GlideApp.with(requireContext()).asBitmapPalette()
            .albumCoverOptions(album.safeGetFirstSong())
            //.checkIgnoreMediaStore()
            .load(RetroGlideExtension.getSongModel(album.safeGetFirstSong()))
            .into(object : SingleColorTarget(binding.image) {
                override fun onColorReady(color: Int) {
                    setColors(color)
                }
            })
    }

    private fun setColors(color: Int) {
        _binding?.fragmentAlbumContent?.apply {
            shuffleAction.applyColor(color)
            playAction.applyOutlineColor(color)
        }
    }

    override fun onAlbumClick(albumId: Long, view: View) {
        findNavController().navigate(
            R.id.albumDetailsFragment,
            bundleOf(EXTRA_ALBUM_ID to albumId),
            null,
            FragmentNavigatorExtras(
                view to albumId.toString()
            )
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_album_detail, menu)
        val sortOrder = menu.findItem(R.id.action_sort_order)
        setUpSortOrderMenu(sortOrder.subMenu)
        ToolbarContentTintHelper.handleOnCreateOptionsMenu(
            requireContext(),
            binding.toolbar,
            menu,
            getToolbarBackgroundColor(binding.toolbar)
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return handleSortOrderMenuItem(item)
    }

    private fun handleSortOrderMenuItem(item: MenuItem): Boolean {
        var sortOrder: String? = null
        val songs = simpleSongAdapter.dataSet
        when (item.itemId) {
            android.R.id.home -> findNavController().navigateUp()
            R.id.action_play_next -> {
                MusicPlayerRemote.playNext(songs)
                return true
            }
            R.id.action_add_to_current_playing -> {
                MusicPlayerRemote.enqueue(songs)
                return true
            }
            R.id.action_add_to_playlist -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val playlists = get<RealRepository>().fetchPlaylists()
                    withContext(Dispatchers.Main) {
                        AddToPlaylistDialog.create(playlists, songs)
                            .show(childFragmentManager, "ADD_PLAYLIST")
                    }
                }
                return true
            }
            R.id.action_delete_from_device -> {
                DeleteSongsDialog.create(songs).show(childFragmentManager, "DELETE_SONGS")
                return true
            }
            R.id.action_tag_editor -> {
                val intent = Intent(requireContext(), AlbumTagEditorActivity::class.java)
                intent.putExtra(AbsTagEditorActivity.EXTRA_ID, album.id)
                val options = ActivityOptions.makeSceneTransitionAnimation(
                    requireActivity(),
                    binding.albumCoverContainer,
                    "${getString(R.string.transition_album_art)}_${album.id}"
                )
                startActivityForResult(
                    intent,
                    TAG_EDITOR_REQUEST, options.toBundle()
                )
                return true
            }
            R.id.action_sort_order_title -> sortOrder = SONG_A_Z
            R.id.action_sort_order_title_desc -> sortOrder = SONG_Z_A
            R.id.action_sort_order_track_list -> sortOrder = SONG_TRACK_LIST
            R.id.action_sort_order_artist_song_duration -> sortOrder = SONG_DURATION
        }
        if (sortOrder != null) {
            item.isChecked = true
            setSaveSortOrder(sortOrder)
        }
        return true
    }

    private fun setUpSortOrderMenu(sortOrder: SubMenu) {
        when (savedSortOrder) {
            SONG_A_Z -> sortOrder.findItem(R.id.action_sort_order_title).isChecked = true
            SONG_Z_A -> sortOrder.findItem(R.id.action_sort_order_title_desc).isChecked = true
            SONG_TRACK_LIST ->
                sortOrder.findItem(R.id.action_sort_order_track_list).isChecked = true
            SONG_DURATION ->
                sortOrder.findItem(R.id.action_sort_order_artist_song_duration).isChecked = true
        }
    }

    private fun setSaveSortOrder(sortOrder: String) {
        PreferenceUtil.albumDetailSongSortOrder = sortOrder
        val songs = when (sortOrder) {
            SONG_TRACK_LIST -> album.songs.sortedWith { o1, o2 ->
                o1.trackNumber.compareTo(
                    o2.trackNumber
                )
            }
            SONG_A_Z -> album.songs.sortedWith { o1, o2 ->
                o1.title.compareTo(
                    o2.title
                )
            }
            SONG_Z_A -> album.songs.sortedWith { o1, o2 ->
                o2.title.compareTo(
                    o1.title
                )
            }
            SONG_DURATION -> album.songs.sortedWith { o1, o2 ->
                o1.duration.compareTo(
                    o2.duration
                )
            }
            else -> throw IllegalArgumentException("invalid $sortOrder")
        }
        album = album.copy(songs = songs)
        simpleSongAdapter.swapDataSet(album.songs)
    }

    private fun handleBackPress(): Boolean {
        cab?.let {
            if (it.isActive()) {
                it.destroy()
                return true
            }
        }
        return false
    }

    private var cab: AttachedCab? = null

    override fun openCab(menuRes: Int, callback: ICabCallback): AttachedCab {
        cab?.let {
            if (it.isActive()) {
                it.destroy()
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG_EDITOR_REQUEST = 9002
    }
}
