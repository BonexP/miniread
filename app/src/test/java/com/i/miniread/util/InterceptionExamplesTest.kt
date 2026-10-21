package com.i.miniread.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * InterceptionExamples 单元测试。
 *
 * 两条硬约束：
 * - **不得**从代码里读到任何硬编码的私有实例地址（示例必须来自传入的 baseUrl）；
 * - 生成结果填进设置页后不应出现「被丢弃」的条目。
 */
class InterceptionExamplesTest {

    // --- parseServer ---

    @Test
    fun parseServer_fullUrl() {
        assertEquals(
            InterceptionExamples.Server("https", "news.example.com"),
            InterceptionExamples.parseServer("https://news.example.com")
        )
    }

    @Test
    fun parseServer_stripsPathQueryAndPort() {
        assertEquals(
            "news.example.com",
            InterceptionExamples.parseServer("https://news.example.com:8443/v1/entries?x=1").host
        )
    }

    @Test
    fun parseServer_withoutScheme_defaultsToHttps() {
        assertEquals(
            InterceptionExamples.Server("https", "news.example.com"),
            InterceptionExamples.parseServer("news.example.com")
        )
    }

    @Test
    fun parseServer_keepsHttpScheme() {
        val server = InterceptionExamples.parseServer("http://192.168.1.5:8080")
        assertEquals("http", server.scheme)
        assertEquals("192.168.1.5", server.host)
    }

    @Test
    fun parseServer_stripsUserInfo() {
        assertEquals(
            "news.example.com",
            InterceptionExamples.parseServer("https://user:pass@news.example.com").host
        )
    }

    @Test
    fun parseServer_ipv6Literal() {
        assertEquals(
            "[::1]",
            InterceptionExamples.parseServer("http://[::1]:8080").host
        )
    }

    @Test
    fun parseServer_blank_returnsInvalid() {
        val server = InterceptionExamples.parseServer("   ")
        assertEquals("", server.host)
        assertFalse(server.isValid)
    }

    // --- build ---

    @Test
    fun build_usesCurrentServerAsDomain() {
        val cfg = InterceptionExamples.build("https://news.example.com/v1", "26", listOf(26))
        assertEquals("news.example.com", cfg.domains)
        assertTrue(cfg.enabled)
    }

    @Test
    fun build_refererRuleUsesSameHostAndScheme() {
        val cfg = InterceptionExamples.build("http://192.168.1.5:8080", "", emptyList())
        assertEquals("images.192.168.1.5=>http://192.168.1.5/", cfg.refererRules)
    }

    @Test
    fun build_noServer_producesEmptyDomainAndRules() {
        val cfg = InterceptionExamples.build("", "26", listOf(26))
        assertEquals("", cfg.domains)
        assertEquals("", cfg.refererRules)
    }

    @Test
    fun build_keepsCurrentFeedIdsUntouched() {
        val cfg = InterceptionExamples.build("https://news.example.com", "51,26", listOf(1, 2, 3))
        assertEquals("51,26", cfg.feedIds)
    }

    @Test
    fun build_fallsBackToFirstThreeAvailableFeeds() {
        val cfg = InterceptionExamples.build("https://news.example.com", "", listOf(7, 8, 9, 10))
        assertEquals("7,8,9", cfg.feedIds)
    }

    @Test
    fun build_deduplicatesFallbackFeeds() {
        val cfg = InterceptionExamples.build("https://news.example.com", "", listOf(7, 7, 8))
        assertEquals("7,8", cfg.feedIds)
    }

    @Test
    fun build_noFeedsAvailable_leavesFeedIdsEmpty() {
        val cfg = InterceptionExamples.build("https://news.example.com", "", emptyList())
        assertEquals("", cfg.feedIds)
    }

    @Test
    fun build_hasNoDroppedItems() {
        val cfg = InterceptionExamples.build("https://news.example.com", "", listOf(7, 8))
        val diag = InterceptionDiagnosticsAnalyzer.analyze(cfg, "https://news.example.com")
        assertEquals(0, diag.droppedTotal)
        assertTrue(diag.isActive)
    }
}
