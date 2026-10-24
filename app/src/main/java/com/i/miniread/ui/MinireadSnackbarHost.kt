package com.i.miniread.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.i.miniread.BuildConfig

/**
 * 全应用统一的 Snackbar 宿主（「已标记为已读」这类提示条）。
 *
 * 为什么不能直接用 `SnackbarHost(hostState)`：
 * M3 1.2.1 的默认配色是**反色** —— `SnackbarDefaults.color` 取自
 * `SnackbarTokens.ContainerColor`，映射到 `ColorSchemeKeyTokens.InverseSurface`；文字用
 * `InverseOnSurface`。结果是：
 * - 深色主题下提示条变成**浅色**（看着像没跟随系统主题）；
 * - E-Ink 恒用浅色方案，提示条反而变成**深色块**（`#313033`），墨水屏上刷新重、残影明显。
 *
 * standard 分支：沿用 M3 `Snackbar`，只把配色换成 `surfaceVariant` / `onSurfaceVariant`。
 *
 * E-Ink 分支：**自绘容器**，白底 + 黑字 + 直角 + 2dp 黑框 + 零阴影（issue #15）。
 * 为什么不能继续用 `Snackbar(snackbarData, modifier)`：
 * 1. 它内部先 `modifier.padding(12.dp)` 再交给 `Surface`，外面套的 `Modifier.border(...)`
 *    会被推到容器之外，看起来是一圈悬浮黑框而不是描边；
 * 2. 它的 `shadowElevation` 写死为 `SnackbarTokens.ContainerElevation`（6dp），**没有公开参数
 *    可覆盖**，在纯白页面上必然留一圈灰晕 —— 这与「只用纯黑纯白」的墨水屏语言冲突。
 * 两条都无法通过参数绕过，所以这里只借 `SnackbarData` 的 message / actionLabel，容器自绘。
 *
 * ⚠️ 已知取舍：自绘版**不支持** `withDismissAction`（当前全仓 `showSnackbar(...)` 都是单参调用）；
 * `SnackbarHost` 自带的淡入淡出仍在（墨水屏上会有一瞬过渡），那是宿主内部动画，未改。
 */
@Composable
fun MinireadSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        if (BuildConfig.IS_EINK) {
            EInkSnackbar(data = data)
        } else {
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                actionColor = MaterialTheme.colorScheme.primary,
                dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * E-Ink 提示条：一条 2dp 黑框 + 纯白底 + 纯黑字，直角，无阴影。
 *
 * 尺寸与配色全部取整数、只用黑/白两色，和阅读页 48dp 底栏（顶部 2dp 黑线）、
 * 列表卡片（2dp 黑框）保持同一套线条语言。
 */
@Composable
private fun EInkSnackbar(data: SnackbarData) {
    val actionLabel = data.visuals.actionLabel
    Surface(
        // 左右 12dp 外边距：黑框不贴屏幕边缘；上下 8dp 避免紧贴底栏或正文
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RectangleShape,
        color = Color.White,
        contentColor = Color.Black,
        // 墨水屏上任何 tonal / shadow 都会变成可见灰块或灰晕，必须归零
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(2.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = if (actionLabel == null) 16.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            )
            if (actionLabel != null) {
                TextButton(
                    onClick = { data.performAction() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
