package com.i.miniread.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * InterceptionClipboardFormat 纯 JVM 单元测试。
 *
 * 这 4 行文本是用户**唯一**的跨设备备份手段（DataStore 被备份规则排除），
 * 因此格式必须被测试锁死，不能随 UI 重构漂移。
 */
class InterceptionClipboardFormatTest {

    @Test
    fun buildExportText_producesFourLinesWithoutTrailingNewline() {
        val text = InterceptionClipboardFormat.buildExportText(
            enabled = true,
            domains = "example.com",
            feedIds = "101,102",
            refererRules = "images.example.com=>https://example.com/"
        )
        assertEquals(
            "enabled=true\ndomains=example.com\nfeedIds=101,102\nrefererRules=images.example.com=>https://example.com/",
            text
        )
        assertFalse(text.endsWith("\n"))
    }

    @Test
    fun buildExportText_emptyConfig_isStillFourLines() {
        assertEquals(
            "enabled=false\ndomains=\nfeedIds=\nrefererRules=",
            InterceptionClipboardFormat.buildExportText(false, "", "", "")
        )
    }

    @Test
    fun roundTrip_viaParseImported() {
        val text = InterceptionClipboardFormat.buildExportText(
            enabled = true,
            domains = "a.com,b.com",
            feedIds = "26,38",
            refererRules = "a.com=>https://a.com/"
        )
        val parsed = InterceptionClipboardFormat.parseImported(text)
        assertEquals(true, parsed?.enabled)
        assertEquals("a.com,b.com", parsed?.domains)
        assertEquals("26,38", parsed?.feedIds)
        assertEquals("a.com=>https://a.com/", parsed?.refererRules)
    }

    @Test
    fun parseImported_blankText_returnsNull() {
        assertNull(InterceptionClipboardFormat.parseImported(""))
        assertNull(InterceptionClipboardFormat.parseImported("   \n  "))
    }

    @Test
    fun parseImported_withoutKnownKeys_returnsNull() {
        assertNull(InterceptionClipboardFormat.parseImported("foo=bar\nbaz=qux"))
        // '=' 在首位也算无效行
        assertNull(InterceptionClipboardFormat.parseImported("=value"))
    }

    @Test
    fun parseImported_crlfAndSpacesAreTrimmed() {
        val parsed = InterceptionClipboardFormat.parseImported(
            "enabled = TRUE \r\ndomains = example.com \r\nfeedIds = 101 \r\nrefererRules = a.com=>https://a.com/ \r\n"
        )
        assertEquals(true, parsed?.enabled)
        assertEquals("example.com", parsed?.domains)
        assertEquals("101", parsed?.feedIds)
        assertEquals("a.com=>https://a.com/", parsed?.refererRules)
    }

    @Test
    fun parseImported_partialKeys_keepsLegacyOverwriteSemantics() {
        // 旧语义：命中任一 key 即成功，未出现的字段取默认空值（会被清空）。
        // 这里锁住它，避免有人「顺手改成只覆盖出现的字段」而改变导入行为。
        val parsed = InterceptionClipboardFormat.parseImported("domains=example.com")
        assertEquals(false, parsed?.enabled)
        assertEquals("example.com", parsed?.domains)
        assertEquals("", parsed?.feedIds)
        assertEquals("", parsed?.refererRules)
        assertTrue(parsed != null)
    }
}
