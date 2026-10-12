package com.i.miniread.util

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

// DataStore 扩展属性
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "miniread_settings")

object DataStoreManager {
    private const val TAG = "DataStoreManager"
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

    // ApiToken 操作（加密存储，委托 SecureTokenStore；公开签名保持不变）
    suspend fun setApiToken(token: String) {
        // 写入加密存储
        SecureTokenStore.set(context, token)
        // 清理 DataStore 中可能残留的旧明文 token，避免被云备份泄漏
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_API_TOKEN)
        }
    }

    suspend fun getApiToken(): String {
        // 1. 优先读取加密存储
        val encrypted = SecureTokenStore.get(context)
        if (encrypted.isNotEmpty()) return encrypted

        // 2. 加密存储为空：检查 DataStore 是否存在旧明文 token（首次迁移）
        val legacy = context.dataStore.data.map { preferences ->
            preferences[KEY_API_TOKEN] ?: ""
        }.first()
        if (legacy.isEmpty()) return ""

        // 3. 写入加密存储成功后才删除旧明文；任一步失败都不删除旧值，保证不丢数据且幂等
        return try {
            SecureTokenStore.set(context, legacy)
            context.dataStore.edit { preferences ->
                preferences.remove(KEY_API_TOKEN)
            }
            legacy
        } catch (e: Exception) {
            Log.e(TAG, "Token migration to encrypted store failed, keeping legacy value", e)
            legacy
        }
    }

    fun getApiTokenFlow(): Flow<String> {
        return flow { emit(getApiToken()) }
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
        // 同时清理加密存储中的 token
        SecureTokenStore.clear(context)
    }
}

