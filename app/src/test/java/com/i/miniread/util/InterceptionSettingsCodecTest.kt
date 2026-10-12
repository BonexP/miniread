package com.i.miniread.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * InterceptionSettingsCodec 纯 JVM 单元测试。
 *
 * 锁定文本格式与解析语义：
 * - 域名 / feedId / Referer 规则均支持 逗号 / 换行 / 分号 分隔
 * - 域名保序去重，feedId 忽略非数字项，Referer 规则保序且丢弃非法项
 * - format* 与 parse* 往返一致，整体 parse(format(s)) == s
 */
class InterceptionSettingsCodecTest {

    // --- parseDomains ---

    @Test
    fun parseDomains_commaSeparated() {
        assertEquals(
            listOf("a.com", "b.com"),
            InterceptionSettingsCodec.parseDomains("a.com,b.com")
        )
    }

    @Test
    fun parseDomains_newlineSeparated() {
        assertEquals(
            listOf("a.com", "b.com"),
            InterceptionSettingsCodec.parseDomains("a.com\nb.com")
        )
    }

    @Test
    fun parseDomains_semicolonSeparated() {
        assertEquals(
            listOf("a.com", "b.com"),
            InterceptionSettingsCodec.parseDomains("a.com;b.com")
        )
    }

    @Test
    fun parseDomains_mixedSeparators() {
        assertEquals(
            listOf("a.com", "b.com", "c.com", "d.com"),
            InterceptionSettingsCodec.parseDomains("a.com,b.com;c.com\nd.com")
        )
    }

    @Test
    fun parseDomains_trimsWhitespace() {
        assertEquals(
            listOf("a.com", "b.com"),
            InterceptionSettingsCodec.parseDomains(" a.com , b.com ")
        )
    }

    @Test
    fun parseDomains_emptyString_returnsEmpty() {
        assertTrue(InterceptionSettingsCodec.parseDomains("").isEmpty())
    }

    @Test
    fun parseDomains_null_returnsEmpty() {
        assertTrue(InterceptionSettingsCodec.parseDomains(null).isEmpty())
    }

    @Test
    fun parseDomains_dedup_keepsFirstOccurrenceOrder() {
        assertEquals(
            listOf("b.com", "a.com"),
            InterceptionSettingsCodec.parseDomains("b.com,a.com,b.com")
        )
    }

    @Test
    fun parseDomains_allBlank_returnsEmpty() {
        assertTrue(InterceptionSettingsCodec.parseDomains(" , ; ").isEmpty())
        assertTrue(InterceptionSettingsCodec.parseDomains("   ").isEmpty())
    }

    // --- parseFeedIds ---

    @Test
    fun parseFeedIds_normal() {
        assertEquals(
            setOf(26, 38, 52, 51),
            InterceptionSettingsCodec.parseFeedIds("26,38,52,51")
        )
    }

    @Test
    fun parseFeedIds_ignoresNonNumericItems() {
        assertEquals(
            setOf(26, 38),
            InterceptionSettingsCodec.parseFeedIds("26,abc,38")
        )
    }

    @Test
    fun parseFeedIds_emptyString_returnsEmptySet() {
        assertTrue(InterceptionSettingsCodec.parseFeedIds("").isEmpty())
    }

    @Test
    fun parseFeedIds_null_returnsEmptySet() {
        assertTrue(InterceptionSettingsCodec.parseFeedIds(null).isEmpty())
    }

    // --- parseRefererRules ---

    @Test
    fun parseRefererRules_single() {
        assertEquals(
            listOf("a.com" to "https://a/"),
            InterceptionSettingsCodec.parseRefererRules("a.com=>https://a/")
        )
    }

    @Test
    fun parseRefererRules_multipleSemicolon_keepsOrder() {
        assertEquals(
            listOf("a.com" to "https://a/", "b.com" to "https://b/"),
            InterceptionSettingsCodec.parseRefererRules("a.com=>https://a/;b.com=>https://b/")
        )
    }

