package com.i.miniread.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * 阅读页 HTML 组装的纯函数集合（无 Android 依赖，可直接跑纯 JVM 单测）。
 *
 * 职责边界：
 * - **标题 / 订阅源名是文本**，必须转义后才能拼进 HTML；
 * - **正文是 Miniflux 返回的 HTML**，原样透传（转义会毁掉正文）；
 * - 发布时间统一在这里格式化，避免与列表页出现两套口径。
 *
 * ⚠️ 本文件被 `ArticleDetailScreen` 的 `remember(...)` 直接调用，
 * 任何新入参都要同步加进那个 `remember` 的 key，否则内容变化不会重建 HTML。
 */
object ArticleHtml {

    private const val INPUT_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX"
    private const val OUTPUT_TIME_PATTERN = "yyyy-MM-dd HH:mm"

    /** Go 的 RFC3339Nano 可能带 6~9 位小数秒，SimpleDateFormat 只认 3 位，先剥掉。 */
    private val FRACTIONAL_SECONDS = Regex("""\.\d+""")

    /**
     * 转义 HTML 文本节点。
     *
     * ⚠️ 逐字符扫描而不是链式 `replace`：链式写法一旦顺序调整（先 `<` 后 `&`），
     * 已生成的 `&lt;` 会被再转义成 `&amp;lt;`，且难以测试。逐字符没有顺序问题。
     */
    fun escapeHtml(value: String): String = buildString(value.length) {
        for (ch in value) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(ch)
            }
        }
    }

    /**
     * 把 Miniflux 的 RFC3339 发布时间转成**指定时区**的 `yyyy-MM-dd HH:mm`。
     *
     * ⚠️ 用 `java.text` 而不是 `java.time`：`minSdk = 25` 且未启用 core library desugaring，
     * `java.time` 需要 API 26（项目里 `EntryListScreen.localizePublishTime` 的
     * `@RequiresApi(O)` 就是这个约束）。这里走 `SimpleDateFormat` 可以不引新的 API 下限。
     *
     * @param raw Miniflux 的 `published_at`（形如 `2026-10-17T04:34:56Z`）
     * @param timeZone 目标时区，默认设备时区；单测固定传 `Asia/Shanghai`
     * @return 解析失败或空值返回 `null`（meta 行会自动省略时间，不显示原始脏串）
     */
    fun formatPublishedAt(raw: String?, timeZone: TimeZone = TimeZone.getDefault()): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null

        val normalized = value.replace(FRACTIONAL_SECONDS, "")
        val parsed = try {
            SimpleDateFormat(INPUT_TIME_PATTERN, Locale.US).apply {
                isLenient = false
                this.timeZone = TimeZone.getTimeZone("UTC")
            }.parse(normalized)
        } catch (e: ParseException) {
            return null
        } ?: return null

        return SimpleDateFormat(OUTPUT_TIME_PATTERN, Locale.US).apply {
            this.timeZone = timeZone
        }.format(parsed)
    }
}
