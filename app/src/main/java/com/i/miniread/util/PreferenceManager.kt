package com.i.miniread.util

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {
    private const val PREF_NAME = "miniread_prefs"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_API_TOKEN = "api_token"

    private lateinit var preferences: SharedPreferences

    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    var baseUrl: String
        get() = preferences.getString(KEY_BASE_URL, "") ?: ""
        set(value) = preferences.edit().putString(KEY_BASE_URL, value).apply()

    var apiToken: String
        get() = preferences.getString(KEY_API_TOKEN, "") ?: ""
        set(value) = preferences.edit().putString(KEY_API_TOKEN, value).apply()
}
