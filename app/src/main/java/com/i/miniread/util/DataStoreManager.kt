package com.i.miniread.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore 扩展属性
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "miniread_settings")

object DataStoreManager {
    private lateinit var context: Context
    private val gson = Gson()

    // Keys
    private val KEY_BASE_URL = stringPreferencesKey("base_url")
    private val KEY_API_TOKEN = stringPreferencesKey("api_token")
    private val KEY_FEED_ORDER = stringPreferencesKey("feed_order")

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    // BaseUrl 操作
    suspend fun setBaseUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BASE_URL] = url
        }
    }

    suspend fun getBaseUrl(): String {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_BASE_URL] ?: ""
        }.first()
    }

    fun getBaseUrlFlow(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_BASE_URL] ?: ""
        }
    }

    // ApiToken 操作
    suspend fun setApiToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_API_TOKEN] = token
        }
    }

    suspend fun getApiToken(): String {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_API_TOKEN] ?: ""
        }.first()
    }

    fun getApiTokenFlow(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_API_TOKEN] ?: ""
        }
    }

    // Feed 排序操作 (存储为 JSON)
    suspend fun saveFeedOrder(feedOrder: Map<Int, Int>) {
        val json = gson.toJson(feedOrder)
        context.dataStore.edit { preferences ->
            preferences[KEY_FEED_ORDER] = json
        }
    }

    suspend fun getFeedOrder(): Map<Int, Int> {
        val json = context.dataStore.data.map { preferences ->
            preferences[KEY_FEED_ORDER]
        }.first()

        return if (json.isNullOrEmpty()) {
            emptyMap()
        } else {
            try {
                val type = object : TypeToken<Map<Int, Int>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    // 清除所有数据
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

