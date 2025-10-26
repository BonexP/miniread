package com.i.miniread.eink

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * E-Ink 辅助工具类
 */
object EInkUtils {

    /**
     * 判断当前是否运行在 E-Ink 设备上
     * 可以通过 BuildConfig 或设备特征判断
     */
    fun isEInkDevice(): Boolean {
        // 这里可以添加实际的 E-Ink 设备检测逻辑
        // 例如检测设备型号、制造商等
        return true // E-Ink flavor 始终返回 true
    }

    /**
     * 优化文本渲染
     */
    fun optimizeTextRendering() {
        // 实现文本渲染优化逻辑
        // 例如：增加文本对比度、调整字体粗细等
    }

    /**
     * 执行屏幕刷新
     * @param fullRefresh 是否执行全屏刷新
     */
    fun refreshScreen(fullRefresh: Boolean = false) {
        // 在实际项目中，这里应该调用 E-Ink 设备的 SDK
        // 例如：ONYX SDK, Boyue SDK 等
        if (fullRefresh) {
            // 执行全屏刷新，清除残影
        } else {
            // 执行局部刷新
        }
    }
}

/**
 * E-Ink 优化的 Composable 包装器
 * 自动应用 E-Ink 优化配置
 */
@Composable
fun EInkOptimized(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 应用 E-Ink 优化
    content()
}

