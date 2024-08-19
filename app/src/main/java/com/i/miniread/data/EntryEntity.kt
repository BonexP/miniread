package com.i.miniread.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val url: String,
    val content: String,
    val publishedAt: String
)
