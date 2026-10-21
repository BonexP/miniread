package com.i.miniread.util

/**
 * 「用当前服务器填入示例」的纯逻辑。
 *
 * ⚠️ 这里**不保存任何硬编码地址**：示例全部从当前已登录的 `baseUrl` 派生，
 * 谁装上看到的都是自己的地址，不会把开发者的实例地址带进 APK。
 *
 * 生成规则：
 * - 目标域名 = baseUrl 的 host（去掉 scheme / 路径 / 端口）
 * - 订阅源编号 = 已有的填写值；没有就用当前已加载订阅源的前 3 个；再没有就留空
 * - Referer 规则 = `images.<host>=><scheme>://<host>/`（语法正确的占位示例，需要用户按实际图床改）
 *
 * 无 Android 依赖，纯 JVM 可测。
 */
object InterceptionExamples {

    private const val DEFAULT_SCHEME = "https"

    /** 兜底示例最多取几个订阅源：够说明「逗号分隔」即可，不要一键把全部源都拦上。 */
    private const val MAX_EXAMPLE_FEEDS = 3

    /** 从 baseUrl 解析出的服务器信息。 */
    data class Server(val scheme: String, val host: String) {
        val isValid: Boolean get() = host.isNotEmpty()
    }

    /**
     * 宽容解析：接受 `https://host`、`host`、`http://host:8080/path`、含 userinfo 的 URL。
     * 解析不出 host 时返回 `Server("https", "")`，由调用方决定如何提示。
     */
    fun parseServer(baseUrl: String?): Server {
        var rest = baseUrl.orEmpty().trim()
        if (rest.isEmpty()) return Server(DEFAULT_SCHEME, "")

        var scheme = DEFAULT_SCHEME
        val schemeEnd = rest.indexOf("://")
        if (schemeEnd > 0) {
            scheme = rest.substring(0, schemeEnd).lowercase()
            rest = rest.substring(schemeEnd + 3)
        }

        rest = rest
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('@')
        // IPv6 字面量形如 [::1]:8080，不能按第一个 ':' 截断
        rest = if (rest.startsWith("[")) {
            val close = rest.indexOf(']')
            if (close > 0) rest.substring(0, close + 1) else rest
        } else {
            rest.substringBefore(':')
        }
        return Server(scheme, rest.trim())
    }

    /**
     * @param currentFeedIds 用户当前已填写的白名单文本（非空时原样保留，不覆盖用户的劳动）
     * @param availableFeedIds 当前已加载的订阅源编号，用作兜底示例
     */
    fun build(
        baseUrl: String?,
        currentFeedIds: String,
        availableFeedIds: List<Int>
    ): InterceptionRawConfig {
        val server = parseServer(baseUrl)
        val feedIds = currentFeedIds.takeIf { it.isNotBlank() }
            ?: availableFeedIds
                .distinct()
                .take(MAX_EXAMPLE_FEEDS)
                .joinToString(InterceptionSettingsCodec.ITEM_SEPARATOR)
        val rules = if (server.isValid) {
            "images.${server.host}=>${server.scheme}://${server.host}/"
        } else {
            ""
        }
        return InterceptionRawConfig(
            enabled = true,
            domains = server.host,
            feedIds = feedIds,
            refererRules = rules
        )
    }
}
