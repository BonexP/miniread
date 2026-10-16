package com.i.miniread.util

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
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

    // 定向拦截配置（v1.0.0 新增；默认全部关闭 / 为空，即公开版本不做任何定向处理）
    private val KEY_INTERCEPT_ENABLED = booleanPreferencesKey("intercept_enabled")
    private val KEY_INTERCEPT_DOMAINS = stringPreferencesKey("intercept_target_domains")
    private val KEY_INTERCEPT_FEED_IDS = stringPreferencesKey("intercept_feed_ids")
    private val KEY_INTERCEPT_REFERER_RULES = stringPreferencesKey("intercept_referer_rules")

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

    /**
     * token 的内存快照（供 [getApiTokenFlow] 观察）。
     * null = 尚未读取过；一旦有值，后续 set / 迁移 / clearAll 都会向订阅者推送新值。
     */
    private val tokenSnapshot = MutableStateFlow<String?>(null)

    suspend fun setApiToken(token: String) {
        // 写入加密存储（失败会抛出、调用方可见；此时旧明文保持不动，不会丢 token）
        SecureTokenStore.set(context, token)
        tokenSnapshot.value = token
        // 清理 DataStore 中可能残留的旧明文 token，避免被云备份泄漏
        // （失败仅记录日志，下一次 getApiToken 会继续重试）
        removeLegacyTokenIfPresent()
    }

    suspend fun getApiToken(): String {
        // 1. 优先读取加密存储
        val encrypted = SecureTokenStore.get(context)
        if (encrypted.isNotEmpty()) {
            // 兜底清理：迁移若在「已写入加密存储、尚未删除旧明文」之间被中断（进程被杀 /
            // 系统回收），旧明文会永久残留 —— 每次读到加密值都补一次删除，失败下次再试。
            removeLegacyTokenIfPresent()
            tokenSnapshot.value = encrypted
            return encrypted
        }

        // 2. 加密存储为空：检查 DataStore 是否存在旧明文 token（首次迁移）
        val legacy = context.dataStore.data.map { preferences ->
            preferences[KEY_API_TOKEN] ?: ""
        }.first()
        if (legacy.isEmpty()) {
            tokenSnapshot.value = ""
            return ""
        }

        // 3. 写入加密存储成功后才删除旧明文；任一步失败都不删除旧值，保证不丢数据且幂等
        return try {
            SecureTokenStore.set(context, legacy)
            removeLegacyTokenIfPresent()
            tokenSnapshot.value = legacy
            legacy
        } catch (e: Exception) {
            Log.e(TAG, "Token migration to encrypted store failed, keeping legacy value", e)
            legacy
        }
    }

    /**
     * token 变化流：先发射当前值，之后随 [setApiToken] / 首次迁移 / [clearAll] 继续发射。
     *
     * 与旧实现（直接映射 DataStore 中的明文 key）语义对齐：订阅者拿到的是一条**持续**的流，
     * 而不是「发射一次即结束」的快照。数据源是加密存储的内存快照 —— 应用内所有写入都必须
     * 经过本对象，因此无需依赖 EncryptedSharedPreferences 的变更监听。
     */
    fun getApiTokenFlow(): Flow<String> = flow {
        tokenSnapshot.value = getApiToken()        // 读盘（含一次性迁移兜底）并刷新快照
        emitAll(tokenSnapshot.filterNotNull())     // 先补发当前值，再持续跟随后续变化
    }

    /**
     * 删除 DataStore 中残留的旧明文 token（best-effort）。
     *
     * 只在确实存在旧值时才写盘；删除失败**不抛异常** ——
     * 留给下一次 [getApiToken] 重试，避免一次 IO 抖动就让调用方看到失败。
     */
    private suspend fun removeLegacyTokenIfPresent() {
        val legacy = context.dataStore.data.map { preferences ->
            preferences[KEY_API_TOKEN]
        }.first()
        if (legacy == null) return
        try {
            context.dataStore.edit { preferences ->
                preferences.remove(KEY_API_TOKEN)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove legacy plaintext token, will retry on next read", e)
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

    // 定向拦截配置：一次性读写，避免多次 DataStore 往返
    suspend fun getInterceptionRaw(): InterceptionRawConfig {
        val prefs = context.dataStore.data.first()
        return InterceptionRawConfig(
            enabled = prefs[KEY_INTERCEPT_ENABLED] ?: InterceptionConfig.DEFAULT_INTERCEPT_ENABLED,
            domains = prefs[KEY_INTERCEPT_DOMAINS] ?: "",
            feedIds = prefs[KEY_INTERCEPT_FEED_IDS] ?: "",
            refererRules = prefs[KEY_INTERCEPT_REFERER_RULES] ?: ""
        )
    }

    suspend fun saveInterceptionRaw(config: InterceptionRawConfig) {
        context.dataStore.edit { preferences ->
            preferences[KEY_INTERCEPT_ENABLED] = config.enabled
            preferences[KEY_INTERCEPT_DOMAINS] = config.domains
            preferences[KEY_INTERCEPT_FEED_IDS] = config.feedIds
            preferences[KEY_INTERCEPT_REFERER_RULES] = config.refererRules
        }
    }

    // 清除所有数据（注意：会连定向拦截配置一起清空，回到中性化默认）
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
        // 同时清理加密存储中的 token
        SecureTokenStore.clear(context)
        tokenSnapshot.value = ""
    }
}
