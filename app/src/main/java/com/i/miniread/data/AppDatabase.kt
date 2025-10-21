package com.i.miniread.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [EntryEntity::class, FeedOrderEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun feedOrderDao(): FeedOrderDao
}
