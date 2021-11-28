package com.hamonie.model.smartplaylist

import androidx.annotation.DrawableRes
import com.hamonie.R
import com.hamonie.model.AbsCustomPlaylist

abstract class AbsSmartPlaylist(
    name: String,
    @DrawableRes val iconRes: Int = R.drawable.ic_queue_music
) : AbsCustomPlaylist(
    id = PlaylistIdGenerator(name, iconRes),
    name = name
)