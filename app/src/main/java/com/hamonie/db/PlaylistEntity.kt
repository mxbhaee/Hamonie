package com.hamonie.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "playlist_id")
    val playListId: Long = 0,
    @ColumnInfo(name = "playlist_name")
    val playlistName: String
) : Parcelable
