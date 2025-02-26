package com.i.miniread.util

import android.content.Context

// 新建 DomainHelper.kt
object DomainHelper {
    fun isTargetDomain(context: Context): Boolean {
        return PreferenceManager.baseUrl.contains(
            "pi.lifeo3.icu",
            ignoreCase = true
        )
    }
}


