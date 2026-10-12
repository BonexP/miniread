package com.i.miniread.util

/**
 * Feed 列表排序工具。
 *
 * 本对象是 MinifluxViewModel.loadCustomFeedOrder() 原排序逻辑的纯函数抽取，
 * 行为与原始实现完全等价（便于在纯 JVM 单元测试中验证）：
 *
 * 原始实现：
 * ```
 * val feedOrderMap = DataStoreManager.getFeedOrder()
 * if (feedOrderMap.isEmpty()) {
 *     feeds.sortedBy { it.title }                        // 无自定义排序：按标题升序
 * } else {
 *     feeds.sortedBy { feed -> feedOrderMap[feed.id] ?: Int.MAX_VALUE }  // 有排序：按索引升序
 * }
 * ```
 *
 * 注意：`sortedBy` 是稳定排序，键相同者保持输入顺序。
 */
object FeedSorter {

    /**
     * 按自定义顺序 [order] 对 [feeds] 排序。
     *
     * @param feeds 待排序列表。
     * @param order FeedId -> OrderIndex 映射；为空时按标题升序。
     * @param idOf 从元素中取出 FeedId。
     * @param titleOf 从元素中取出标题（仅在 [order] 为空时使用）。
     * @return 排序后的新列表（不修改输入）。
     */
    fun <T> sort(
        feeds: List<T>,
        order: Map<Int, Int>,
        idOf: (T) -> Int,
        titleOf: (T) -> String
    ): List<T> {
        return if (order.isEmpty()) {
            // 没有保存的排序，返回按标题排序的列表
            feeds.sortedBy { titleOf(it) }
        } else {
            // 按照保存的顺序排列；未命中（无索引）的项排在最后，与 Int.MAX_VALUE 占位一致
            feeds.sortedBy { order[idOf(it)] ?: Int.MAX_VALUE }
        }
    }
}
