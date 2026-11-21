package com.i.miniread.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.i.miniread.BuildConfig

/**
 * 列表卡片（standard / E-Ink 双形态的**唯一**实现）。
 *
 * ## 为什么存在
 * 此前 `CategoryItem` / `FeedItem` / `EntryItem` 各自手写了一遍同样的分支：
 *
 * ```kotlin
 * colors = if (BuildConfig.IS_EINK) CardDefaults.cardColors(containerColor = Color.White)
 *          else CardDefaults.cardColors()
 * modifier = Modifier.then(if (BuildConfig.IS_EINK)
 *          Modifier.border(2.dp, Color.Black, MaterialTheme.shapes.medium) else Modifier)
 * ```
 *
 * 三个文件合计 **16 处 `BuildConfig.IS_EINK`**（Category 4 / Feed 4 / Entry 8）。
 * 收敛到这里之后，各屏只负责自己的内容，不再关心 flavor 差异——
 * 这正是「E-Ink 与 standard 是两套设计」该有的落法：**差异集中在一处，而不是散落各处**。
 *
 * ## 两种设计在视觉上的唯一差异
 * - **standard**：M3 `Card` 默认 token（`surfaceContainerLow` 底 + 圆角 + 无边框）
 * - **E-Ink**：纯白底 + 2dp 纯黑边框 + 直角（与阅读页底栏、设置页按钮同一套线条语言）
 *
 * ⚠️ E-Ink 分支**必须**显式 `containerColor = Color.White`：M3 默认容器色在
 * `EInkLightColorScheme` 下虽是白色，但 `Card` 会叠加 `surfaceTint` 的 tonal 图层，
 * 在墨水屏上变成可见灰块（§8 坑 18 同源）。
 *
 * ## 参数
 * - `interactionModifier`：**插在 `padding(vertical = 8.dp)` 之前**，
 *   供 `EntryItem` 的滑动 `offset {}` + `pointerInput {}` 使用。
 *   调用方不要在这里传 `clickable`——统一走 [onClick]。
 * - `onClick == null` 时不加点击态（如编辑模式外的只读卡片）。
 * - `enabled = false` 时禁用点击（如 `FeedItem` 的编辑模式）。
 *
 * @param interactionModifier 交互相关的额外 modifier（滑动位移 / 手势检测）
 * @param onClick 点击回调；`null` = 不可点击
 * @param enabled 是否启用点击
 */
@Composable
fun MiniCard(
    modifier: Modifier = Modifier,
    interactionModifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val eink = BuildConfig.IS_EINK
    val shape = MaterialTheme.shapes.medium

    Card(
        shape = shape,
        colors = if (eink) {
            CardDefaults.cardColors(containerColor = Color.White)
        } else {
            CardDefaults.cardColors()
        },
        modifier = modifier
            .fillMaxWidth()
            .then(interactionModifier)
            .padding(vertical = 8.dp)
            .then(if (eink) Modifier.border(2.dp, Color.Black, shape) else Modifier)
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled) { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

/**
 * 列表卡片里的**主标题**颜色（三种卡片共用）。
 *
 * standard 用 `primary`（主题色强调标题），E-Ink 用纯黑——墨水屏上彩色
 * `primary` 会退化成灰阶，失去「标题 vs 正文」的层级（§8 坑 18 同源）。
 */
@Composable
fun miniCardTitleColor(): Color =
    if (BuildConfig.IS_EINK) Color.Black else MaterialTheme.colorScheme.primary

/**
 * 列表卡片里的**次要文字**颜色（订阅源名 / 时间 / 未读数）。
 *
 * standard 用 `onSurface` 降透明度（M3 推荐的次要文本表达）；E-Ink 用深灰
 * `DarkGray`——注意这是 E-Ink 分支里**唯一**允许的灰度，因为完全纯黑会让
 * 主次信息无法区分，而中灰（如 `Gray`）在墨水屏上对比度不足。
 */
@Composable
fun miniCardSecondaryColor(): Color =
    if (BuildConfig.IS_EINK) Color.DarkGray else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
