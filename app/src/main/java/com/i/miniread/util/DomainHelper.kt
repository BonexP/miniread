package com.i.miniread.util

import android.content.Context
import kotlinx.coroutines.runBlocking

object DomainHelper {
    fun isTargetDomain(context: Context): Boolean {
        // 使用 runBlocking 同步读取 DataStore 数据
        return runBlocking {
            val baseUrl = DataStoreManager.getBaseUrl()
            baseUrl.contains("pi.lifeo3.icu", ignoreCase = true)
        }
    }
}

