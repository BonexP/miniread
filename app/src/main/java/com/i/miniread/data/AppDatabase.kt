package com.i.miniread.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [EntryEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}
