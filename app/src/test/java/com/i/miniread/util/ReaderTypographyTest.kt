package com.i.miniread.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTypographyTest {

    // ===== textZoom 换算 =====

    @Test
    fun `textZoom default matches legacy 125`() {
        assertEquals(125, ReaderTypography.textZoomFor(ReaderTypography.DEFAULT_FONT_SCALE))
    }

    @Test
    fun `textZoom scales linearly and rounds`() {
        assertEquals(106, ReaderTypography.textZoomFor(85))
        assertEquals(150, ReaderTypography.textZoomFor(120))
        assertEquals(188, ReaderTypography.textZoomFor(150))
    }

    @Test
    fun `textZoom clamps out of range scale`() {
        assertEquals(
            ReaderTypography.textZoomFor(ReaderTypography.FONT_SCALE_MIN),
            ReaderTypography.textZoomFor(-100)
        )
        assertEquals(
            ReaderTypography.textZoomFor(ReaderTypography.FONT_SCALE_MAX),
            ReaderTypography.textZoomFor(999)
        )
    }

    // ===== clamp =====

    @Test
    fun `clamp keeps value inside range`() {
        assertEquals(85, ReaderTypography.clamp(10, 85, 150))
        assertEquals(120, ReaderTypography.clamp(120, 85, 150))
        assertEquals(150, ReaderTypography.clamp(400, 85, 150))
    }

    @Test
    fun `clampFloat keeps value inside range`() {
        assertEquals(1.4f, ReaderTypography.clampFloat(0f, 1.4f, 2.0f), 0f)
        assertEquals(2.0f, ReaderTypography.clampFloat(9f, 1.4f, 2.0f), 0f)
        assertEquals(1.65f, ReaderTypography.clampFloat(1.65f, 1.4f, 2.0f), 0f)
    }

    @Test
    fun `num renders compact CSS numbers`() {
        assertEquals("1.6", ReaderTypography.num(1.6f))
        assertEquals("2", ReaderTypography.num(2.0f))
        assertEquals("1.75", ReaderTypography.num(1.75f))
        assertEquals("1.67", ReaderTypography.num(1.6666667f))
    }

    // ===== buildCss：只写尺寸、不写颜色 =====

    @Test
    fun `default css has no color declarations`() {
        val css = ReaderTypography.DEFAULT.buildCss()
        // 只检查「声明」（带冒号），注释里 "no colors" 是文字说明，不算声明
        assertFalse(css.contains("color:"))
        assertFalse(css.contains("background:"))
        assertTrue(css.contains("/* miniread reader typography"))
    }

    @Test
    fun `default css has no body or paragraph overrides`() {
        val css = ReaderTypography.DEFAULT.buildCss()
        assertFalse(css.contains("line-height:"))
        assertFalse(css.contains("max-width:"))
        assertFalse(css.contains("margin-bottom:"))
        assertFalse(css.contains("font-family:"))
        assertFalse(css.contains("text-align:"))
    }

    @Test
    fun `line height is emitted on body`() {
        val css = ReaderTypography(lineHeight = 1.8f).buildCss()
        assertTrue(css.contains("body{line-height:1.8"))
        assertFalse(css.contains("color:"))
    }

    @Test
    fun `width step emits max-width and centering but never fixed width`() {
        val css = ReaderTypography(widthStep = 2).buildCss()
        assertTrue(css.contains("max-width:32em"))
        assertTrue(css.contains("margin:0 auto"))
        // 用正则确认没有「独立」的 width: 声明（max-width 里含子串 width: 属正常）
        assertFalse(Regex("""(?<!max-)width:\d""").containsMatchIn(css))
    }

    @Test
    fun `width step zero emits nothing`() {
        assertFalse(ReaderTypography(widthStep = 0).buildCss().contains("max-width"))
    }

    @Test
    fun `paragraph spacing is emitted in em`() {
        val css = ReaderTypography(paragraphSpacing = 1.2f).buildCss()
        assertTrue(css.contains("p,li,blockquote{margin-bottom:1.2em"))
    }

    @Test
    fun `serif family overrides body only`() {
        val css = ReaderTypography(fontFamily = ReaderTypography.FONT_FAMILY_SERIF).buildCss()
        assertTrue(css.contains("font-family:Georgia"))
        assertFalse(css.contains("color:"))
    }

    @Test
    fun `justify applies to body and blocks`() {
        val css = ReaderTypography(textAlign = ReaderTypography.ALIGN_JUSTIFY).buildCss()
        assertTrue(css.contains("body{text-align:justify"))
        assertTrue(css.contains("p,li,blockquote{text-align:justify"))
    }

    @Test
    fun `all options combined produce single compact css without colors`() {
        val css = ReaderTypography(
            fontScale = 120,
            lineHeight = 2.0f,
            widthStep = 1,
            paragraphSpacing = 0.8f,
            fontFamily = 1,
            textAlign = 1
        ).buildCss()
        assertTrue(css.contains("line-height:2"))
        assertTrue(css.contains("max-width:24em"))
        assertTrue(css.contains("margin-bottom:0.8em"))
        assertTrue(css.contains("font-family:Georgia"))
        assertTrue(css.contains("text-align:justify"))
        assertFalse(css.contains("color:"))
        // 字号不走 CSS（由 WebSettings.textZoom 处理）
        assertFalse(css.contains("font-size"))
    }

    // ===== Codec：宽容 + clamp =====

    @Test
    fun `decode all missing returns default`() {
        assertEquals(ReaderTypography.DEFAULT, ReaderTypographyCodec.decode(ReaderTypographyRaw()))
    }

    @Test
    fun `decode blank and non numeric strings return default`() {
        val t = ReaderTypographyCodec.decode(
            ReaderTypographyRaw(fontScale = "abc", lineHeight = "", widthStep = "NaN")
        )
        assertEquals(ReaderTypography.DEFAULT_FONT_SCALE, t.fontScale)
        assertEquals(ReaderTypography.UNSET, t.lineHeight)
        assertEquals(0, t.widthStep)
    }

    @Test
    fun `decode clamps out of range numeric values`() {
        val t = ReaderTypographyCodec.decode(
            ReaderTypographyRaw(
                fontScale = "999",
                lineHeight = "0.5",
                widthStep = "50",
                paragraphSpacing = "9",
                fontFamily = "7",
                textAlign = "-2"
            )
        )
        assertEquals(ReaderTypography.FONT_SCALE_MAX, t.fontScale)
        assertEquals(ReaderTypography.LINE_HEIGHT_MIN, t.lineHeight)
        assertEquals(ReaderTypography.WIDTH_STEP_MAX, t.widthStep)
        assertEquals(ReaderTypography.PARAGRAPH_SPACING_MAX, t.paragraphSpacing)
        assertEquals(ReaderTypography.FONT_FAMILY_MAX, t.fontFamily)
        assertEquals(0, t.textAlign)
    }

    @Test
    fun `decode keeps NaN and Infinity as unset`() {
        val t = ReaderTypographyCodec.decode(
            ReaderTypographyRaw(lineHeight = "NaN", paragraphSpacing = "Infinity")
        )
        assertEquals(ReaderTypography.UNSET, t.lineHeight)
        assertEquals(ReaderTypography.UNSET, t.paragraphSpacing)
    }

    @Test
    fun `decode preserves unset sentinel`() {
        val t = ReaderTypographyCodec.decode(
            ReaderTypographyRaw(lineHeight = "-1", paragraphSpacing = "-1")
        )
        assertEquals(ReaderTypography.UNSET, t.lineHeight)
        assertEquals(ReaderTypography.UNSET, t.paragraphSpacing)
    }

    @Test
    fun `encode decode round trip preserves valid values`() {
        val t = ReaderTypography(
            fontScale = 120,
            lineHeight = 1.75f,
            widthStep = 3,
            paragraphSpacing = 1.0f,
            fontFamily = 1,
            textAlign = 1
        )
        assertEquals(t, ReaderTypographyCodec.decode(ReaderTypographyCodec.encode(t)))
    }

    @Test
    fun `encode decode round trip preserves defaults`() {
        assertEquals(
            ReaderTypography.DEFAULT,
            ReaderTypographyCodec.decode(ReaderTypographyCodec.encode(ReaderTypography.DEFAULT))
        )
    }

    // ===== 档位表 =====

    @Test
    fun `font scales cover 85 to 150 with default at index 1`() {
        assertEquals(85, ReaderTypography.FONT_SCALES.first())
        assertEquals(150, ReaderTypography.FONT_SCALES.last())
        assertEquals(ReaderTypography.DEFAULT_FONT_SCALE, ReaderTypography.FONT_SCALES[1])
    }

    @Test
    fun `width em table index zero is default no override`() {
        assertEquals(0, ReaderTypography.WIDTH_EM[0])
        assertTrue(ReaderTypography.WIDTH_EM.drop(1).all { it > 0 })
    }
}
