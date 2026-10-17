package com.i.miniread.util

/**
 * 阅读页主题判定（纯函数，无 Android 依赖，可直接跑纯 JVM 单测）。
 *
 * 背景：Compose 侧 E-Ink flavor 早已强制浅色（`ui/theme/Theme.kt` 的 `EInkLightColorScheme`），
 * 但 WebView 正文此前只看系统 `uiMode`，于是系统深色下会出现「白色工具栏 + 深色正文」的割裂；
 * 墨水屏上深底还会加重残影。
 *
 * 判据只有一条：**E-Ink 永远浅色，standard 跟随系统**。两处调用点
 * （`loadHtmlContentAsync` 的初始注入、`ArticleWebView` 的运行期就地刷新）
 * 必须走同一个函数，否则一处改了另一处会覆盖回来。
 *
 * ⚠️ 入参故意用 `Boolean` 而不是 `Configuration` / `BuildConfig`：
 * 后两者属于 Android 与生成类，单元测试禁止依赖 `android.*`（见 AGENTS.md）。
 */
object ReaderThemeResolver {

    /**
     * @param isEink 当前 flavor 是否为 E-Ink（调用方传 `BuildConfig.IS_EINK`）
     * @param systemNightMode 系统是否处于深色模式（由 `uiMode and UI_MODE_NIGHT_MASK` 判定）
     * @return 正文是否应使用深色主题
     */
    fun isDarkMode(isEink: Boolean, systemNightMode: Boolean): Boolean =
        !isEink && systemNightMode
}
