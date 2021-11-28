package com.hamonie.interfaces

import android.view.View
import com.hamonie.model.Genre

interface IGenreClickListener {
    fun onClickGenre(genre: Genre, view: View)
}