    @Test
    fun parseRefererRules_newlineSeparated() {
        assertEquals(
            listOf("a.com" to "https://a/", "b.com" to "https://b/"),
            InterceptionSettingsCodec.parseRefererRules("a.com=>https://a/\nb.com=>https://b/")
        )
    }

    @Test
    fun parseRefererRules_emptyPattern_isDropped() {
        assertTrue(
            InterceptionSettingsCodec.parseRefererRules("=>x").isEmpty()
        )
    }

    @Test
    fun parseRefererRules_emptyReferer_isDropped() {
        assertTrue(
            InterceptionSettingsCodec.parseRefererRules("a=>").isEmpty()
        )
    }

    @Test
    fun parseRefererRules_noArrow_isDropped() {
        assertTrue(
            InterceptionSettingsCodec.parseRefererRules("noarrow").isEmpty()
        )
    }

    @Test
    fun parseRefererRules_null_returnsEmpty() {
        assertTrue(InterceptionSettingsCodec.parseRefererRules(null).isEmpty())
    }

    @Test
    fun parseRefererRules_emptyString_returnsEmpty() {
        assertTrue(InterceptionSettingsCodec.parseRefererRules("").isEmpty())
    }

    // --- format* ---

    @Test
    fun formatDomains_joinsWithComma() {
        assertEquals(
            "a.com,b.com",
            InterceptionSettingsCodec.formatDomains(listOf("a.com", "b.com"))
        )
    }

    @Test
    fun formatFeedIds_joinsWithComma() {
        assertEquals(
            "26,38",
            InterceptionSettingsCodec.formatFeedIds(setOf(26, 38))
        )
    }

    @Test
    fun formatRefererRules_joinsWithSemicolonAndArrow() {
        assertEquals(
            "a.com=>https://a/;b.com=>https://b/",
            InterceptionSettingsCodec.formatRefererRules(
                listOf("a.com" to "https://a/", "b.com" to "https://b/")
            )
        )
    }

    // --- 往返一致性 ---

    @Test
    fun roundTrip_domains() {
        val domains = listOf("a.com", "b.com", "c.com")
        assertEquals(
            domains,
            InterceptionSettingsCodec.parseDomains(InterceptionSettingsCodec.formatDomains(domains))
        )
    }

    @Test
    fun roundTrip_feedIds() {
        val ids = setOf(26, 38, 52, 51)
        assertEquals(
            ids,
            InterceptionSettingsCodec.parseFeedIds(InterceptionSettingsCodec.formatFeedIds(ids))
        )
    }

    @Test
    fun roundTrip_refererRules() {
        val rules = listOf("a.com" to "https://a/", "b.com" to "https://b/")
        assertEquals(
            rules,
            InterceptionSettingsCodec.parseRefererRules(
                InterceptionSettingsCodec.formatRefererRules(rules)
            )
        )
    }

    // --- 整体往返 ---

    @Test
    fun roundTrip_settings_viaRawConfig() {
        val settings = InterceptionSettings(
            enabled = true,
            targetDomains = listOf("a.com", "b.com"),
            feedIds = setOf(26, 38, 52, 51),
            refererRules = listOf("a.com" to "https://a/", "b.com" to "https://b/")
        )
        assertEquals(
            settings,
            InterceptionSettingsCodec.parse(InterceptionSettingsCodec.format(settings))
        )
    }

    @Test
    fun parse_emptyRawConfig_equalsEmptySettings() {
        assertEquals(
            InterceptionSettings.EMPTY,
            InterceptionSettingsCodec.parse(InterceptionRawConfig())
        )
    }

    @Test
    fun format_emptySettings_producesEmptyStrings() {
        val raw = InterceptionSettingsCodec.format(InterceptionSettings.EMPTY)
        assertEquals(false, raw.enabled)
        assertEquals("", raw.domains)
        assertEquals("", raw.feedIds)
        assertEquals("", raw.refererRules)
    }
}
