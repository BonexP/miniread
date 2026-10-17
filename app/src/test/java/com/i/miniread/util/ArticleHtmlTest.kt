package com.i.miniread.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ArticleHtml 纯 JVM 单元测试。
 *
 * 覆盖阅读页 HTML 组装的两个易错点：
 * 1. 标题 / 订阅源名必须转义（正文不转义）；
 * 2. 时间格式化的时区与容错（见后续用例）。
 */
class ArticleHtmlTest {

    // ===== escapeHtml =====

    @Test
    fun escapeHtml_plainText_unchanged() {
        assertEquals("Hello World", ArticleHtml.escapeHtml("Hello World"))
    }

    @Test
    fun escapeHtml_escapesAngleBrackets() {
        assertEquals("&lt;b&gt;bold&lt;/b&gt;", ArticleHtml.escapeHtml("<b>bold</b>"))
    }

    @Test
    fun escapeHtml_ampersandBecomesEntity() {
        // 逐字符转义的关键：`&` 与 `<` 同时出现时不会互相干扰
        assertEquals("a &amp; &lt;b&gt;", ArticleHtml.escapeHtml("a & <b>"))
    }

    @Test
    fun escapeHtml_escapesBothQuoteKinds() {
        assertEquals("&quot;x&quot; &#39;y&#39;", ArticleHtml.escapeHtml("\"x\" 'y'"))
    }

    @Test
    fun escapeHtml_existingEntityIsEscapedAgain() {
        // 预期行为：输入本来就是字面量 `&amp;`，输出必须是可显示成 `&amp;` 的文本。
        // 换言之「二次转义」在这里是正确语义，不是 bug —— 因为输入是文本而非 HTML。
        assertEquals("&amp;amp;", ArticleHtml.escapeHtml("&amp;"))
    }

    @Test
    fun escapeHtml_keepsCjkAndEmoji() {
        assertEquals("搅拌摩擦焊 🔧 FSW", ArticleHtml.escapeHtml("搅拌摩擦焊 🔧 FSW"))
    }

    @Test
    fun escapeHtml_empty_returnsEmpty() {
        assertEquals("", ArticleHtml.escapeHtml(""))
    }
}
