package com.i.miniread.eink

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.ui.graphics.Color

/**
 * E-Ink 优化配置类
 * 针对电子墨水屏的特性进行优化
 */
object EInkConfig {

    /**
     * 是否禁用动画
     * E-Ink 屏幕刷新较慢，建议禁用动画以提升用户体验
     */
    const val DISABLE_ANIMATIONS = true

    /**
     * 刷新模式
     */
    enum class RefreshMode {
        NORMAL,     // 普通刷新
        FAST,       // 快速刷新（可能有残影）
        QUALITY     // 质量刷新（完全刷新，无残影）
    }

    /**
     * 默认刷新模式
     */
    var defaultRefreshMode = RefreshMode.NORMAL

    /**
     * 获取动画规格（E-Ink 版本使用无动画）
     */
    fun <T> getAnimationSpec(): AnimationSpec<T> = snap()

    /**
     * E-Ink 优化的颜色方案
     */
    object Colors {
        val Background = Color.White
        val Surface = Color(0xFFF5F5F5)
        val OnSurface = Color.Black
        val Primary = Color.Black
        val Secondary = Color(0xFF757575)
        val Divider = Color(0xFFBDBDBD)
    }

    /**
     * 文本对比度增强
     */
    const val TEXT_CONTRAST_ENHANCEMENT = true

    /**
     * 触发全局刷新的页面数
     * 每浏览指定数量的页面后，触发一次全局刷新以清除残影
     */
    const val FULL_REFRESH_INTERVAL = 5
}

