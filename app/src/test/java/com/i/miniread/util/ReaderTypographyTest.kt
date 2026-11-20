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
        assertTrue(css.contains("max-width:21em"))
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
        assertTrue(css.contains("max-width:18em"))
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

    // ===== v5：行宽展示序单调 + 旧持久化值兼容 =====

    @Test
    fun `width display order is strictly ascending in em and ends with unlimited`() {
        val emsInDisplayOrder = ReaderTypography.WIDTH_STEP_ORDER.map { ReaderTypography.WIDTH_EM[it] }
        assertTrue(
            "窄 < 中 < 宽 必须严格递增：$emsInDisplayOrder",
            emsInDisplayOrder.dropLast(1).zipWithNext().all { (a, b) -> b > a }
        )
        // 最右端是「不限」（不输出 max-width = 铺满可用宽）
        assertEquals(0, emsInDisplayOrder.last())
        // 展示序必须覆盖全部四档（不缺、不重）
        assertEquals(
            ReaderTypography.WIDTH_EM.indices.toList(),
            ReaderTypography.WIDTH_STEP_ORDER.sorted()
        )
    }

    @Test
    fun `width display index round trips through storage step`() {
        for (step in ReaderTypography.WIDTH_EM.indices) {
            assertEquals(
                step,
                ReaderTypography.widthStepForDisplayIndex(
                    ReaderTypography.displayIndexForWidthStep(step)
                )
            )
        }
    }

    @Test
    fun `unknown width values fall back to unlimited instead of crashing`() {
        // 越界展示序 → 存储值回落到「不限」（索引 0）
        assertEquals(0, ReaderTypography.widthStepForDisplayIndex(-1))
        assertEquals(0, ReaderTypography.widthStepForDisplayIndex(4))
        assertEquals(0, ReaderTypography.widthStepForDisplayIndex(999))
        // 未知存储值 → 显示为最后一档（「全宽」），不会越界
        assertEquals(
            ReaderTypography.WIDTH_STEP_ORDER.lastIndex,
            ReaderTypography.displayIndexForWidthStep(-3)
        )
        assertEquals(
            ReaderTypography.WIDTH_STEP_ORDER.lastIndex,
            ReaderTypography.displayIndexForWidthStep(9)
        )
    }

    @Test
    fun `legacy width steps 0 to 3 still decode without crash`() {
        // 旧版本写过 0..3（语义 0=不限 / 24 / 32 / 40em），升级后必须仍能解码且落在合法档位
        for (legacy in 0..ReaderTypography.WIDTH_STEP_MAX) {
            val decoded = ReaderTypographyCodec.decode(ReaderTypographyRaw(widthStep = legacy.toString()))
            assertTrue(decoded.widthStep in ReaderTypography.WIDTH_EM.indices)
            assertTrue(
                "旧值 $legacy 必须能映射到展示序",
                ReaderTypography.displayIndexForWidthStep(decoded.widthStep) in
                    ReaderTypography.WIDTH_STEP_ORDER.indices
            )
        }
        // 越界旧值仍被 clamp（宽容，不抛）
        assertEquals(
            ReaderTypography.WIDTH_STEP_MAX,
            ReaderTypographyCodec.decode(ReaderTypographyRaw(widthStep = "99")).widthStep
        )
    }

    /**
     * 通用单调性判据（D2 §2.2 / §4.4c）：最宽**显式**档在「最坏 emPx」下也不得超过
     * `tablet_styles.css` 的 `max-width` 上限，否则「宽」会比「全宽」还宽（反序）。
     *
     * zone800 = 视口 881–919dp（cap 800px，基准 18px，最高 textZoom 1.88）
     * zone900 = 视口 ≥980dp（cap 900px，基准 19px，最高 textZoom 1.88）
     */
    @Test
    fun `widest explicit width stays under tablet css cap at every font scale`() {
        val widestEm = ReaderTypography.WIDTH_EM.filter { it > 0 }.max()
        val worstZoom = ReaderTypography.textZoomFor(ReaderTypography.FONT_SCALE_MAX) / 100f

        val zone800EmPx = 18f * worstZoom
        val zone900EmPx = 19f * worstZoom

        assertTrue(
            "zone800: ${widestEm}em × ${zone800EmPx}px = ${widestEm * zone800EmPx}px 必须 ≤ 800px",
            widestEm * zone800EmPx <= 800f
        )
        assertTrue(
            "zone900: ${widestEm}em × ${zone900EmPx}px = ${widestEm * zone900EmPx}px 必须 ≤ 900px",
            widestEm * zone900EmPx <= 900f
        )
    }

    @Test
    fun `css max width strictly increases across display order`() {
        // 把「UI 顺序」与「CSS 实际效果」焊在一起：防止将来改了展示序却忘了改 WIDTH_EM。
        // 展示序是「窄 → 中 → 宽 → 全宽」，因此 max-width 必须严格递增（全宽 = 不输出 = ∞）。
        val widths = ReaderTypography.WIDTH_STEP_ORDER.map { step ->
            val css = ReaderTypography(widthStep = step).buildCss()
            val match = Regex("""max-width:(\d+)em""").find(css)
            match?.groupValues?.get(1)?.toFloat() ?: Float.POSITIVE_INFINITY
        }
        assertTrue(
            "展示序（窄→全宽）的 max-width 必须严格递增：$widths",
            widths.zipWithNext().all { (a, b) -> b > a }
        )
        assertEquals(Float.POSITIVE_INFINITY, widths.last())
    }

    // ===== v5：字号数值化（百分比）与 textZoom 自洽 =====

    @Test
    fun `font scales are strictly increasing so percent labels are monotonic`() {
        // UI 直接显示 FONT_SCALES 的字面值 + "%"，因此这张表本身必须单调
        assertTrue(ReaderTypography.FONT_SCALES.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun `font percent labels match text zoom actually applied`() {
        // 显示值（百分比）→ textZoom 必须严格单调，且默认档对齐既有硬编码 125
        val zooms = ReaderTypography.FONT_SCALES.map { ReaderTypography.textZoomFor(it) }
        assertEquals(listOf(106, 125, 150, 188), zooms)
        assertTrue(zooms.zipWithNext().all { (a, b) -> b > a })
        // 百分比与 textZoom 的比例关系自洽（125 是基准，容差来自 roundToInt）
        for ((scale, zoom) in ReaderTypography.FONT_SCALES.zip(zooms)) {
            val expected = ReaderTypography.BASE_TEXT_ZOOM * scale / 100.0
            assertTrue(
                "fontScale=$scale 的 textZoom=$zoom 必须与 $expected 相差 < 1",
                kotlin.math.abs(zoom - expected) < 1.0
            )
        }
    }
}
