package com.hamonie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.hamonie.appthemehelper.ThemeStore
import com.hamonie.*
import com.hamonie.adapter.base.MediaEntryViewHolder
import com.hamonie.db.PlaylistEntity
import com.hamonie.db.PlaylistWithSongs
import com.hamonie.glide.GlideApp
import com.hamonie.glide.RetroGlideExtension
import com.hamonie.helper.MusicPlayerRemote
import com.hamonie.helper.menu.SongMenuHelper
import com.hamonie.model.*
import com.hamonie.model.smartplaylist.AbsSmartPlaylist
import com.hamonie.repository.PlaylistSongsLoader
import com.hamonie.util.MusicUtil
import java.util.*

class SearchAdapter(
    private val activity: FragmentActivity,
    private var dataSet: List<Any>
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    fun swapDataSet(dataSet: List<Any>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        if (dataSet[position] is Album) return ALBUM
        if (dataSet[position] is Artist) return if ((dataSet[position] as Artist).isAlbumArtist) ALBUM_ARTIST else ARTIST
        if (dataSet[position] is Genre) return GENRE
        if (dataSet[position] is PlaylistEntity) return PLAYLIST
        return if (dataSet[position] is Song) SONG else HEADER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == HEADER) ViewHolder(
            LayoutInflater.from(activity).inflate(
                R.layout.sub_header,
                parent,
                false
            ), viewType
        )
        else if (viewType == ALBUM || viewType == ARTIST || viewType== ALBUM_ARTIST)
            ViewHolder(
                LayoutInflater.from(activity).inflate(
                    R.layout.item_list_big,
                    parent,
                    false
                ), viewType
            )
        else
            ViewHolder(
                LayoutInflater.from(activity).inflate(R.layout.item_list, parent, false),
                viewType
            )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            ALBUM -> {
                holder.imageTextContainer?.isVisible = true
                val album = dataSet[position] as Album
                holder.title?.text = album.title
                holder.text?.text = album.artistName
                GlideApp.with(activity).asDrawable().albumCoverOptions(album.safeGetFirstSong()).load(RetroGlideExtension.getSongModel(album.safeGetFirstSong()))
                    .into(holder.image!!)
            }
            ARTIST -> {
                holder.imageTextContainer?.isVisible = true
                val artist = dataSet[position] as Artist
                holder.title?.text = artist.name
                holder.text?.text = MusicUtil.getArtistInfoString(activity, artist)
                GlideApp.with(activity).asDrawable().artistImageOptions(artist).load(
                    RetroGlideExtension.getArtistModel(artist)).into(holder.image!!)
            }
            SONG -> {
                holder.imageTextContainer?.isVisible = true
                val song = dataSet[position] as Song
                holder.title?.text = song.title
                holder.text?.text = song.albumName
                GlideApp.with(activity).asDrawable().songCoverOptions(song).load(RetroGlideExtension.getSongModel(song)).into(holder.image!!)
            }
            GENRE -> {
                val genre = dataSet[position] as Genre
                holder.title?.text = genre.name
                holder.text?.text = String.format(
                    Locale.getDefault(),
                    "%d %s",
                    genre.songCount,
                    if (genre.songCount > 1) activity.getString(R.string.songs) else activity.getString(
                        R.string.song
                    )
                )
            }
            PLAYLIST -> {
                val playlist = dataSet[position] as PlaylistEntity
                holder.title?.text = playlist.playlistName
                //holder.text?.text = MusicUtil.playlistInfoString(activity, playlist.songs)
            }
            ALBUM_ARTIST -> {
                holder.imageTextContainer?.isVisible = true
                val artist = dataSet[position] as Artist
                holder.title?.text = artist.name
                holder.text?.text = MusicUtil.getArtistInfoString(activity, artist)
                GlideApp.with(activity).asDrawable().artistImageOptions(artist).load(
                    RetroGlideExtension.getArtistModel(artist)).into(holder.image!!)
            }
            else -> {
                holder.title?.text = dataSet[position].toString()
                holder.title?.setTextColor(ThemeStore.accentColor(activity))
            }
        }
    }

    private fun getSongs(playlist: Playlist): List<Song> {
        val songs = mutableListOf<Song>()
        if (playlist is AbsSmartPlaylist) {
            songs.addAll(playlist.getSongs())
        } else {
            songs.addAll(PlaylistSongsLoader.getPlaylistSongList(activity, playlist.id))
        }
        return songs
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    inner class ViewHolder(itemView: View, itemViewType: Int) : MediaEntryViewHolder(itemView) {
        init {
            itemView.setOnLongClickListener(null)
            imageTextContainer?.isInvisible = true
            if (itemViewType == SONG) {
                imageTextContainer?.isGone = true
                menu?.visibility = View.VISIBLE
                menu?.setOnClickListener(object : SongMenuHelper.OnClickSongMenu(activity) {
                    override val song: Song
                        get() = dataSet[layoutPosition] as Song
                })
            } else {
                menu?.visibility = View.GONE
            }

            when (itemViewType) {
                ALBUM -> setImageTransitionName(activity.getString(R.string.transition_album_art))
                ARTIST -> setImageTransitionName(activity.getString(R.string.transition_artist_image))
                else -> {
                    val container = itemView.findViewById<View>(R.id.imageContainer)
                    container?.visibility = View.GONE
                }
            }
        }

        override fun onClick(v: View?) {
            val item = dataSet[layoutPosition]
            when (itemViewType) {
                ALBUM -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.albumDetailsFragment,
                        bundleOf(EXTRA_ALBUM_ID to (item as Album).id)
                    )
                }
                ARTIST -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.artistDetailsFragment,
                        bundleOf(EXTRA_ARTIST_ID to (item as Artist).id)
                    )
                }
                ALBUM_ARTIST ->{
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.albumArtistDetailsFragment,
                        bundleOf(EXTRA_ARTIST_NAME to (item as Artist).name)
                    )
                }
                GENRE -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.genreDetailsFragment,
                        bundleOf(EXTRA_GENRE to (item as Genre))
                    )
                }
                PLAYLIST -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.playlistDetailsFragment,
                        bundleOf(EXTRA_PLAYLIST to (item as PlaylistWithSongs))
                    )
                }
                SONG -> {
                    val playList = mutableListOf<Song>()
                    playList.add(item as Song)
                    MusicPlayerRemote.openQueue(playList, 0, true)
                }
            }
        }
    }

    companion object {
        private const val HEADER = 0
        private const val ALBUM = 1
        private const val ARTIST = 2
        private const val SONG = 3
        private const val GENRE = 4
        private const val PLAYLIST = 5
        private const val ALBUM_ARTIST = 6
    }
}
