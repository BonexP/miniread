package com.i.miniread.eink

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * E-Ink 辅助工具类（标准版）
 * 标准版本中的空实现，保持 API 兼容性
 */
object EInkUtils {

    /**
     * 标准版不是 E-Ink 设备
     */
    fun isEInkDevice(): Boolean = false

    /**
     * 标准版不需要优化文本渲染
     */
    fun optimizeTextRendering() {
        // 标准版无需特殊处理
    }

    /**
     * 标准版不需要刷新屏幕
     */
    fun refreshScreen(fullRefresh: Boolean = false) {
        // 标准版无需特殊处理
    }
}

/**
 * E-Ink 优化的 Composable 包装器（标准版）
 */
@Composable
fun EInkOptimized(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 标准版直接渲染内容
    content()
}

