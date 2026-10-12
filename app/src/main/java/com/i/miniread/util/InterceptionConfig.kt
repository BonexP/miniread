package com.i.miniread.util

/**
 * 定向拦截的默认值与纯判定函数。
 *
 * ⚠️ 重要：v1.0.0 起"针对特定 feed 源/特定域名"的定向能力改为**用户可配置**，
 * 因此这里的默认值全部**中性化**（关闭 / 空）——公开版本装上去不会对任何站点做特殊处理。
 * 需要该能力时，在应用内连续点击顶栏标题 7 次进入隐藏设置页填写；
 * 运行期生效值统一由 [InterceptionRuntime] 提供。
 *
 * 本文件只保留纯函数（无 Android 依赖），便于纯 JVM 单元测试锁定语义。
 */
object InterceptionConfig {

    /** 默认目标域名列表：空 → 默认不启用定向拦截。 */
    val DEFAULT_TARGET_DOMAINS: List<String> = emptyList()

    /** 默认高级 feed 白名单：空。 */
    val DEFAULT_ADVANCED_FEED_IDS: Set<Int> = emptySet()

    /**
     * 默认 Referer 规则：空。
     * 生效时**顺序敏感**（先匹配者生效），例如
     * `sspai.com => https://sspai.com/`、`sinaimg.cn => https://weibo.com/`。
     */
    val DEFAULT_REFERER_RULES: List<Pair<String, String>> = emptyList()

    /** 默认总开关：关闭。 */
    const val DEFAULT_INTERCEPT_ENABLED: Boolean = false

    /**
     * 判断 baseUrl 是否命中目标域名。
     *
     * 语义与原实现 `baseUrl.contains(target, ignoreCase = true)` 保持一致，
     * 但增加 [target] 非空白保护 —— 空串会让 `contains` 恒为 true，
     * 那意味着"未配置域名"变成了"拦截一切"，必须避免。
     */
    fun matchesBaseUrl(baseUrl: String, target: String): Boolean {
        if (target.isBlank()) return false
        return baseUrl.contains(target, ignoreCase = true)
    }

    /** 命中任一目标域名即返回 true。 */
    fun matchesAnyBaseUrl(baseUrl: String, targets: List<String>): Boolean {
        return targets.any { matchesBaseUrl(baseUrl, it) }
    }

    /**
     * 按 [rules] 顺序为 host 计算 Referer，未命中返回空串。
     * 等价于原 `sspai.com -> https://sspai.com/` / `sinaimg.cn -> https://weibo.com/` 的
     * if / else-if / else 逻辑。
     */
    fun refererFor(host: String?, rules: List<Pair<String, String>>): String {
        for ((pattern, referer) in rules) {
            if (pattern.isNotBlank() && host?.contains(pattern) == true) {
                return referer
            }
        }
        return ""
    }
}
