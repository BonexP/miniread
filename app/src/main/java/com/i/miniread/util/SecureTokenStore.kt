package com.i.miniread.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 加密 token 存储。
 *
 * 使用 [MasterKey]（AES256_GCM）+ [EncryptedSharedPreferences] 将 api_token 加密落盘，
 * 避免明文写入 DataStore 后被云备份泄漏。
 *
 * 公开 API：[get] / [set] / [clear]，签名稳定，供 [DataStoreManager] 内部委托使用。
 */
object SecureTokenStore {

    private const val TAG = "SecureTokenStore"

    /** EncryptedSharedPreferences 落盘文件名（sharedpref 域下为 miniread_secure_prefs.xml）。 */
    const val PREFS_FILE_NAME: String = "miniread_secure_prefs"

    private const val KEY_API_TOKEN = "api_token"

    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences {
        cachedPrefs?.let { return it }
        synchronized(this) {
            cachedPrefs?.let { return it }
            val appContext = context.applicationContext
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val created = EncryptedSharedPreferences.create(
                appContext,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            cachedPrefs = created
            return created
        }
    }

    /** 读取 token；失败时返回空串（不抛异常，保证调用方降级为未登录）。 */
    fun get(context: Context): String {
        return try {
            prefs(context).getString(KEY_API_TOKEN, "") ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read encrypted token", e)
            ""
        }
    }

    /**
     * 写入 token。使用 commit() 同步落盘，确保迁移场景下写入成功后才允许删除旧明文。
     * 写入失败时抛出异常，由调用方决定是否保留旧值。
     */
    fun set(context: Context, token: String) {
        val ok = prefs(context).edit().putString(KEY_API_TOKEN, token).commit()
        if (!ok) {
            throw IllegalStateException("Failed to commit encrypted token")
        }
    }

    /** 清除 token；失败仅记录日志（不阻塞登出流程）。 */
    fun clear(context: Context) {
        try {
            prefs(context).edit().remove(KEY_API_TOKEN).commit()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear encrypted token", e)
        }
    }
}
