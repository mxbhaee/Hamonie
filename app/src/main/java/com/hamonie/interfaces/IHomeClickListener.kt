package com.hamonie.interfaces

import com.hamonie.model.Album
import com.hamonie.model.Artist
import com.hamonie.model.Genre

interface IHomeClickListener {
    fun onAlbumClick(album: Album)

    fun onArtistClick(artist: Artist)

    fun onGenreClick(genre: Genre)
}