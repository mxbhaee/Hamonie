package com.hamonie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.hamonie.R
import com.hamonie.adapter.base.MediaEntryViewHolder
import com.hamonie.glide.GlideApp
import com.hamonie.glide.RetroGlideExtension
import com.hamonie.glide.RetroMusicColoredTarget
import com.hamonie.interfaces.IGenreClickListener
import com.hamonie.model.Genre
import com.hamonie.util.MusicUtil
import com.hamonie.util.color.MediaNotificationProcessor
import java.util.*

/**
 * @author Hemanth S (h4h13).
 */

class GenreAdapter(
    private val activity: FragmentActivity,
    var dataSet: List<Genre>,
    private val mItemLayoutRes: Int,
    private val listener: IGenreClickListener
) : RecyclerView.Adapter<GenreAdapter.ViewHolder>() {

    init {
        this.setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(activity).inflate(mItemLayoutRes, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val genre = dataSet[position]
        holder.title?.text = genre.name
        holder.text?.text = String.format(
            Locale.getDefault(),
            "%d %s",
            genre.songCount,
            if (genre.songCount > 1) activity.getString(R.string.songs) else activity.getString(R.string.song)
        )
        loadGenreImage(genre, holder)
    }

    private fun loadGenreImage(genre: Genre, holder: GenreAdapter.ViewHolder) {
        val genreSong = MusicUtil.songByGenre(genre.id)
        GlideApp.with(activity)
            .asBitmapPalette()
            .load(RetroGlideExtension.getSongModel(genreSong))
            .songCoverOptions(genreSong)
            .into(object : RetroMusicColoredTarget(holder.image!!) {
                override fun onColorReady(colors: MediaNotificationProcessor) {
                    setColors(holder, colors)
                }
            })
        // Just for a bit of shadow around image
        holder.image?.outlineProvider = ViewOutlineProvider.BOUNDS
    }

    private fun setColors(holder: ViewHolder, color: MediaNotificationProcessor) {
        holder.imageContainerCard?.setCardBackgroundColor(color.backgroundColor)
        holder.title?.setTextColor(color.primaryTextColor)
        holder.text?.setTextColor(color.secondaryTextColor)
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    fun swapDataSet(list: List<Genre>) {
        dataSet = list
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {
        override fun onClick(v: View?) {
            listener.onClickGenre(dataSet[layoutPosition], itemView)
        }
    }
}
