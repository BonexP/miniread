package com.i.miniread.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ReaderThemeResolver 纯 JVM 单元测试。
 *
 * 覆盖「flavor × 系统深浅色」的完整真值表，锁死 issue #12 的核心约束：
 * E-Ink 版正文必须与 Compose 界面一致地恒用浅色。
 */
class ReaderThemeResolverTest {

    @Test
    fun einkWithSystemDark_usesLightTheme() {
        assertFalse(ReaderThemeResolver.isDarkMode(isEink = true, systemNightMode = true))
    }

    @Test
    fun einkWithSystemLight_usesLightTheme() {
        assertFalse(ReaderThemeResolver.isDarkMode(isEink = true, systemNightMode = false))
    }

    @Test
    fun standardWithSystemDark_usesDarkTheme() {
        assertTrue(ReaderThemeResolver.isDarkMode(isEink = false, systemNightMode = true))
    }

    @Test
    fun standardWithSystemLight_usesLightTheme() {
        assertFalse(ReaderThemeResolver.isDarkMode(isEink = false, systemNightMode = false))
    }
}
