package com.i.miniread.util

/**
 * 定向拦截的**原始**配置，直接对应 DataStore 中的 4 个 key（未解析）。
 */
data class InterceptionRawConfig(
    val enabled: Boolean = InterceptionConfig.DEFAULT_INTERCEPT_ENABLED,
    val domains: String = "",
    val feedIds: String = "",
    val refererRules: String = ""
)

/**
 * 定向拦截的**生效**配置（已解析）。默认值中性化，即"未配置 = 不启用"。
 */
data class InterceptionSettings(
    val enabled: Boolean = InterceptionConfig.DEFAULT_INTERCEPT_ENABLED,
    val targetDomains: List<String> = InterceptionConfig.DEFAULT_TARGET_DOMAINS,
    val feedIds: Set<Int> = InterceptionConfig.DEFAULT_ADVANCED_FEED_IDS,
    val refererRules: List<Pair<String, String>> = InterceptionConfig.DEFAULT_REFERER_RULES
) {
    /** 总开关打开**且**至少配置了一个目标域名，才算真正生效。 */
    val isActive: Boolean
        get() = enabled && targetDomains.isNotEmpty()

    companion object {
        /** 中性化默认配置（公开版本的初始状态）。 */
        val EMPTY: InterceptionSettings = InterceptionSettings()
    }
}

/**
 * 配置文本 <-> [InterceptionSettings] 的纯解析 / 序列化。
 *
 * 无任何 Android 依赖，可直接跑纯 JVM 单元测试。
 *
 * 文本格式：
 * - 目标域名：`a.com,b.com`（逗号 / 换行 / 分号分隔，忽略空白项，保序去重）
 * - feed id：`26,38,52,51`（同上，非数字项忽略）
 * - Referer 规则：`sspai.com=>https://sspai.com/;sinaimg.cn=>https://weibo.com/`（**保序**）
 */
object InterceptionSettingsCodec {

    const val RULE_ARROW = "=>"
    const val ENTRY_SEPARATOR = ";"
    const val ITEM_SEPARATOR = ","

    private val SPLIT_REGEX = Regex("[,\\n;]+")

    fun parseDomains(raw: String?): List<String> =
        raw.orEmpty()
            .split(SPLIT_REGEX)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    fun parseFeedIds(raw: String?): Set<Int> =
        raw.orEmpty()
            .split(SPLIT_REGEX)
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()

    fun parseRefererRules(raw: String?): List<Pair<String, String>> =
        raw.orEmpty()
            .split(SPLIT_REGEX)
            .mapNotNull { entry ->
                val idx = entry.indexOf(RULE_ARROW)
                if (idx <= 0) return@mapNotNull null
                val pattern = entry.substring(0, idx).trim()
                val referer = entry.substring(idx + RULE_ARROW.length).trim()
                if (pattern.isEmpty() || referer.isEmpty()) null else pattern to referer
            }

    fun formatDomains(domains: List<String>): String =
        domains.joinToString(ITEM_SEPARATOR)

    fun formatFeedIds(ids: Set<Int>): String =
        ids.joinToString(ITEM_SEPARATOR)

    fun formatRefererRules(rules: List<Pair<String, String>>): String =
        rules.joinToString(ENTRY_SEPARATOR) { (pattern, referer) ->
            "$pattern$RULE_ARROW$referer"
        }

    fun parse(raw: InterceptionRawConfig): InterceptionSettings = InterceptionSettings(
        enabled = raw.enabled,
        targetDomains = parseDomains(raw.domains),
        feedIds = parseFeedIds(raw.feedIds),
        refererRules = parseRefererRules(raw.refererRules)
    )

    fun format(settings: InterceptionSettings): InterceptionRawConfig = InterceptionRawConfig(
        enabled = settings.enabled,
        domains = formatDomains(settings.targetDomains),
        feedIds = formatFeedIds(settings.feedIds),
        refererRules = formatRefererRules(settings.refererRules)
    )
}
