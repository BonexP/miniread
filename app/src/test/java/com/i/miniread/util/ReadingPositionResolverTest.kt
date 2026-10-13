package com.i.miniread.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ReadingPositionResolver 纯 JVM 单元测试。
 *
 * 覆盖阅读页「旋转 / resize / WebView 重建后恢复滚动位置」的换算规则：
 * 顶部早退、未布局保护、贴底吸附、文档相对比例换算、clamp 与极端值。
 */
class ReadingPositionResolverTest {

    private fun snap(scrollY: Int, contentHeight: Int, viewportHeight: Int) =
        ScrollSnapshot(scrollY, contentHeight, viewportHeight)

    @Test
    fun sameLayout_restoresExactScrollY() {
        val s = snap(1000, 5000, 2000)
        assertEquals(1000, ReadingPositionResolver.resolve(s, s))
    }

    @Test
    fun sameLayoutButScrollFieldDiffers_stillExact() {
        // 布局（内容高度/视口高度）完全相同，只有 scrollY 字段不同
        val saved = snap(1000, 5000, 2000)
        val current = snap(0, 5000, 2000)
        // ratio = 1000/5000 = 0.2 → 0.2 * 5000 = 1000
        assertEquals(1000, ReadingPositionResolver.resolve(saved, current))
    }

    @Test
    fun sameLayout_atBottom_restoresBottom() {
        val s = snap(3000, 5000, 2000)
        assertEquals(3000, ReadingPositionResolver.resolve(s, s))
    }

    @Test
    fun contentGrowsAfterRotation_landsAtSameDocumentFraction() {
        // 竖屏：内容 5000、视口 2000，滚到 1000（ratio 0.2）
        // 横屏：内容 9000、视口 1200 → 0.2 * 9000 = 1800
        val saved = snap(1000, 5000, 2000)
        val current = snap(0, 9000, 1200)
        assertEquals(1800, ReadingPositionResolver.resolve(saved, current))
    }

    @Test
    fun contentShrinksAfterRotation_landsAtSameDocumentFraction() {
        // ratio = 2000/8000 = 0.25 → 0.25 * 6000 = 1500
        val saved = snap(2000, 8000, 3000)
        val current = snap(0, 6000, 1000)
        assertEquals(1500, ReadingPositionResolver.resolve(saved, current))
    }

    @Test
    fun ratioIsRoundedToNearestInt() {
        // ratio = 1000/3000 = 1/3 → 4000/3 = 1333.33 → 1333
        val saved = snap(1000, 3000, 1000)
        val current = snap(0, 4000, 1000)
        assertEquals(1333, ReadingPositionResolver.resolve(saved, current))
    }

    @Test
    fun atBottom_snapsToNewBottom() {
        // savedMaxScroll = 3000，4900 >= 3000 - 64 → 判定为贴底
        val saved = snap(4900, 5000, 2000)
        val current = snap(0, 9000, 1200)
        assertEquals(7800, ReadingPositionResolver.resolve(saved, current))
    }

    @Test
    fun contentDidNotFillViewportAtSaveTime_snapsToNewBottom() {
        // savedMaxScroll = 0（保存时内容不满屏）→ 贴底吸附
        val saved = snap(5, 1500, 2000)
        val current = snap(0, 9000, 1200)
        assertEquals(7800, ReadingPositionResolver.resolve(saved, current))
    }

    @Test
    fun zeroSavedScrollY_returnsZero() {
        val saved = snap(0, 5000, 2000)
        assertEquals(0, ReadingPositionResolver.resolve(saved, snap(0, 5000, 2000)))
    }

    @Test
    fun negativeSavedScrollY_returnsZero() {
        val saved = snap(-100, 5000, 2000)
        assertEquals(0, ReadingPositionResolver.resolve(saved, snap(0, 5000, 2000)))
    }

    @Test
    fun currentContentShorterThanViewport_returnsZero() {
        // currentMaxScroll = max(800 - 2000, 0) = 0 → clamp 到 0
        val saved = snap(1000, 5000, 2000)
        val current = snap(0, 800, 2000)
        assertEquals(0, ReadingPositionResolver.resolve(saved, current))
    }

    @Test
    fun currentNotYetLaidOut_returnsZero_soCallerRetries() {
        // contentHeight 仍为 0（WebView 未完成布局）→ 返回 0，由调用方重试
        val saved = snap(1000, 5000, 2000)
        assertEquals(0, ReadingPositionResolver.resolve(saved, snap(0, 0, 2000)))
        assertEquals(0, ReadingPositionResolver.resolve(saved, snap(0, 5000, 0)))
    }

    @Test
    fun savedContentHeightCorrupt_returnsZero() {
        // 损坏快照：saved.contentHeight <= 0 → 第 4 道守卫直接返回 0，不参与任何比例/贴底运算
        val saved = snap(1000, 0, 2000)
        assertEquals(0, ReadingPositionResolver.resolve(saved, snap(0, 5000, 2000)))
    }

    @Test
    fun resolvedScrollIsClampedToCurrentMaxScroll() {
        // ratio = 2900/5000 = 0.58 → 0.58 * 2100 = 1218，但 currentMaxScroll 只有 100
        val saved = snap(2900, 5000, 2000)
        val current = snap(0, 2100, 2000)
        assertEquals(100, ReadingPositionResolver.resolve(saved, current))
    }

    @Test
    fun savedScrollYExceedsContent_snapsToBottom() {
        // 损坏快照：scrollY 远大于 contentHeight → 走贴底吸附，不会返回越界值
        val saved = snap(9999, 5000, 2000)
        val current = snap(0, 5000, 2000)
        assertEquals(3000, ReadingPositionResolver.resolve(saved, current))
    }

    @Test
    fun hugeValues_noOverflow() {
        // ratio = 500000/2000000 = 0.25 → 0.25 * 3000000 = 750000（用 Double 计算，无整型溢出）
        val saved = snap(500_000, 2_000_000, 600_000)
        val current = snap(0, 3_000_000, 400_000)
        assertEquals(750_000, ReadingPositionResolver.resolve(saved, current))
    }

    @Test
    fun customBottomEpsilon_isHonored() {
        // 把容差调大到 500：savedMaxScroll=3000，2900 >= 3000-500=2500 → 贴底
        val saved = snap(2900, 5000, 2000)
        val current = snap(0, 9000, 1200)
        assertEquals(7800, ReadingPositionResolver.resolve(saved, current, bottomEpsilonPx = 500f))
    }
}