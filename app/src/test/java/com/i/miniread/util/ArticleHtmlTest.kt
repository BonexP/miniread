package com.i.miniread.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.TimeZone

/**
 * ArticleHtml 纯 JVM 单元测试。
 *
 * 覆盖阅读页 HTML 组装的三类易错点：
 * 1. 标题 / 订阅源名必须转义，正文必须原样透传；
 * 2. 元数据缺失时的省略规则（Miniflux 各字段都可能为空）；
 * 3. 时间格式化的时区换算与容错。
 */
class ArticleHtmlTest {

    /** 固定时区，避免用例受运行机器时区影响。 */
    private val shanghai: TimeZone = TimeZone.getTimeZone("Asia/Shanghai")

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

    // ===== formatPublishedAt =====

    @Test
    fun formatPublishedAt_utcToShanghai() {
        assertEquals("2026-10-17 12:34", ArticleHtml.formatPublishedAt("2026-10-17T04:34:56Z", shanghai))
    }

    @Test
    fun formatPublishedAt_offsetInput() {
        assertEquals(
            "2026-10-17 12:34",
            ArticleHtml.formatPublishedAt("2026-10-17T12:34:56+08:00", shanghai)
        )
    }

    @Test
    fun formatPublishedAt_fractionalSeconds() {
        // Go 的 RFC3339Nano 可能是 6~9 位小数秒，SimpleDateFormat 只认 3 位，必须先剥掉
        assertEquals(
            "2026-10-17 12:34",
            ArticleHtml.formatPublishedAt("2026-10-17T04:34:56.123456789Z", shanghai)
        )
    }

    @Test
    fun formatPublishedAt_invalid_returnsNull() {
        assertNull(ArticleHtml.formatPublishedAt("not-a-date", shanghai))
    }

    @Test
    fun formatPublishedAt_nullOrBlank_returnsNull() {
        assertNull(ArticleHtml.formatPublishedAt(null, shanghai))
        assertNull(ArticleHtml.formatPublishedAt("   ", shanghai))
    }

    // ===== buildContent =====

    @Test
    fun buildContent_fullMetadata_ordersTitleMetaContent() {
        val html = ArticleHtml.buildContent(
            title = "标题",
            feedTitle = "来源",
            publishedAt = "2026-10-17T04:34:56Z",
            content = "<p>正文</p>",
            timeZone = shanghai
        )
        assertEquals(
            "<h1>标题</h1><p class=\"miniread-meta\">来源 · 2026-10-17 12:34</p><p>正文</p>",
            html
        )
    }

    @Test
    fun buildContent_nullTitle_omitsH1() {
        val html = ArticleHtml.buildContent(
            title = null,
            feedTitle = "来源",
            publishedAt = null,
            content = "<p>正文</p>"
        )
        assertEquals("<p class=\"miniread-meta\">来源</p><p>正文</p>", html)
    }

    @Test
    fun buildContent_blankTitle_omitsH1() {
        val html = ArticleHtml.buildContent(
            title = "   ",
            feedTitle = null,
            publishedAt = null,
            content = "<p>正文</p>"
        )
        assertEquals("<p>正文</p>", html)
    }

    @Test
    fun buildContent_nullFeed_withTime_showsTimeOnly() {
        val html = ArticleHtml.buildContent(
            title = "标题",
            feedTitle = null,
            publishedAt = "2026-10-17T04:34:56Z",
            content = "",
            timeZone = shanghai
        )
        assertEquals("<h1>标题</h1><p class=\"miniread-meta\">2026-10-17 12:34</p>", html)
    }

    @Test
    fun buildContent_nullPublishedAt_showsFeedOnly() {
        val html = ArticleHtml.buildContent(
            title = "标题",
            feedTitle = "源",
            publishedAt = null,
            content = ""
        )
        assertEquals("<h1>标题</h1><p class=\"miniread-meta\">源</p>", html)
    }

    @Test
    fun buildContent_bothMetaAbsent_omitsMetaParagraph() {
        val html = ArticleHtml.buildContent(
            title = "标题",
            feedTitle = "  ",
            publishedAt = "garbage",
            content = "<p>x</p>"
        )
        assertEquals("<h1>标题</h1><p>x</p>", html)
    }

    @Test
    fun buildContent_escapesTitle() {
        val html = ArticleHtml.buildContent(
            title = "<script>alert(1)</script>",
            feedTitle = null,
            publishedAt = null,
            content = ""
        )
        assertEquals("<h1>&lt;script&gt;alert(1)&lt;/script&gt;</h1>", html)
    }

    @Test
    fun buildContent_escapesFeedTitle() {
        val html = ArticleHtml.buildContent(
            title = null,
            feedTitle = "A & B",
            publishedAt = null,
            content = ""
        )
        assertEquals("<p class=\"miniread-meta\">A &amp; B</p>", html)
    }

    @Test
    fun buildContent_keepsArticleHtmlRaw() {
        val content = "<p>a &amp; b</p><img src=\"x.png\">"
        val html = ArticleHtml.buildContent(
            title = null,
            feedTitle = null,
            publishedAt = null,
            content = content
        )
        assertEquals(content, html)
    }
}
