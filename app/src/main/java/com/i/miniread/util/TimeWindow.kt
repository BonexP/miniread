package com.i.miniread.util

/**
 * 时间窗口工具。
 *
 * 本对象是 MinifluxViewModel.fetchTodayEntries() 中「最近 24 小时」时间戳计算的
 * 纯函数抽取，行为与原始实现完全等价（便于在纯 JVM 单元测试中验证）：
 *
 * 原始实现：
 * ```
 * val unixTime = System.currentTimeMillis() / 1000
 * val befeore24h = unixTime - 86400
 * ```
 */
object TimeWindow {

    /** 一天的秒数。 */
    private const val SECONDS_PER_DAY = 86400L

    /**
     * 计算「最近 24 小时」的起始 Unix 时间戳（秒）。
     *
     * 与原始公式 `nowMillis / 1000 - 86400` 等价。
     * 使用整数除法（向零取整），与 Kotlin Long 除法语义一致。
     *
     * @param nowMillis 当前时间（毫秒，通常为 System.currentTimeMillis()）。
     * @return published_after 查询参数值（秒）。
     */
    fun publishedAfterForLast24h(nowMillis: Long): Long {
        return nowMillis / 1000 - SECONDS_PER_DAY
    }
}
