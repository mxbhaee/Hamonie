package com.hamonie.adapter.album

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import com.hamonie.R
import com.hamonie.adapter.base.AbsMultiSelectAdapter
import com.hamonie.adapter.base.MediaEntryViewHolder
import com.hamonie.glide.GlideApp
import com.hamonie.glide.RetroGlideExtension
import com.hamonie.glide.RetroMusicColoredTarget
import com.hamonie.helper.SortOrder
import com.hamonie.helper.menu.SongsMenuHelper
import com.hamonie.interfaces.IAlbumClickListener
import com.hamonie.interfaces.ICabHolder
import com.hamonie.model.Album
import com.hamonie.model.Song
import com.hamonie.util.MusicUtil
import com.hamonie.util.PreferenceUtil
import com.hamonie.util.color.MediaNotificationProcessor
import me.zhanghai.android.fastscroll.PopupTextProvider

open class AlbumAdapter(
    override val activity: FragmentActivity,
    var dataSet: List<Album>,
    var itemLayoutRes: Int,
    iCabHolder: ICabHolder?,
    val listener: IAlbumClickListener?
) : AbsMultiSelectAdapter<AlbumAdapter.ViewHolder, Album>(
    activity,
    iCabHolder,
    R.menu.menu_media_selection
), PopupTextProvider {

    init {
        this.setHasStableIds(true)
    }

    fun swapDataSet(dataSet: List<Album>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        return createViewHolder(view, viewType)
    }

    protected open fun createViewHolder(view: View, viewType: Int): ViewHolder {
        return ViewHolder(view)
    }

    private fun getAlbumTitle(album: Album): String {
        return album.title
    }

    protected open fun getAlbumText(album: Album): String? {
        return album.albumArtist.let {
            if (it.isNullOrEmpty()) {
                album.artistName
            } else {
                it
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = dataSet[position]
        val isChecked = isChecked(album)
        holder.itemView.isActivated = isChecked
        holder.title?.text = getAlbumTitle(album)
        holder.text?.text = getAlbumText(album)
        // Check if imageContainer exists so we can have a smooth transition without
        // CardView clipping, if it doesn't exist in current layout set transition name to image instead.
        if (holder.imageContainer != null) {
            ViewCompat.setTransitionName(holder.imageContainer!!, album.id.toString())
        } else {
            ViewCompat.setTransitionName(holder.image!!, album.id.toString())
        }
        loadAlbumCover(album, holder)
    }

    protected open fun setColors(color: MediaNotificationProcessor, holder: ViewHolder) {
        if (holder.paletteColorContainer != null) {
            holder.title?.setTextColor(color.primaryTextColor)
            holder.text?.setTextColor(color.secondaryTextColor)
            holder.paletteColorContainer?.setBackgroundColor(color.backgroundColor)
        }
        holder.mask?.backgroundTintList = ColorStateList.valueOf(color.primaryTextColor)
        holder.imageContainerCard?.setCardBackgroundColor(color.backgroundColor)
    }

    protected open fun loadAlbumCover(album: Album, holder: ViewHolder) {
        if (holder.image == null) {
            return
        }
        val song = album.safeGetFirstSong()
        GlideApp.with(activity).asBitmapPalette().albumCoverOptions(song)
            //.checkIgnoreMediaStore()
            .load(RetroGlideExtension.getSongModel(song))
            .into(object : RetroMusicColoredTarget(holder.image!!) {
                override fun onColorReady(colors: MediaNotificationProcessor) {
                    setColors(colors, holder)
                }
            })
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].id
    }

    override fun getIdentifier(position: Int): Album? {
        return dataSet[position]
    }

    override fun getName(album: Album): String {
        return album.title
    }

    override fun onMultipleItemAction(
        menuItem: MenuItem,
        selection: List<Album>
    ) {
        SongsMenuHelper.handleMenuClick(activity, getSongList(selection), menuItem.itemId)
    }

    private fun getSongList(albums: List<Album>): List<Song> {
        val songs = ArrayList<Song>()
        for (album in albums) {
            songs.addAll(album.songs)
        }
        return songs
    }

    override fun getPopupText(position: Int): String {
        return getSectionName(position)
    }

    private fun getSectionName(position: Int): String {
        var sectionName: String? = null
        when (PreferenceUtil.albumSortOrder) {
            SortOrder.AlbumSortOrder.ALBUM_A_Z, SortOrder.AlbumSortOrder.ALBUM_Z_A -> sectionName =
                dataSet[position].title
            SortOrder.AlbumSortOrder.ALBUM_ARTIST -> sectionName = dataSet[position].artistName
            SortOrder.AlbumSortOrder.ALBUM_YEAR -> return MusicUtil.getYearString(
                dataSet[position].year
            )
        }
        return MusicUtil.getSectionName(sectionName)
    }

    inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {

        init {
            menu?.visibility = View.GONE
        }

        override fun onClick(v: View?) {
            super.onClick(v)
            if (isInQuickSelectMode) {
                toggleChecked(layoutPosition)
            } else {
                image?.let {
                    listener?.onAlbumClick(dataSet[layoutPosition].id, imageContainer ?: it)
                }
            }
        }

        override fun onLongClick(v: View?): Boolean {
            return toggleChecked(layoutPosition)
        }
    }

    companion object {
        val TAG: String = AlbumAdapter::class.java.simpleName
    }
}
