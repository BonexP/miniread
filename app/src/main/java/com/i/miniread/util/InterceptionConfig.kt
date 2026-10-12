package com.i.miniread.util

/**
 * 定向特性常量集中定义。
 *
 * ⚠️ 重要：本文件中的常量与判定函数只是对既有字面量的「逐字搬运」，
 * 严禁修改任何取值或判定顺序，否则会改变针对特定 feed 源/特定域名的定向行为。
 */
object InterceptionConfig {

    /** 目标域名（来自 DomainHelper.kt 与 ArticleDetailScreen.kt 的原字面量）。 */
    const val TARGET_DOMAIN: String = "pi.lifeo3.icu"

    /** 特定 feed 白名单（原字面量：listOf(26, 38, 52, 51)）。 */
    val ADVANCED_FEED_IDS: Set<Int> = setOf(26, 38, 52, 51)

    /**
     * Referer 映射规则，**顺序敏感**（先匹配者生效）。
     * 原逻辑：sspai.com -> https://sspai.com/ ；sinaimg.cn -> https://weibo.com/ ；否则空串。
     */
    val REFERER_RULES: List<Pair<String, String>> = listOf(
        "sspai.com" to "https://sspai.com/",
        "sinaimg.cn" to "https://weibo.com/"
    )

    /**
     * 判断 baseUrl 是否命中目标域名。
     * 与 DomainHelper.kt 原实现 `baseUrl.contains(target, ignoreCase = true)` 完全等价。
     */
    fun matchesBaseUrl(baseUrl: String, target: String = TARGET_DOMAIN): Boolean {
        return baseUrl.contains(target, ignoreCase = true)
    }

    /**
     * 按 [REFERER_RULES] 顺序为 host 计算 Referer，未命中返回空串。
     * 等价于 ArticleDetailScreen.kt 原 if / else-if / else 逻辑。
     */
    fun refererFor(host: String?): String {
        for ((pattern, referer) in REFERER_RULES) {
            if (host?.contains(pattern) == true) {
                return referer
            }
        }
        return ""
    }
}
