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
 *    **与缩放比例无关**；`assets/template.html` 未声明 `<meta name="viewport">`，
 *    CSS 布局宽度固定在约 980px，旋转只改 page scale、不改 CSS 布局，
 *    因此按文档比例换算能精确落回同一段文字。
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

        // 5. 贴底吸附：保存时已到底 / 本就滚不动 → 恢复后也贴底
        val savedMaxScroll = (saved.contentHeight - saved.viewportHeight).coerceAtLeast(0)
        if (savedMaxScroll <= bottomEpsilonPx ||
            saved.scrollY >= savedMaxScroll - bottomEpsilonPx
        ) {
            return currentMaxScroll
        }

        // 6. 主策略：按文档相对比例换算，并 clamp 到当前可滚动范围
        val ratio = (saved.scrollY.toDouble() / saved.contentHeight).coerceIn(0.0, 1.0)
        val target = (ratio * current.contentHeight).roundToInt()
        return target.coerceIn(0, currentMaxScroll)
    }
}