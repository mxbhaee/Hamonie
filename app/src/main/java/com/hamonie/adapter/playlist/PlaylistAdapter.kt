package com.hamonie.adapter.playlist

import android.graphics.Color
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import com.hamonie.R
import com.hamonie.adapter.base.AbsMultiSelectAdapter
import com.hamonie.adapter.base.MediaEntryViewHolder
import com.hamonie.db.PlaylistEntity
import com.hamonie.db.PlaylistWithSongs
import com.hamonie.db.toSongs
import com.hamonie.extensions.hide
import com.hamonie.extensions.show
import com.hamonie.glide.GlideApp
import com.hamonie.glide.playlistPreview.PlaylistPreview
import com.hamonie.helper.menu.PlaylistMenuHelper
import com.hamonie.helper.menu.SongsMenuHelper
import com.hamonie.interfaces.ICabHolder
import com.hamonie.interfaces.IPlaylistClickListener
import com.hamonie.model.Song
import com.hamonie.util.MusicUtil

class PlaylistAdapter(
    override val activity: FragmentActivity,
    private var dataSet: List<PlaylistWithSongs>,
    private var itemLayoutRes: Int,
    ICabHolder: ICabHolder?,
    private val listener: IPlaylistClickListener
) : AbsMultiSelectAdapter<PlaylistAdapter.ViewHolder, PlaylistWithSongs>(
    activity,
    ICabHolder,
    R.menu.menu_playlists_selection
) {

    init {
        setHasStableIds(true)
    }

    fun swapDataSet(dataSet: List<PlaylistWithSongs>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].playlistEntity.playListId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        return createViewHolder(view)
    }

    fun createViewHolder(view: View): ViewHolder {
        return ViewHolder(view)
    }

    private fun getPlaylistTitle(playlist: PlaylistEntity): String {
        return if (TextUtils.isEmpty(playlist.playlistName)) "-" else playlist.playlistName
    }

    private fun getPlaylistText(playlist: PlaylistWithSongs): String {
        return MusicUtil.getPlaylistInfoString(activity, playlist.songs.toSongs())
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = dataSet[position]
        holder.itemView.isActivated = isChecked(playlist)
        holder.title?.text = getPlaylistTitle(playlist.playlistEntity)
        holder.text?.text = getPlaylistText(playlist)
        val isChecked = isChecked(playlist)
        if (isChecked) {
            holder.menu?.hide()
        } else {
            holder.menu?.show()
        }
        GlideApp.with(activity)
            .load(PlaylistPreview(playlist))
            .playlistOptions()
            .into(holder.image!!)
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getIdentifier(position: Int): PlaylistWithSongs {
        return dataSet[position]
    }

    override fun getName(playlist: PlaylistWithSongs): String {
        return playlist.playlistEntity.playlistName
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<PlaylistWithSongs>) {
        when (menuItem.itemId) {
            else -> SongsMenuHelper.handleMenuClick(
                activity,
                getSongList(selection),
                menuItem.itemId
            )
        }
    }

    private fun getSongList(playlists: List<PlaylistWithSongs>): List<Song> {
        val songs = mutableListOf<Song>()
        playlists.forEach {
            songs.addAll(it.songs.toSongs())
        }
        return songs
    }

    inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {
        init {
            menu?.setOnClickListener { view ->
                val popupMenu = PopupMenu(activity, view)
                popupMenu.inflate(R.menu.menu_item_playlist)
                popupMenu.setOnMenuItemClickListener { item ->
                    PlaylistMenuHelper.handleMenuClick(activity, dataSet[layoutPosition], item)
                }
                popupMenu.show()
            }

            imageTextContainer?.apply {
                cardElevation = 0f
                setCardBackgroundColor(Color.TRANSPARENT)
            }
        }

        override fun onClick(v: View?) {
            if (isInQuickSelectMode) {
                toggleChecked(layoutPosition)
            } else {
                ViewCompat.setTransitionName(itemView, "playlist")
                listener.onPlaylistClick(dataSet[layoutPosition], itemView)
            }
        }

        override fun onLongClick(v: View?): Boolean {
            toggleChecked(layoutPosition)
            return true
        }
    }

    companion object {
        val TAG: String = PlaylistAdapter::class.java.simpleName
    }
}
