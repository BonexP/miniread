package com.i.miniread.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FeedSelection 纯 JVM 单元测试。
 *
 * 锁定选择器的三条核心语义：
 * - 解析口径与 InterceptionSettingsCodec 一致（分隔符、丢弃非数字）
 * - 增删**保序**，新项追加到末尾（历史的 `26,38,52,51` 不会被重排）
 * - 失效 id 永不因搜索 / 空列表被裁剪
 */
class FeedSelectionTest {

    private fun option(id: Int, title: String) = FeedSelection.Option(id, title)

    private val feeds = listOf(
        option(26, "少数派"),
        option(38, "阮一峰的网络日志"),
        option(51, "Matrix"),
        option(52, "爱范儿")
    )

    // --- parseCsv ---

    @Test
    fun parseCsv_preservesOrderAndDropsNonNumeric() {
        assertEquals(
            listOf(26, 38, 52, 51),
            FeedSelection.parseCsv("26,38,52,51")
        )
        assertEquals(listOf(26, 38), FeedSelection.parseCsv("26,abc,38"))
    }

    @Test
    fun parseCsv_chineseCommaIsNotSeparator() {
        // 中文逗号不参与切分 → 整条不是数字 → 全丢。诊断层会对此单独给中文标点提示。
        assertTrue(FeedSelection.parseCsv("26，38").isEmpty())
    }

    // --- toggle ---

    @Test
    fun toggle_addsAtEnd_keepsExistingOrder() {
        assertEquals("26,38,52", FeedSelection.toggle("26,38", 52))
    }

    @Test
    fun toggle_removesWhenSelected() {
        assertEquals("26,52", FeedSelection.toggle("26,38,52", 38))
    }

    @Test
    fun toggle_twiceRestoresOriginal() {
        val once = FeedSelection.toggle("26,38", 52)
        assertEquals("26,38", FeedSelection.toggle(once, 52))
    }

    // --- selectAll / clearAll ---

    @Test
    fun selectAll_appendsOnlyMissing() {
        assertEquals("26,38,52,51", FeedSelection.selectAll("26,38", listOf(38, 52, 51, 26)))
    }

    @Test
    fun selectAll_doesNotReorder() {
        assertEquals("51,26,38", FeedSelection.selectAll("51,26", listOf(26, 38, 51)))
    }

    @Test
    fun selectAll_emptyIds_isNoOp() {
        assertEquals("26,38", FeedSelection.selectAll("26,38", emptyList()))
    }

    @Test
    fun clearAll_returnsEmpty() {
        assertEquals("", FeedSelection.clearAll())
    }

    // --- buildView ---

    @Test
    fun buildView_blankQueryShowsAllFeeds() {
        val view = FeedSelection.buildView("26", feeds, "")
        assertEquals(4, view.available.size)
        assertEquals(listOf(26, 38, 51, 52), view.visibleIds)
        assertEquals(listOf(26), view.selected)
    }

    @Test
    fun buildView_filtersByTitleCaseInsensitive() {
        val view = FeedSelection.buildView("", feeds, "matrix")
        assertEquals(listOf(51), view.visibleIds)
    }

    @Test
    fun buildView_filtersByNumericId() {
        assertEquals(listOf(38), FeedSelection.buildView("", feeds, "#38").visibleIds)
        assertEquals(listOf(38), FeedSelection.buildView("", feeds, "38").visibleIds)
    }

    @Test
    fun buildView_keepsMissingIdsWhenQueryFiltersThemOut() {
        val view = FeedSelection.buildView("26,999", feeds, "matrix")
        // 999 不在订阅列表中，搜索也不能把它藏起来
        assertEquals(listOf(999), view.missing)
        assertEquals(listOf(51), view.visibleIds)
        assertEquals(listOf(26, 999), view.selected)
    }

    @Test
    fun buildView_emptyFeeds_returnsAllSelectedAsMissing() {
        val view = FeedSelection.buildView("26,38", emptyList(), "")
        assertEquals(listOf(26, 38), view.missing)
        assertTrue(view.available.isEmpty())
        assertTrue(view.visibleIds.isEmpty())
    }
}
