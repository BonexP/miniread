package com.i.miniread.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * InterceptionConfig 纯 JVM 单元测试。
 *
 * v1.0.0 起默认值全部中性化（关闭 / 空），定向能力改为用户可配置，
 * 因此 [InterceptionConfig.matchesBaseUrl] 与 [InterceptionConfig.refererFor]
 * 都需要显式传入 target / rules。本测试锁定以下语义：
 * - contains 匹配（大小写不敏感）与 target 空白保护
 * - matchesAnyBaseUrl 的任一命中语义
 * - refererFor 的顺序敏感与未命中返回空串
 * - 默认值中性化
 */
class InterceptionConfigTest {

    private val target = "pi.lifeo3.icu"

    // --- matchesBaseUrl: 命中（contains 语义） ---

    @Test
    fun exactMatch_returnsTrue() {
        assertTrue(InterceptionConfig.matchesBaseUrl("https://pi.lifeo3.icu", target))
    }

    @Test
    fun mixedCase_returnsTrue() {
        assertTrue(InterceptionConfig.matchesBaseUrl("https://PI.Lifeo3.ICU", target))
    }

    @Test
    fun trailingSlash_returnsTrue() {
        assertTrue(InterceptionConfig.matchesBaseUrl("https://pi.lifeo3.icu/", target))
    }

    @Test
    fun subdomain_returnsTrue() {
        assertTrue(InterceptionConfig.matchesBaseUrl("https://sub.pi.lifeo3.icu", target))
    }

    @Test
    fun withPath_returnsTrue() {
        assertTrue(InterceptionConfig.matchesBaseUrl("https://pi.lifeo3.icu/v1/entries", target))
    }

    @Test
    fun embeddedTarget_returnsTrue_becauseContains() {
        // contains 语义：即使目标串嵌入在更长字符串中也会命中
        assertTrue(InterceptionConfig.matchesBaseUrl("https://pi.lifeo3.icu.example.com", target))
    }

    // --- matchesBaseUrl: 不命中 ---

    @Test
    fun otherDomain_returnsFalse() {
        assertFalse(InterceptionConfig.matchesBaseUrl("https://example.com", target))
    }

    @Test
    fun emptyBaseUrl_returnsFalse() {
        assertFalse(InterceptionConfig.matchesBaseUrl("", target))
    }

    // --- matchesBaseUrl: 空白保护（新行为） ---

    @Test
    fun emptyTarget_returnsFalse() {
        assertFalse(InterceptionConfig.matchesBaseUrl("https://anything", ""))
    }

    @Test
    fun blankTarget_returnsFalse() {
        assertFalse(InterceptionConfig.matchesBaseUrl("https://anything", "   "))
    }

    // --- matchesAnyBaseUrl ---

    @Test
    fun matchesAnyBaseUrl_hitsOneInList_returnsTrue() {
        assertTrue(
            InterceptionConfig.matchesAnyBaseUrl(
                "https://pi.lifeo3.icu",
                listOf("other.com", target)
            )
        )
    }

    @Test
    fun matchesAnyBaseUrl_emptyList_returnsFalse() {
        assertFalse(InterceptionConfig.matchesAnyBaseUrl("https://pi.lifeo3.icu", emptyList()))
    }

    @Test
    fun matchesAnyBaseUrl_blankEntriesDoNotMatch_returnsFalse() {
        assertFalse(
            InterceptionConfig.matchesAnyBaseUrl("https://example.com", listOf("", "   "))
        )
    }

    // --- refererFor ---

    @Test
    fun refererFor_orderSensitive_returnsFirstMatch() {
        // 同一 host 同时命中两条规则，应返回先出现的那条
        val rules = listOf(
            "lifeo3.icu" to "https://first/",
            "pi.lifeo3.icu" to "https://second/"
        )
        assertEquals(
            "https://first/",
            InterceptionConfig.refererFor("pi.lifeo3.icu", rules)
        )
    }

    @Test
    fun refererFor_noMatch_returnsEmpty() {
        val rules = listOf("sspai.com" to "https://sspai.com/")
        assertEquals("", InterceptionConfig.refererFor("pi.lifeo3.icu", rules))
    }

    @Test
    fun refererFor_nullHost_returnsEmpty() {
        val rules = listOf("pi.lifeo3.icu" to "https://pi.lifeo3.icu/")
        assertEquals("", InterceptionConfig.refererFor(null, rules))
    }

    @Test
    fun refererFor_emptyRules_returnsEmpty() {
        assertEquals("", InterceptionConfig.refererFor("pi.lifeo3.icu", emptyList()))
    }

    @Test
    fun refererFor_blankPattern_isSkipped() {
        val rules = listOf(
            "   " to "https://ignored/",
            "pi.lifeo3.icu" to "https://matched/"
        )
        assertEquals(
            "https://matched/",
            InterceptionConfig.refererFor("pi.lifeo3.icu", rules)
        )
    }

    // --- 默认值中性化 ---

    @Test
    fun defaultInterceptEnabled_isFalse() {
        assertFalse(InterceptionConfig.DEFAULT_INTERCEPT_ENABLED)
    }

    @Test
    fun defaultTargetDomains_isEmpty() {
        assertTrue(InterceptionConfig.DEFAULT_TARGET_DOMAINS.isEmpty())
    }

    @Test
    fun defaultAdvancedFeedIds_isEmpty() {
        assertTrue(InterceptionConfig.DEFAULT_ADVANCED_FEED_IDS.isEmpty())
    }

    @Test
    fun defaultRefererRules_isEmpty() {
        assertTrue(InterceptionConfig.DEFAULT_REFERER_RULES.isEmpty())
    }
}
