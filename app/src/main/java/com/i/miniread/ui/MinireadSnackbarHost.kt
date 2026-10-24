package com.i.miniread.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 * - E-Ink 恒用浅色方案，提示条反而变成**深色块**，墨水屏上刷新重、残影明显。
 *
 * 这里按主题取正常 surface 色，E-Ink 另走「白底 + 黑字 + 直角」，与
 * `FeedWhitelistPicker` / 帮助页的示例块保持同一套视觉语言。
 *
 * ⚠️ E-Ink **不要给提示条加外框**（issue #15，2026-10-24 用户实测）：`Modifier.border(...)` 套在
 * `Snackbar(snackbarData, modifier)` 上会被它内部的 `padding(12.dp)` 推到容器之外，观感是一圈
 * 悬浮在整条外面的黑框，而不是描边。要去掉边框就真的不写 `border`，不要靠调粗细或圆角去「修」
 * ——ink 屏上那圈框比任何边界感都碍眼。
 */
@Composable
fun MinireadSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        if (BuildConfig.IS_EINK) {
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(0.dp),
                containerColor = Color.White,
                contentColor = Color.Black,
                actionColor = Color.Black,
                dismissActionContentColor = Color.Black
            )
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
