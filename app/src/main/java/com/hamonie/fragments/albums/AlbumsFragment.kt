package com.hamonie.fragments.albums

import android.os.Bundle
import android.view.*
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.hamonie.EXTRA_ALBUM_ID
import com.hamonie.R
import com.hamonie.adapter.album.AlbumAdapter
import com.hamonie.extensions.navigate
import com.hamonie.extensions.surfaceColor
import com.hamonie.fragments.ReloadType
import com.hamonie.fragments.base.AbsRecyclerViewCustomGridSizeFragment
import com.hamonie.helper.MusicPlayerRemote
import com.hamonie.helper.SortOrder.AlbumSortOrder
import com.hamonie.interfaces.IAlbumClickListener
import com.hamonie.interfaces.ICabCallback
import com.hamonie.interfaces.ICabHolder
import com.hamonie.service.MusicService
import com.hamonie.util.PreferenceUtil
import com.hamonie.util.RetroColorUtil
import com.hamonie.util.RetroUtil
import com.afollestad.materialcab.attached.AttachedCab
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialcab.attached.isActive
import com.afollestad.materialcab.createCab
import com.google.android.gms.cast.framework.CastButtonFactory

class AlbumsFragment : AbsRecyclerViewCustomGridSizeFragment<AlbumAdapter, GridLayoutManager>(),
    IAlbumClickListener, ICabHolder {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        libraryViewModel.getAlbums().observe(viewLifecycleOwner, {
            if (it.isNotEmpty())
                adapter?.swapDataSet(it)
            else
                adapter?.swapDataSet(listOf())
        })
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (!handleBackPress()) {
                remove()
                mainActivity.finish()
            }
        }
    }

    override val titleRes: Int
        get() = R.string.albums

    override val emptyMessage: Int
        get() = R.string.no_albums

    override val isShuffleVisible: Boolean
        get() = true

    override fun onShuffleClicked() {
        libraryViewModel.getAlbums().value?.let {
            MusicPlayerRemote.setShuffleMode(MusicService.SHUFFLE_MODE_NONE)
            MusicPlayerRemote.openQueue(
                queue = it.shuffled().flatMap { album -> album.songs },
                startPosition = 0,
                startPlaying = true
            )
        }
    }

    override fun createLayoutManager(): GridLayoutManager {
        return GridLayoutManager(requireActivity(), getGridSize())
    }

    override fun createAdapter(): AlbumAdapter {
        val dataSet = if (adapter == null) ArrayList() else adapter!!.dataSet
        return AlbumAdapter(
            requireActivity(),
            dataSet,
            itemLayoutRes(),
            this,
            this
        )
    }

    override fun setGridSize(gridSize: Int) {
        layoutManager?.spanCount = gridSize
        adapter?.notifyDataSetChanged()
    }

    override fun loadSortOrder(): String {
        return PreferenceUtil.albumSortOrder
    }

    override fun saveSortOrder(sortOrder: String) {
        PreferenceUtil.albumSortOrder = sortOrder
    }

    override fun loadGridSize(): Int {
        return PreferenceUtil.albumGridSize
    }

    override fun saveGridSize(gridColumns: Int) {
        PreferenceUtil.albumGridSize = gridColumns
    }

    override fun loadGridSizeLand(): Int {
        return PreferenceUtil.albumGridSizeLand
    }

    override fun saveGridSizeLand(gridColumns: Int) {
        PreferenceUtil.albumGridSizeLand = gridColumns
    }

    override fun setSortOrder(sortOrder: String) {
        libraryViewModel.forceReload(ReloadType.Albums)
    }

    override fun loadLayoutRes(): Int {
        return PreferenceUtil.albumGridStyle
    }

    override fun saveLayoutRes(layoutRes: Int) {
        PreferenceUtil.albumGridStyle = layoutRes
    }

    companion object {
        fun newInstance(): AlbumsFragment {
            return AlbumsFragment()
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
        reenterTransition = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val gridSizeItem: MenuItem = menu.findItem(R.id.action_grid_size)
        if (RetroUtil.isLandscape()) {
            gridSizeItem.setTitle(R.string.action_grid_size_land)
        }
        setUpGridSizeMenu(gridSizeItem.subMenu)
        val layoutItem = menu.findItem(R.id.action_layout_type)
        setupLayoutMenu(layoutItem.subMenu)
        setUpSortOrderMenu(menu.findItem(R.id.action_sort_order).subMenu)
        //Setting up cast button
        CastButtonFactory.setUpMediaRouteButton(requireContext(), menu, R.id.action_cast)
    }

    private fun setUpSortOrderMenu(
        sortOrderMenu: SubMenu
    ) {
        val currentSortOrder: String? = getSortOrder()
        sortOrderMenu.clear()
        sortOrderMenu.add(
            0,
            R.id.action_album_sort_order_asc,
            0,
            R.string.sort_order_a_z
        ).isChecked =
            currentSortOrder.equals(AlbumSortOrder.ALBUM_A_Z)
        sortOrderMenu.add(
            0,
            R.id.action_album_sort_order_desc,
            1,
            R.string.sort_order_z_a
        ).isChecked =
            currentSortOrder.equals(AlbumSortOrder.ALBUM_Z_A)
        sortOrderMenu.add(
            0,
            R.id.action_album_sort_order_artist,
            2,
            R.string.sort_order_artist
        ).isChecked =
            currentSortOrder.equals(AlbumSortOrder.ALBUM_ARTIST)
        sortOrderMenu.add(
            0,
            R.id.action_album_sort_order_year,
            3,
            R.string.sort_order_year
        ).isChecked =
            currentSortOrder.equals(AlbumSortOrder.ALBUM_YEAR)
        sortOrderMenu.add(
            0,
            R.id.action_album_sort_order_num_songs,
            4,
            R.string.sort_order_num_songs
        ).isChecked =
            currentSortOrder.equals(AlbumSortOrder.ALBUM_NUMBER_OF_SONGS)

        sortOrderMenu.setGroupCheckable(0, true, true)
    }

    private fun setupLayoutMenu(
        subMenu: SubMenu
    ) {
        when (itemLayoutRes()) {
            R.layout.item_card -> subMenu.findItem(R.id.action_layout_card).isChecked = true
            R.layout.item_grid -> subMenu.findItem(R.id.action_layout_normal).isChecked = true
            R.layout.item_card_color ->
                subMenu.findItem(R.id.action_layout_colored_card).isChecked = true
            R.layout.item_grid_circle ->
                subMenu.findItem(R.id.action_layout_circular).isChecked = true
            R.layout.image -> subMenu.findItem(R.id.action_layout_image).isChecked = true
            R.layout.item_image_gradient ->
                subMenu.findItem(R.id.action_layout_gradient_image).isChecked = true
        }
    }

    private fun setUpGridSizeMenu(
        gridSizeMenu: SubMenu
    ) {
        when (getGridSize()) {
            1 -> gridSizeMenu.findItem(R.id.action_grid_size_1).isChecked =
                true
            2 -> gridSizeMenu.findItem(R.id.action_grid_size_2).isChecked = true
            3 -> gridSizeMenu.findItem(R.id.action_grid_size_3).isChecked = true
            4 -> gridSizeMenu.findItem(R.id.action_grid_size_4).isChecked = true
            5 -> gridSizeMenu.findItem(R.id.action_grid_size_5).isChecked = true
            6 -> gridSizeMenu.findItem(R.id.action_grid_size_6).isChecked = true
            7 -> gridSizeMenu.findItem(R.id.action_grid_size_7).isChecked = true
            8 -> gridSizeMenu.findItem(R.id.action_grid_size_8).isChecked = true
        }
        val gridSize: Int = maxGridSize
        if (gridSize < 8) {
            gridSizeMenu.findItem(R.id.action_grid_size_8).isVisible = false
        }
        if (gridSize < 7) {
            gridSizeMenu.findItem(R.id.action_grid_size_7).isVisible = false
        }
        if (gridSize < 6) {
            gridSizeMenu.findItem(R.id.action_grid_size_6).isVisible = false
        }
        if (gridSize < 5) {
            gridSizeMenu.findItem(R.id.action_grid_size_5).isVisible = false
        }
        if (gridSize < 4) {
            gridSizeMenu.findItem(R.id.action_grid_size_4).isVisible = false
        }
        if (gridSize < 3) {
            gridSizeMenu.findItem(R.id.action_grid_size_3).isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (handleGridSizeMenuItem(item)) {
            return true
        }
        if (handleLayoutResType(item)) {
            return true
        }
        if (handleSortOrderMenuItem(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleSortOrderMenuItem(
        item: MenuItem
    ): Boolean {
        val sortOrder: String = when (item.itemId) {
            R.id.action_album_sort_order_asc -> AlbumSortOrder.ALBUM_A_Z
            R.id.action_album_sort_order_desc -> AlbumSortOrder.ALBUM_Z_A
            R.id.action_album_sort_order_artist -> AlbumSortOrder.ALBUM_ARTIST
            R.id.action_album_sort_order_year -> AlbumSortOrder.ALBUM_YEAR
            R.id.action_album_sort_order_num_songs -> AlbumSortOrder.ALBUM_NUMBER_OF_SONGS
            else -> PreferenceUtil.albumSortOrder
        }
        if (sortOrder != PreferenceUtil.albumSortOrder) {
            item.isChecked = true
            setAndSaveSortOrder(sortOrder)
            return true
        }
        return false
    }

    private fun handleLayoutResType(
        item: MenuItem
    ): Boolean {
        val layoutRes = when (item.itemId) {
            R.id.action_layout_normal -> R.layout.item_grid
            R.id.action_layout_card -> R.layout.item_card
            R.id.action_layout_colored_card -> R.layout.item_card_color
            R.id.action_layout_circular -> R.layout.item_grid_circle
            R.id.action_layout_image -> R.layout.image
            R.id.action_layout_gradient_image -> R.layout.item_image_gradient
            else -> PreferenceUtil.albumGridStyle
        }
        if (layoutRes != PreferenceUtil.albumGridStyle) {
            item.isChecked = true
            setAndSaveLayoutRes(layoutRes)
            return true
        }
        return false
    }

    private fun handleGridSizeMenuItem(
        item: MenuItem
    ): Boolean {
        val gridSize = when (item.itemId) {
            R.id.action_grid_size_1 -> 1
            R.id.action_grid_size_2 -> 2
            R.id.action_grid_size_3 -> 3
            R.id.action_grid_size_4 -> 4
            R.id.action_grid_size_5 -> 5
            R.id.action_grid_size_6 -> 6
            R.id.action_grid_size_7 -> 7
            R.id.action_grid_size_8 -> 8
            else -> 0
        }
        if (gridSize > 0) {
            item.isChecked = true
            setAndSaveGridSize(gridSize)
            return true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Albums)
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
            println("Cab")
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
}
