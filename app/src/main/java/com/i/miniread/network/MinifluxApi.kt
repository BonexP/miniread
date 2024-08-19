package com.i.miniread.network

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface MinifluxApi {

    @GET("v1/feeds")
    suspend fun getFeeds(
        @Header("X-Auth-Token") authToken: String
    ): List<Entry>

    @GET("v1/categories")
    suspend fun getCategories(
        @Header("X-Auth-Token") authToken: String
    ): List<Category>

    @POST("v1/feeds")
    suspend fun createFeed(
        @Header("X-Auth-Token") authToken: String,
        @Body feed: FeedCreationRequest
    ): Entry

    @DELETE("v1/feeds/{feedId}")
    suspend fun deleteFeed(
        @Header("X-Auth-Token") authToken: String,
        @Path("feedId") feedId: Int
    ): Void

    @POST("v1/entries/{entryId}/bookmark")
    suspend fun bookmarkEntry(
        @Header("X-Auth-Token") authToken: String,
        @Path("entryId") entryId: Int
    ): Void

    @POST("v1/entries/{entryId}/unread")
    suspend fun markEntryAsUnread(
        @Header("X-Auth-Token") authToken: String,
        @Path("entryId") entryId: Int
    ): Void

    @GET("v1/entries")
    suspend fun getEntries(
        @Header("X-Auth-Token") authToken: String,
        @Query("status") status: String? = null,
        @Query("category_id") categoryId: Int? = null
    ): List<Entry>

    @GET("v1/me")
    suspend fun getUserInfo(
        @Header("X-Auth-Token") authToken: String
    ): UserInfo
}

data class AuthResponse(val token: String)

data class Entry(
    val id: Int,
    val title: String?,
    val url: String?,
    val content: String?,
    val published_at: String?,
    val status: String?
)

data class Category(
    val id: Int,
    val title: String
)

data class FeedCreationRequest(
    val feed_url: String,
    val category_id: Int? = null
)

data class UserInfo(
    val id: Int,
    val username: String,
    val is_admin: Boolean
)

object RetrofitInstance {
    private const val TAG = "MinifluxApi"

    val api: MinifluxApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://pi.lifeo3.icu:4081/")  // 替换为实际的 Miniflux API URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MinifluxApi::class.java)
    }

    init {
        Log.d(TAG, "RetrofitInstance initialized with base URL: https://pi.lifeo3.icu:4081/")
    }
}
