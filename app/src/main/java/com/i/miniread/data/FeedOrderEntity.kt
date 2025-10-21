package com.i.miniread.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_order")
data class FeedOrderEntity(
    @PrimaryKey val feedId: Int,
    val orderIndex: Int
)

