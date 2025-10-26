package com.i.miniread.eink

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color

/**
 * E-Ink 配置类（标准版）
 * 标准版本中的空实现，保持 API 兼容性
 */
object EInkConfig {

    /**
     * 标准版不禁用动画
     */
    const val DISABLE_ANIMATIONS = false

    /**
     * 刷新模式（标准版不使用）
     */
    enum class RefreshMode {
        NORMAL,
        FAST,
        QUALITY
    }

    var defaultRefreshMode = RefreshMode.NORMAL

    /**
     * 获取动画规格（标准版使用正常动画）
     */
    fun <T> getAnimationSpec(): AnimationSpec<T> = spring()

    /**
     * 标准版颜色方案
     */
    object Colors {
        val Background = Color.White
        val Surface = Color.White
        val OnSurface = Color.Black
        val Primary = Color(0xFF6200EE)
        val Secondary = Color(0xFF03DAC6)
        val Divider = Color(0xFFE0E0E0)
    }

    const val TEXT_CONTRAST_ENHANCEMENT = false
    const val FULL_REFRESH_INTERVAL = 0
}

