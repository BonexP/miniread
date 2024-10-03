package com.i.miniread.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MinifluxApi {

    @GET("v1/feeds")
    suspend fun getFeeds(
        @Header("X-Auth-Token") authToken: String
    ): List<Feed>

    @GET("v1/categories")
    suspend fun getCategories(
        @Header("X-Auth-Token") authToken: String
    ): List<Category>

    @POST("v1/feeds")
    suspend fun createFeed(
        @Header("X-Auth-Token") authToken: String,
        @Body feed: FeedCreationRequest
    ): Feed

    @DELETE("v1/feeds/{feedId}")
    suspend fun deleteFeed(
        @Header("X-Auth-Token") authToken: String,
        @Path("feedId") feedId: Int
    ): Void

    @PUT("v1/feeds/{feedId}/refresh")
    suspend fun refreshFeed(
        @Header("X-Auth-Token") authToken: String,
        @Path("feedId") feedId: Int
    ): Void

    @PUT("v1/feeds/refresh")
    suspend fun refreshAllFeeds(
        @Header("X-Auth-Token") authToken: String
    ): Void

    @GET("v1/feeds/{feedId}/entries")
    suspend fun getFeedEntries(
        @Header("X-Auth-Token") authToken: String,
        @Path("feedId") feedId: Int,
        @Query("status") status: String?,
        @Query("order") order: String,
        @Query("direction") direction: String?
    ): EntriesResponse

    @GET("v1/feeds/{feedId}/icon")
    suspend fun getFeedIcon(
        @Header("X-Auth-Token") authToken: String,
        @Path("feedId") feedId: Int
    ): Icon

    @GET("v1/entries/{entryId}")
    suspend fun getEntry(
        @Header("X-Auth-Token") authToken: String,
        @Path("entryId") entryId: Int
    ): Entry

    @POST("v1/entries/{entryId}/bookmark")
    suspend fun bookmarkEntry(
        @Header("X-Auth-Token") authToken: String,
        @Path("entryId") entryId: Int
    ): Void

    @PUT("v1/entries")
    suspend fun markEntryAsUnread(
        @Header("X-Auth-Token") authToken: String,
        @Body body: EntryAndStatus,
    ): Response<Void?>

    @GET("v1/entries")
    suspend fun getEntries(
        @Header("X-Auth-Token") authToken: String,
        @Query("status") status: String? = "unread",
        @Query("category_id") categoryId: Int? = null,
        @Query("direction") direction: String? = "desc",
    ): EntriesResponse


    @GET("v1/entries")
    suspend fun getTodayEntries(
        @Header("X-Auth-Token") authToken: String,
        @Query("status") status: String? = "unread",
        @Query("direction") direction: String? = "desc",
        @Query("published_after") published_after: Long? = System.currentTimeMillis() / 1000 - 86400,
    ): EntriesResponse

    @PUT("/v1/entries/{entryId}/bookmark")
    suspend fun toggleEntryBookMark(
        @Header("X-Auth-Token") authToken: String,
        @Path("entryId") entryId: Int
    ): Response<Void?>

    @GET("v1/me")
    suspend fun getUserInfo(
        @Header("X-Auth-Token") authToken: String
    ): UserInfo


    @DELETE("v1/feeds/{feedId}")
    suspend fun deleteFeed(
        @Header("X-Auth-Token") authToken: String,
        @Path("feedId") feedId: Long
    )

}


data class EntryAndStatus
    (
    val entry_ids: List<Int>,
    val status: String = "unread"
)


data class AuthResponse(val token: String)

// 创建 EntriesResponse 数据类以匹配新的 API 响应结构
data class EntriesResponse(
    val total: Int,
    val entries: List<Entry>
)

data class Feed(
    var id: Int,
    val title: String,
    val site_url: String,
    val feed_url: String,
    val category: Category,
    val disabled: Boolean
) {
    constructor(id: Int) : this(
        id = id,
        title = "",
        site_url = "",
        feed_url = "",
        category = Category(),// Assuming Category has a default constructor
        disabled = false
    )
}


data class Entry(
    val id: Int,
    val title: String?,
    val url: String?,
    val content: String?,
    val published_at: String?,
    val status: String?,
    val feed_id: Int,
    val starred: Boolean,
    val feed: Feed
)

data class Category(
    val id: Int,
    val title: String
) {
    constructor() : this(
        id = -1,
        title = ""
    )
}

data class FeedCreationRequest(
    val feed_url: String,
    val category_id: Int? = null
)

data class UserInfo(
    val id: Int,
    val username: String,
    val is_admin: Boolean
)

data class Icon(
    val id: Int,
    val data: String,
    val mime_type: String
)

object RetrofitInstance {
    private const val TAG = "MinifluxApi"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level =
            HttpLoggingInterceptor.Level.BASIC // Logs request and response lines and their respective headers and bodies (if present)
    }


    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val api: MinifluxApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://pi.lifeo3.icu:4081/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MinifluxApi::class.java)
    }

    init {
        Log.d(TAG, "RetrofitInstance initialized with base URL: https://pi.lifeo3.icu:4081/")
    }
}
