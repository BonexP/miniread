package com.i.miniread.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * FeedSorter 纯 JVM 单元测试。
 *
 * 覆盖 MinifluxViewModel.loadCustomFeedOrder() 抽取出的排序语义：
 * - order 为空 -> 按 title 升序
 * - order 非空 -> 按 order[id] 升序，未命中排最后（Int.MAX_VALUE 占位）
 * - 空列表、重复 id 的稳定性
 */
class FeedSorterTest {

    private data class Item(val id: Int, val title: String)

    private val idOf: (Item) -> Int = { it.id }
    private val titleOf: (Item) -> String = { it.title }

    @Test
    fun emptyOrder_sortsByTitleAscending() {
        val feeds = listOf(
            Item(3, "Charlie"),
            Item(1, "Alpha"),
            Item(2, "Bravo")
        )

        val sorted = FeedSorter.sort(feeds, emptyMap(), idOf, titleOf)

        assertEquals(listOf(1, 2, 3), sorted.map { it.id })
        assertEquals(listOf("Alpha", "Bravo", "Charlie"), sorted.map { it.title })
    }

    @Test
    fun withOrder_sortsByIndexAscending() {
        val feeds = listOf(
            Item(10, "Ten"),
            Item(20, "Twenty"),
            Item(30, "Thirty")
        )
        // 自定义顺序：30 第一，10 第二，20 第三
        val order = mapOf(30 to 0, 10 to 1, 20 to 2)

        val sorted = FeedSorter.sort(feeds, order, idOf, titleOf)

        assertEquals(listOf(30, 10, 20), sorted.map { it.id })
    }

    @Test
    fun withOrder_itemsMissingFromOrderGoLast() {
        val feeds = listOf(
            Item(1, "One"),
            Item(2, "Two"),
            Item(3, "Three"),
            Item(99, "NinetyNine")
        )
        // 仅 1 与 3 有自定义索引；2 与 99 未命中，应排在最后
        val order = mapOf(3 to 0, 1 to 1)

        val sorted = FeedSorter.sort(feeds, order, idOf, titleOf)

        assertEquals(listOf(3, 1), sorted.take(2).map { it.id })
        // 未命中项位于尾部，且保持相对输入顺序（稳定排序）
        assertEquals(listOf(2, 99), sorted.drop(2).map { it.id })
    }

    @Test
    fun emptyList_returnsEmpty() {
        val sorted = FeedSorter.sort(emptyList<Item>(), mapOf(1 to 0), idOf, titleOf)
        assertEquals(0, sorted.size)
    }

    @Test
    fun withOrder_duplicateIds_areStable() {
        // 两个元素 id 相同 -> 映射到同一索引，稳定排序应保持其输入顺序
        val feeds = listOf(
            Item(5, "First"),
            Item(5, "Second"),
            Item(6, "Other")
        )
        val order = mapOf(5 to 0, 6 to 1)

        val sorted = FeedSorter.sort(feeds, order, idOf, titleOf)

        assertEquals(listOf("First", "Second", "Other"), sorted.map { it.title })
    }

    @Test
    fun emptyOrder_duplicateTitles_areStable() {
        val feeds = listOf(
            Item(2, "Same"),
            Item(1, "Same")
        )

        val sorted = FeedSorter.sort(feeds, emptyMap(), idOf, titleOf)

        // 标题相同 -> 保持输入顺序
        assertEquals(listOf(2, 1), sorted.map { it.id })
    }

    @Test
    fun doesNotMutateInput() {
        val feeds = listOf(
            Item(2, "Bravo"),
            Item(1, "Alpha")
        )
        val original = feeds.toList()

        FeedSorter.sort(feeds, emptyMap(), idOf, titleOf)

        assertEquals(original, feeds)
    }
}
