package com.i.miniread.util

import kotlin.math.roundToInt

/**
 * 阅读位置快照：WebView 某一时刻的滚动位置与可视区域尺寸。
 *
 * ⚠️ 三个字段必须同为 **view px**（不乘 scale），写入与恢复要走同一口径，
 * 否则比例换算会引入 scale 比值的系统偏差。
 */
data class ScrollSnapshot(
    val scrollY: Int,
    val contentHeight: Int,
    val viewportHeight: Int
)

/**
 * 阅读位置换算工具（纯函数，无 Android 依赖，可直接跑纯 JVM 单测）。
 *
 * 用途：旋转屏幕 / 分屏 resize / WebView 重建之后，把保存的滚动位置换算成
 * 新布局下的目标 scrollY，避免阅读进度归零。
 *
 * 换算策略（两条，顺序敏感）：
 *
 * 1. **贴底吸附**：保存时已停在底部（或保存时内容本就不满屏），恢复后直接贴到新布局底部。
 * 2. **文档相对比例**：`savedScrollY / savedContentHeight` → 乘以当前 `contentHeight`。
 *    `scrollY` 与 `contentHeight` 同为 view px，其比值即「CSS 文档中的相对位置」，
 *    **与布局宽度 / 缩放比例无关**。template.html 已改为响应式
 *    （`<meta name="viewport" content="width=device-width, initial-scale=1">`），
 *    旋转 / 分屏会触发真实 CSS 重排、`contentHeight` 随之变化，
 *    但文档比例换算不受影响，仍能落回同一段文字附近。
 */
object ReadingPositionResolver {

    /** 判定「已停在底部」的容差（px），与既有代码 remainingScroll < 50 同量级。 */
    private const val BOTTOM_EPSILON_PX = 64f

    /**
     * 把保存时的滚动位置换算为当前布局下的目标 scrollY。
     *
     * @param saved 保存时的快照。
     * @param current 恢复时的快照（布局已稳定、尺寸有效）。
     * @param bottomEpsilonPx 贴底判定容差（px）。
     * @return 目标 scrollY，已 clamp 到 `[0, currentMaxScroll]`；无法恢复时返回 0。
     */
    fun resolve(
        saved: ScrollSnapshot,
        current: ScrollSnapshot,
        bottomEpsilonPx: Float = BOTTOM_EPSILON_PX
    ): Int {
        // 1. 已在顶部：无需恢复，也不让 0 参与比例运算
        if (saved.scrollY <= 0) return 0

        // 2. 恢复侧尚未布局完成：交给调用方重试，此处不给出错误位置
        if (current.contentHeight <= 0 || current.viewportHeight <= 0) return 0

        val currentMaxScroll = (current.contentHeight - current.viewportHeight).coerceAtLeast(0)

        // 3. 布局完全一致：原样返回（精确还原，也避开浮点误差）
        if (saved == current) {
            return saved.scrollY.coerceIn(0, currentMaxScroll)
        }

        // 4. 保存时内容高度不可信（写入侧已过滤，这里兜底，避免除零）
        if (saved.contentHeight <= 0) return 0

        // 5. 贴底吸附：保存时已到底 / 本就滚不动 → 恢复后也贴底。
        //    守卫：仅当 saved.viewportHeight 可信（0 < viewport <= contentHeight）才允许贴底判定——
        //    布局未稳定时的异常测量会让 savedMaxScroll 失真并误触发贴底；
        //    viewport 不可信时跳过贴底，落到第 6 步「文档相对比例」（只用 scrollY/contentHeight，
        //    天然免疫 viewport 损坏）。
        val viewportTrustworthy = saved.viewportHeight in 1..saved.contentHeight
        val savedMaxScroll = (saved.contentHeight - saved.viewportHeight).coerceAtLeast(0)
        if (viewportTrustworthy &&
            (savedMaxScroll <= bottomEpsilonPx ||
                saved.scrollY >= savedMaxScroll - bottomEpsilonPx)
        ) {
            return currentMaxScroll
        }

        // 6. 主策略：按文档相对比例换算，并 clamp 到当前可滚动范围
        val ratio = (saved.scrollY.toDouble() / saved.contentHeight).coerceIn(0.0, 1.0)
        val target = (ratio * current.contentHeight).roundToInt()
        return target.coerceIn(0, currentMaxScroll)
    }

    /**
     * 纯布局变化（旋转 / 分屏 resize，页面**未重新加载**）后，判断是否需要按保存快照重新定位。
     *
     * 背景：合并 NavHost 后旋转不再重建 WebView，滚动位置主要由 Chromium 原生保留；但旋转会
     * 触发 CSS 重排（width/height 对调），原生可能只保留「像素偏移」而没有做滚动锚定，此时
     * 落点会偏离到文章后段。本函数用「文档相对比例」判断原生落点是否已在正确位置附近：
     *
     *  - 保存位置在顶部 / 无快照            → false（无需恢复，不打扰）
     *  - 原生位置丢失（scrollY <= 0）       → true（交给比例映射兜底）
     *  - 原生测量不可用（高度为 0）         → true（交给上层重试）
     *  - 原生落点与「比例映射目标」偏差小于容差 → false（原生已正确，不要多动一次 scrollTo）
     *  - 偏差过大（仅保留了像素偏移）        → true（按比例纠正）
     *
     * @param native 布局变化并稳定后 WebView 的当前快照。
     * @param saved 布局变化前保存的快照；null 表示从未读过。
     * @param toleranceViewports 容差（单位：视口高度倍数），默认半屏。
     */
    fun needsRestoreAfterLayoutChange(
        native: ScrollSnapshot,
        saved: ScrollSnapshot?,
        toleranceViewports: Float = 0.5f
    ): Boolean {
        if (saved == null || saved.scrollY <= 0) return false
        if (native.contentHeight <= 0 || native.viewportHeight <= 0) return true
        if (native.scrollY <= 0) return true

        val savedFraction = saved.scrollY.toDouble() / saved.contentHeight
        val expected = (savedFraction * native.contentHeight).roundToInt()
        val tolerancePx = (native.viewportHeight * toleranceViewports).roundToInt()
        return kotlin.math.abs(native.scrollY - expected) > tolerancePx
    }
}