package com.hamonie.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PlaylistEntity::class, SongEntity::class, HistoryEntity::class, PlayCountEntity::class, BlackListStoreEntity::class, LyricsEntity::class],
    version = 23,
    exportSchema = false
)
abstract class RetroDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun blackListStore(): BlackListStoreDao
    abstract fun playCountDao(): PlayCountDao
    abstract fun historyDao(): HistoryDao
    abstract fun lyricsDao(): LyricsDao
}
