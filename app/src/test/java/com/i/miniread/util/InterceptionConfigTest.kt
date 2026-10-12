package com.i.miniread.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * InterceptionConfig.matchesBaseUrl() 纯 JVM 单元测试（仅读测试，不修改被测文件）。
 *
 * 被测实现等价于 `baseUrl.contains(TARGET_DOMAIN, ignoreCase = true)`，
 * 目标域名为 "pi.lifeo3.icu"。
 */
class InterceptionConfigTest {

    @Test
    fun exactMatch_returnsTrue() {
        assertTrue(InterceptionConfig.matchesBaseUrl("https://pi.lifeo3.icu"))
    }

    @Test
    fun mixedCase_returnsTrue() {
        assertTrue(InterceptionConfig.matchesBaseUrl("https://PI.Lifeo3.ICU"))
    }

    @Test
    fun trailingSlash_returnsTrue() {
        assertTrue(InterceptionConfig.matchesBaseUrl("https://pi.lifeo3.icu/"))
    }

    @Test
    fun subdomain_returnsTrue() {
        assertTrue(InterceptionConfig.matchesBaseUrl("https://sub.pi.lifeo3.icu"))
    }

    @Test
    fun withPath_returnsTrue() {
        assertTrue(InterceptionConfig.matchesBaseUrl("https://pi.lifeo3.icu/v1/entries"))
    }

    @Test
    fun customTarget_returnsTrue() {
        assertTrue(InterceptionConfig.matchesBaseUrl("https://example.org", target = "example.org"))
    }

    @Test
    fun embeddedTarget_returnsTrue_becauseContains() {
        // contains 语义：即使目标串嵌入在更长字符串中也会命中
        assertTrue(InterceptionConfig.matchesBaseUrl("https://pi.lifeo3.icu.example.com"))
    }

    @Test
    fun otherDomain_returnsFalse() {
        assertFalse(InterceptionConfig.matchesBaseUrl("https://example.com"))
    }

    @Test
    fun emptyString_returnsFalse() {
        assertFalse(InterceptionConfig.matchesBaseUrl(""))
    }

    @Test
    fun illegalUrl_returnsFalse() {
        assertFalse(InterceptionConfig.matchesBaseUrl("not a url"))
        assertFalse(InterceptionConfig.matchesBaseUrl("http://[invalid"))
    }

    @Test
    fun defaultTarget_isTargetDomain() {
        assertTrue(InterceptionConfig.matchesBaseUrl(InterceptionConfig.TARGET_DOMAIN))
    }
}
