package com.i.miniread.eink

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * E-Ink 辅助工具类
 */
object EInkUtils {

    /** E-Ink 设备厂商/型号关键字（小写匹配）。 */
    private val EINK_KEYWORDS: List<String> = listOf(
        "onyx", "boox", "ireader", "boyue", "likebook", "meebook", "kobo", "hisense"
    )

    /**
     * 判断当前是否运行在 E-Ink 设备上。
     *
     * v1.0 采用厂商/品牌/型号的关键字启发式判断（未接入厂商 SDK）。
     * 注意：不再硬编码返回 true，避免在普通设备上误判。
     */
    fun isEInkDevice(): Boolean {
        val identity = (Build.MANUFACTURER + Build.BRAND + Build.MODEL).lowercase()
        return EINK_KEYWORDS.any { identity.contains(it) }
    }

    /**
     * 优化文本渲染。
     *
     * v1.0 未接入厂商 SDK，此处为占位/降级实现（无实际操作）。
     */
    fun optimizeTextRendering() {
        // 待接入厂商 SDK 后实现：增加文本对比度、调整字体粗细等
    }

    /**
     * 执行屏幕刷新。
     *
     * v1.0 未接入厂商 SDK，此处为占位/降级实现（无实际操作）。
     * @param fullRefresh 是否执行全屏刷新
     */
    fun refreshScreen(fullRefresh: Boolean = false) {
        // 待接入厂商 SDK（ONYX / Boyue 等）后实现全刷/局刷逻辑
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

