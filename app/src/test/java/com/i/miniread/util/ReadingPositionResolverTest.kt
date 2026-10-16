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
    fun contentDidNotFillViewportAtSaveTime_usesRatio() {
        // saved.viewportHeight(2000) > saved.contentHeight(1500) → viewport 不可信 → 跳过贴底吸附，
        // 落到文档相对比例：ratio = 5/1500 → 5/1500 * 9000 = 30
        val saved = snap(5, 1500, 2000)
        val current = snap(0, 9000, 1200)
        assertEquals(30, ReadingPositionResolver.resolve(saved, current))
    }

    @Test
    fun corruptSavedViewport_largerThanContent_usesRatio() {
        // viewport 异常测量（大于内容高度）不得触发贴底，应走比例：0.2 * 9000 = 1800
        val saved = snap(1000, 5000, 6000)
        val current = snap(0, 9000, 1200)
        assertEquals(1800, ReadingPositionResolver.resolve(saved, current))
    }

    @Test
    fun corruptSavedViewport_zero_usesRatio() {
        // viewport 为 0（布局未稳定）不得触发贴底（savedMaxScroll 会失真），应走比例
        val saved = snap(1000, 5000, 0)
        val current = snap(0, 9000, 1200)
        assertEquals(1800, ReadingPositionResolver.resolve(saved, current))
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

    // ===== needsRestoreAfterLayoutChange：纯布局变化（旋转/分屏，页面未重载）的守卫判据 =====

    @Test
    fun layoutUnchanged_noRestore() {
        // 布局与位置都没变：原生落点即正确，不打扰
        val s = snap(1000, 5000, 2000)
        assertEquals(false, ReadingPositionResolver.needsRestoreAfterLayoutChange(s, s))
    }

    @Test
    fun layoutChange_anchorPreserved_noRestore() {
        // 文档重排（5000 → 9000）但原生把同一段落留在了视口（比例一致 0.2）
        val native = snap(1800, 9000, 1200)
        val saved = snap(1000, 5000, 2000)
        assertEquals(false, ReadingPositionResolver.needsRestoreAfterLayoutChange(native, saved))
    }

    @Test
    fun layoutChange_pixelOffsetOnly_restores() {
        // 文档重排但原生只保留了像素偏移（scrollY 仍是 1000），
        // 比例映射目标 = 0.2 * 9000 = 1800，偏差 800 > 容差 0.5*1200=600 → 需要纠正
        val native = snap(1000, 9000, 1200)
        val saved = snap(1000, 5000, 2000)
        assertEquals(true, ReadingPositionResolver.needsRestoreAfterLayoutChange(native, saved))
    }

    @Test
    fun layoutChange_withinTolerance_noRestore() {
        // saved 比例 4000/5000 = 0.8；映射目标 = 0.8 * 9000 = 7200，原生 7100，偏差 100 < 600
        val native = snap(7100, 9000, 1200)
        val saved = snap(4000, 5000, 2000)
        assertEquals(false, ReadingPositionResolver.needsRestoreAfterLayoutChange(native, saved))
    }

    @Test
    fun layoutChange_nativeScrollLost_restores() {
        // 原生位置丢失（scrollY == 0）而保存位置非零 → 需要兜底恢复
        val native = snap(0, 9000, 1200)
        val saved = snap(1000, 5000, 2000)
        assertEquals(true, ReadingPositionResolver.needsRestoreAfterLayoutChange(native, saved))
    }

    @Test
    fun layoutChange_nativeNotMeasurable_restores() {
        // 原生测量不可用（高度为 0）→ 交给上层重试兜底
        val native = snap(0, 0, 0)
        val saved = snap(1000, 5000, 2000)
        assertEquals(true, ReadingPositionResolver.needsRestoreAfterLayoutChange(native, saved))
    }

    @Test
    fun layoutChange_noSavedOrAtTop_noRestore() {
        val native = snap(500, 9000, 1200)
        assertEquals(false, ReadingPositionResolver.needsRestoreAfterLayoutChange(native, null))
        assertEquals(
            false,
            ReadingPositionResolver.needsRestoreAfterLayoutChange(native, snap(0, 5000, 2000))
        )
    }
}