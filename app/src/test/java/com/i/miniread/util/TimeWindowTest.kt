package com.i.miniread.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * TimeWindow 纯 JVM 单元测试。
 *
 * 覆盖 MinifluxViewModel.fetchTodayEntries() 抽取出的时间戳公式：
 * publishedAfterForLast24h(now) == now / 1000 - 86400
 */
class TimeWindowTest {

    @Test
    fun zeroMillis_matchesFormula() {
        assertEquals(-86400L, TimeWindow.publishedAfterForLast24h(0L))
        assertEquals(0L / 1000 - 86400L, TimeWindow.publishedAfterForLast24h(0L))
    }

    @Test
    fun epochMillis_matchesFormula() {
        // 一个真实的纪元时间：2023-11-14T22:13:20Z = 1_700_000_000_000 ms
        val now = 1_700_000_000_000L
        assertEquals(now / 1000 - 86400L, TimeWindow.publishedAfterForLast24h(now))
        assertEquals(1_700_000_000L - 86400L, TimeWindow.publishedAfterForLast24h(now))
    }

    @Test
    fun maxLongValue_matchesFormula_noOverflow() {
        val now = Long.MAX_VALUE
        // Long.MAX_VALUE / 1000 - 86400，减法不会溢出
        assertEquals(now / 1000 - 86400L, TimeWindow.publishedAfterForLast24h(now))
        assertEquals(9223372036854775L - 86400L, TimeWindow.publishedAfterForLast24h(now))
    }

    @Test
    fun exactlyOneDayInMillis_yieldsZero() {
        // 恰好一天的毫秒数 -> 起始时间戳为 0
        assertEquals(0L, TimeWindow.publishedAfterForLast24h(86_400_000L))
    }

    @Test
    fun crossDayBoundary_differenceIsExactly86400Seconds() {
        // 任意基准时间 t，与 t + 24h 之间，published_after 差值恰好为 86400 秒
        val t = 1_699_999_999_000L
        val oneDayLater = t + 86_400_000L

        val diff = TimeWindow.publishedAfterForLast24h(oneDayLater) -
            TimeWindow.publishedAfterForLast24h(t)

        assertEquals(86400L, diff)
    }

    @Test
    fun subSecondMillisTruncatesTowardsZero() {
        // 整数除法：不足 1 秒的毫秒被丢弃
        val now = 1_700_000_000_999L // 余数 999ms
        assertEquals(1_700_000_000L - 86400L, TimeWindow.publishedAfterForLast24h(now))
    }
}
