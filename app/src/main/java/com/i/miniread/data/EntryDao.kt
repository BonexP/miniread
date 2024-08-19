package com.i.miniread.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<EntryEntity>)

    @Query("SELECT * FROM entries")
    suspend fun getAllEntries(): List<EntryEntity>
}
