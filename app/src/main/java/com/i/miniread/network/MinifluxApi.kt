package com.i.miniread.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface MinifluxApi {
    @GET("v1/feeds")
    suspend fun getFeeds(@Header("X-Auth-Token") authToken: String): List<Entry>

    @GET("v1/categories")
    suspend fun getCategories(@Header("X-Auth-Token") authToken: String): List<Category>
}

data class Entry(val id: Int, val title: String, val url: String, val content: String, val published_at: String)
data class Category(val id: Int, val title: String)

object RetrofitInstance {
    val api: MinifluxApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://lifeo3.icu:4081/")  // 替换为实际的 Miniflux API URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MinifluxApi::class.java)
    }
}
