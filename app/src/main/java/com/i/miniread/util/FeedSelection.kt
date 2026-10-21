package com.i.miniread.util

/**
 * 订阅源白名单（逗号分隔的 id 文本）的纯逻辑。
 *
 * 与 [InterceptionSettingsCodec.parseFeedIds] 保持**同一套解析口径**（分隔符、丢弃非数字），
 * 区别只有两点，都是选择器必需的：
 * 1. **保序**：`26,38,52,51` 解析回来仍是这个顺序，保存后不会被重排；
 * 2. **可增删**：`toggle` / `selectAll` 只做增删，绝不因为搜索过滤或空列表裁剪已选项。
 *
 * 无 Android 依赖，可纯 JVM 单测。
 */
object FeedSelection {

    /** 选择器里的一行：订阅源编号 + 标题。 */
    data class Option(val id: Int, val title: String)

    /**
     * 选择器某一时刻的视图数据。
     *
     * @param selected 已选 id，保序（来自 csv 原文）
     * @param available 命中搜索词、且存在于订阅源列表中的项（保序同 feeds）
     * @param missing 已选但**不在**订阅源列表中的 id（可能订阅源已被删除 / 尚未加载）
     * @param visibleIds `available` 的 id 列表，「全选当前结果」用
     */
    data class View(
        val selected: List<Int>,
        val available: List<Option>,
        val missing: List<Int>,
        val visibleIds: List<Int>
    )

    /** 解析为 id 列表：保序、去重、丢弃非数字项（与 [InterceptionSettingsCodec.parseFeedIds] 同口径）。 */
    fun parseCsv(csv: String?): List<Int> =
        InterceptionSettingsCodec.splitEntries(csv)
            .mapNotNull { it.toIntOrNull() }
            .distinct()

    /** id 列表 → 逗号分隔文本。 */
    fun encode(ids: List<Int>): String =
        ids.joinToString(InterceptionSettingsCodec.ITEM_SEPARATOR)

    /** 勾选 / 取消勾选：已存在则移除，不存在则**追加到末尾**（幂等，不重排已有项）。 */
    fun toggle(csv: String?, id: Int): String {
        val ids = parseCsv(csv)
        val next = if (id in ids) ids.filterNot { it == id } else ids + id
        return encode(next)
    }

    /** 批量勾选：只补当前缺失的项，已选项顺序不变。 */
    fun selectAll(csv: String?, ids: List<Int>): String {
        val current = parseCsv(csv)
        val missing = ids.filterNot { it in current }.distinct()
        return encode(if (missing.isEmpty()) current else current + missing)
    }

    /** 清空全部（含搜索外与失效项）。 */
    fun clearAll(): String = ""

    /**
     * 搜索匹配：空查询恒真；`#26` / `26` 按编号精确匹配；否则按标题忽略大小写包含匹配。
     */
    fun matches(query: String, option: Option): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        val numeric = q.removePrefix("#").toIntOrNull()
        if (numeric != null && numeric == option.id) return true
        return option.title.contains(q, ignoreCase = true)
    }

    /**
     * 生成选择器视图。
     *
     * ⚠️ `missing` **不受 query 影响**：搜索时必须始终显示失效项，否则用户既搜不到、
     * 也看不到那几条过期 id，等于静默丢失配置。
     */
    fun buildView(csv: String?, feeds: List<Option>, query: String): View {
        val selected = parseCsv(csv)
        val known = feeds.mapTo(HashSet()) { it.id }
        val missing = selected.filterNot { it in known }
        val available = feeds.filter { matches(query, it) }
        return View(
            selected = selected,
            available = available,
            missing = missing,
            visibleIds = available.map { it.id }
        )
    }
}
