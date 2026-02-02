package com.i.miniread.ui.adaptive

import android.util.Log
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * 自适应布局工具类
 *
 * 用于检测设备类型和屏幕尺寸，提供平板/手机的适配逻辑
 */
@Stable
object AdaptiveLayoutHelper {

    private const val TAG = "AdaptiveLayoutHelper"

    /**
     * 判断当前设备是否为平板
     * 标准：最小屏幕宽度 >= 600dp
     */
    @Composable
    fun isTablet(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp >= 600
    }

    /**
     * 判断当前设备是否为横屏模式
     */
    @Composable
    fun isLandscape(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp > configuration.screenHeightDp
    }

    /**
     * 判断是否应该使用侧边导航栏（NavigationRail）
     * 条件：平板设备 + 横屏模式
     */
    @Composable
    fun shouldUseNavigationRail(): Boolean {
        return isTablet() && isLandscape()
    }

    /**
     * 判断是否应该使用双栏布局
     * 条件：屏幕宽度 >= 840dp（超大屏幕）
     */
    @Composable
    fun shouldUseTwoPane(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp >= 840
    }

    /**
     * 根据 WindowWidthSizeClass 判断布局类型
     */
    fun shouldUseNavigationRail(windowSizeClass: WindowWidthSizeClass): Boolean {
        val shouldUse = windowSizeClass >= WindowWidthSizeClass.Medium
        Log.d(TAG, "Checking adaptive layout: WidthClass=$windowSizeClass, UseRail=$shouldUse")
        return shouldUse
    }

    /**
     * 获取内容最大宽度（用于阅读器）
     * - 手机：填充全屏
     * - 平板：限制最大宽度为 800dp，居中显示
     */
    @Composable
    fun getContentMaxWidth(): Int {
        val configuration = LocalConfiguration.current
        return if (configuration.screenWidthDp >= 600) {
            800 // 平板模式：限制最大宽度
        } else {
            configuration.screenWidthDp // 手机模式：全屏
        }
    }
}

/**
 * 导航类型枚举
 */
enum class NavigationType {
    BOTTOM_NAVIGATION,  // 底部导航栏（手机）
    NAVIGATION_RAIL,    // 侧边导航栏（平板横屏）
    PERMANENT_DRAWER    // 永久抽屉（超大屏）
}

/**
 * 内容类型枚举
 */
enum class ContentType {
    SINGLE_PANE,  // 单栏布局
    DUAL_PANE     // 双栏布局
}
