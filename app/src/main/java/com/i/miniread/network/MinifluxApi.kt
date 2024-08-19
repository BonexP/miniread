package com.i.miniread.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface MinifluxApi {
    @POST("v1/authentication")
    @FormUrlEncoded
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): AuthResponse

    @GET("v1/feeds")
    suspend fun getFeeds(@Header("X-Auth-Token") authToken: String): List<Entry>

    @GET("v1/categories")
    suspend fun getCategories(@Header("X-Auth-Token") authToken: String): List<Category>
}

data class AuthResponse(val token: String)
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
