package com.i.miniread.util

/** 从剪贴板导入时解析出的 4 个字段（未解析）。 */
data class ImportedConfig(
    val enabled: Boolean,
    val domains: String,
    val feedIds: String,
    val refererRules: String
)

/**
 * 隐藏设置页的「导出 / 导入到剪贴板」文本格式。
 *
 * 格式固定为 4 行 `key=value`，末尾无换行：
 * ```
 * enabled=false
 * domains=
 * feedIds=
 * refererRules=
 * ```
 *
 * ⚠️ 这是用户唯一的跨设备备份手段（DataStore 被备份规则整体排除），**格式不得变动**。
 * 从 [com.i.miniread.ui.HiddenSettingsScreen] 上移到这里，只是为了能加纯 JVM 回归测试，
 * 行为与上移前逐字一致（包括「命中任一 key 即认为导入成功」这一旧语义）。
 */
object InterceptionClipboardFormat {

    fun buildExportText(
        enabled: Boolean,
        domains: String,
        feedIds: String,
        refererRules: String
    ): String = buildString {
        append("enabled=").append(enabled).append('\n')
        append("domains=").append(domains).append('\n')
        append("feedIds=").append(feedIds).append('\n')
        append("refererRules=").append(refererRules)
    }

    /** 解析剪贴板文本；无法识别任何已知 key 时返回 null（视为解析失败）。 */
    fun parseImported(text: String): ImportedConfig? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        var enabled = "false"
        var domains = ""
        var feedIds = ""
        var refererRules = ""
        var matched = false

        trimmed.split('\n').forEach { rawLine ->
            val line = rawLine.trim()
            val idx = line.indexOf('=')
            if (idx <= 0) return@forEach
            val key = line.substring(0, idx).trim()
            val value = line.substring(idx + 1).trim()
            when (key) {
                "enabled" -> { enabled = value; matched = true }
                "domains" -> { domains = value; matched = true }
                "feedIds" -> { feedIds = value; matched = true }
                "refererRules" -> { refererRules = value; matched = true }
            }
        }

        if (!matched) return null
        return ImportedConfig(
            enabled = enabled.equals("true", ignoreCase = true),
            domains = domains,
            feedIds = feedIds,
            refererRules = refererRules
        )
    }
}
