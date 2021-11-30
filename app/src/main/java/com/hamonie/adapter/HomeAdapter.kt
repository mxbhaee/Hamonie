package com.hamonie.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.os.bundleOf
import androidx.fragment.app.findFragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hamonie.appthemehelper.ThemeStore
import com.hamonie.appthemehelper.util.ColorUtil
import com.hamonie.*
import com.hamonie.adapter.album.AlbumAdapter
import com.hamonie.adapter.artist.ArtistAdapter
import com.hamonie.adapter.song.SongAdapter
import com.hamonie.extensions.hide
import com.hamonie.fragments.home.HomeFragment
import com.hamonie.glide.GlideApp
import com.hamonie.glide.RetroGlideExtension
import com.hamonie.helper.MusicPlayerRemote
import com.hamonie.interfaces.IAlbumClickListener
import com.hamonie.interfaces.IArtistClickListener
import com.hamonie.interfaces.IGenreClickListener
import com.hamonie.model.*
import com.hamonie.util.PreferenceUtil
import com.google.android.material.card.MaterialCardView

class HomeAdapter(
    private val activity: AppCompatActivity
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), IArtistClickListener, IAlbumClickListener,
    IGenreClickListener {

    private var list = listOf<Home>()

    override fun getItemViewType(position: Int): Int {
        return list[position].homeSection
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout =
            LayoutInflater.from(activity).inflate(R.layout.section_recycler_view, parent, false)
        return when (viewType) {
            RECENT_ARTISTS, TOP_ARTISTS -> ArtistViewHolder(layout)
            GENRES -> GenreViewHolder(layout)
            FAVOURITES -> PlaylistViewHolder(layout)
            TOP_ALBUMS, RECENT_ALBUMS -> AlbumViewHolder(layout)
            else -> {
                SuggestionsViewHolder(
                    LayoutInflater.from(activity).inflate(
                        R.layout.item_suggestions,
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val home = list[position]
        when (getItemViewType(position)) {
            RECENT_ALBUMS -> {
                val viewHolder = holder as AlbumViewHolder
                viewHolder.bindView(home)
                viewHolder.clickableArea.setOnClickListener {
                    it.findFragment<HomeFragment>().setSharedAxisXTransitions()
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.detailListFragment,
                        bundleOf("type" to RECENT_ALBUMS)
                    )
                }
            }
            TOP_ALBUMS -> {
                val viewHolder = holder as AlbumViewHolder
                viewHolder.bindView(home)
                viewHolder.clickableArea.setOnClickListener {
                    it.findFragment<HomeFragment>().setSharedAxisXTransitions()
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.detailListFragment,
                        bundleOf("type" to TOP_ALBUMS)
                    )
                }
            }
            RECENT_ARTISTS -> {
                val viewHolder = holder as ArtistViewHolder
                viewHolder.bindView(home)
                viewHolder.clickableArea.setOnClickListener {
                    it.findFragment<HomeFragment>().setSharedAxisXTransitions()
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.detailListFragment,
                        bundleOf("type" to RECENT_ARTISTS)
                    )
                }
            }
            TOP_ARTISTS -> {
                val viewHolder = holder as ArtistViewHolder
                viewHolder.bindView(home)
                viewHolder.clickableArea.setOnClickListener {
                    it.findFragment<HomeFragment>().setSharedAxisXTransitions()
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.detailListFragment,
                        bundleOf("type" to TOP_ARTISTS)
                    )
                }
            }
            SUGGESTIONS -> {
                val viewHolder = holder as SuggestionsViewHolder
                viewHolder.bindView(home)
            }
            FAVOURITES -> {
                val viewHolder = holder as PlaylistViewHolder
                viewHolder.bindView(home)
                viewHolder.clickableArea.setOnClickListener {
                    it.findFragment<HomeFragment>().setSharedAxisXTransitions()
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.detailListFragment,
                        bundleOf("type" to FAVOURITES)
                    )
                }
            }
            GENRES -> {
                val viewHolder = holder as GenreViewHolder
                viewHolder.bind(home)
            }
            PLAYLISTS -> {
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun swapData(sections: List<Home>) {
        list = sections
        notifyDataSetChanged()
    }

    private inner class AlbumViewHolder(view: View) : AbsHomeViewItem(view) {
        fun bindView(home: Home) {
            title.setText(home.titleRes)
            recyclerView.apply {
                adapter = albumAdapter(home.arrayList as List<Album>)
                layoutManager = gridLayoutManager()
            }
        }
    }

    private inner class ArtistViewHolder(view: View) : AbsHomeViewItem(view) {
        fun bindView(home: Home) {
            title.setText(home.titleRes)
            recyclerView.apply {
                layoutManager = linearLayoutManager()
                adapter = artistsAdapter(home.arrayList as List<Artist>)
            }
        }
    }

    private inner class SuggestionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val images = listOf(
            R.id.image1,
            R.id.image2,
            R.id.image3
            /**R.id.image4,
            R.id.image5,
            R.id.image6,
            R.id.image7,
            R.id.image8 **/
        )

        fun bindView(home: Home) {
            val color = ThemeStore.accentColor(activity)
            itemView.findViewById<TextView>(R.id.message).apply {
                setTextColor(color)
                setOnClickListener {
                    MusicPlayerRemote.playNext((home.arrayList as List<Song>).subList(0, 8))
                    if (!MusicPlayerRemote.isPlaying) {
                        MusicPlayerRemote.playNextSong()
                    }
                }
            }
            itemView.findViewById<MaterialCardView>(R.id.card6).apply {
                setCardBackgroundColor(ColorUtil.withAlpha(color, 0.12f))
            }
            images.forEachIndexed { index, id ->
                itemView.findViewById<View>(id).setOnClickListener {
                    MusicPlayerRemote.playNext(home.arrayList[index] as Song)
                    if (!MusicPlayerRemote.isPlaying) {
                        MusicPlayerRemote.playNextSong()
                    }
                }
                GlideApp.with(activity)
                    .asBitmap()
                    .songCoverOptions(home.arrayList[index] as Song)
                    .load(RetroGlideExtension.getSongModel(home.arrayList[index] as Song))
                    .into(itemView.findViewById(id))
            }
        }
    }

    private inner class PlaylistViewHolder(view: View) : AbsHomeViewItem(view) {
        fun bindView(home: Home) {
            title.setText(home.titleRes)
            recyclerView.apply {
                val songAdapter = SongAdapter(
                    activity,
                    home.arrayList as MutableList<Song>,
                    R.layout.item_favourite_card, null
                )
                layoutManager = linearLayoutManager()
                adapter = songAdapter
            }
        }
    }

    private inner class GenreViewHolder(itemView: View) : AbsHomeViewItem(itemView) {
        fun bind(home: Home) {
            arrow.hide()
            title.setText(home.titleRes)
            val genreAdapter = GenreAdapter(
                activity,
                home.arrayList as List<Genre>,
                R.layout.item_grid_genre,
                this@HomeAdapter
            )
            recyclerView.apply {
                layoutManager = GridLayoutManager(activity, 3, GridLayoutManager.HORIZONTAL, false)
                adapter = genreAdapter
            }
        }
    }

    open class AbsHomeViewItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recyclerView: RecyclerView = itemView.findViewById(R.id.recyclerView)
        val title: AppCompatTextView = itemView.findViewById(R.id.title)
        val arrow: ImageView = itemView.findViewById(R.id.arrow)
        val clickableArea: ViewGroup = itemView.findViewById(R.id.clickable_area)
    }

    private fun artistsAdapter(artists: List<Artist>) =
        ArtistAdapter(activity, artists, PreferenceUtil.homeArtistGridStyle, null, this)

    private fun albumAdapter(albums: List<Album>) =
        AlbumAdapter(activity, albums, PreferenceUtil.homeAlbumGridStyle, null, this)

    private fun gridLayoutManager() =
        GridLayoutManager(activity, 1, GridLayoutManager.HORIZONTAL, false)

    private fun linearLayoutManager() =
        LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)

    override fun onArtist(artistId: Long, view: View) {
        activity.findNavController(R.id.fragment_container).navigate(
            R.id.artistDetailsFragment,
            bundleOf(EXTRA_ARTIST_ID to artistId),
            null,
            FragmentNavigatorExtras(
                view to artistId.toString()
            )
        )
    }

    override fun onAlbumClick(albumId: Long, view: View) {
        activity.findNavController(R.id.fragment_container).navigate(
            R.id.albumDetailsFragment,
            bundleOf(EXTRA_ALBUM_ID to albumId),
            null,
            FragmentNavigatorExtras(
                view to albumId.toString()
            )
        )
    }

    override fun onClickGenre(genre: Genre, view: View) {
        activity.findNavController(R.id.fragment_container).navigate(
            R.id.genreDetailsFragment,
            bundleOf(EXTRA_GENRE to genre),
            null,
            FragmentNavigatorExtras(
                view to "genre"
            )
        )
    }

}
