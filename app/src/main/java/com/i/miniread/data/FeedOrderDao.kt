package com.i.miniread.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FeedOrderDao {
    @Query("SELECT * FROM feed_order ORDER BY orderIndex ASC")
    suspend fun getAllFeedOrders(): List<FeedOrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedOrder(feedOrder: FeedOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFeedOrders(feedOrders: List<FeedOrderEntity>)

    @Query("DELETE FROM feed_order WHERE feedId = :feedId")
    suspend fun deleteFeedOrder(feedId: Int)

    @Query("DELETE FROM feed_order")
    suspend fun deleteAllFeedOrders()
}

