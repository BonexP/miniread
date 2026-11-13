package com.i.miniread.util

import kotlin.math.round
import kotlin.math.roundToInt

/**
 * 阅读页正文排版配置（**纯逻辑，无 Android 依赖**，可直接跑纯 JVM 单测）。
 *
 * 对应 issue #19：字号 / 行距 / 行宽 / 段落间距 / 字体族 / 对齐，全局生效、持久化、
 * 调节时即时生效且不打断当前阅读位置。
 *
 * 设计要点：
 * - **字号走 `WebSettings.textZoom`**（不碰 CSS 字号）：手机用 rem 基数、平板用 px 字号，
 *   两套尺寸模型混着改会出现「手机生效、平板不动」；统一缩放是唯一同时命中两套模型的开关。
 *   `fontScale = 100` 对应既有的 `textZoom = 125`，因此默认值下观感逐像素不变。
 * - **行距 / 段落间距默认「跟随 CSS」**（哨兵 [UNSET]）：手机 1.6 / 平板 1.8 由样式表
 *   媒体查询决定，保持现状；用户一动就写入具体值，由排版表覆盖。
 * - 排版表**只写尺寸、不写颜色**：颜色归 `custom.css` / `customdark.css`，
 *   避免重演 issue #18「同一属性两个开关」的冲突。
 */
data class ReaderTypography(
    /** 字号百分比，85..150；100 = 默认（textZoom 125）。 */
    val fontScale: Int = DEFAULT_FONT_SCALE,
    /** 行距倍数，1.4..2.0；[UNSET] = 跟随 CSS（手机 1.6 / 平板 1.8）。 */
    val lineHeight: Float = UNSET,
    /** 行宽档位 0..3：0 = 默认（不限宽），1/2/3 = 窄 / 中 / 宽。 */
    val widthStep: Int = 0,
    /** 段落间距（em），0.8..2.0；[UNSET] = 跟随 CSS。 */
    val paragraphSpacing: Float = UNSET,
    /** 正文字体族：0 = 无衬线（跟随 CSS），1 = 衬线。 */
    val fontFamily: Int = 0,
    /** 对齐：0 = 左对齐（跟随 CSS），1 = 两端对齐。 */
    val textAlign: Int = 0
) {
    /**
     * 生成注入到 `<style id="miniread-typo-css">` 的 CSS 片段。
     *
     * 该块在 `template.html` 中位于主题表与平板表**之后**，同优先级下后注入者胜出，
     * 因此这里的 `body` 规则能覆盖平板的 `max-width: 800px/900px` 与 `line-height: 1.8`。
     * 只用 `max-width` + `margin: 0 auto`（不写死 width），只在超限时收窄。
     *
     * ⚠️ 不含任何颜色声明；`em` 相对元素自身字号，不依赖 html 的 6.4px rem 基数。
     */
    fun buildCss(): String {
        val sb = StringBuilder()
        sb.append("/* miniread reader typography: sizes only, no colors */\n")

        val bodyRules = mutableListOf<String>()
        if (lineHeight != UNSET) bodyRules += "line-height:${num(lineHeight)}"
        val widthEm = WIDTH_EM.getOrElse(widthStep) { 0 }
        if (widthEm > 0) {
            bodyRules += "max-width:${widthEm}em"
            bodyRules += "margin:0 auto"
        }
        if (fontFamily == FONT_FAMILY_SERIF) {
            bodyRules += "font-family:Georgia,'Times New Roman',serif"
        }
        if (textAlign == ALIGN_JUSTIFY) {
            bodyRules += "text-align:justify"
            bodyRules += "text-justify:inter-ideograph"
        }
        if (bodyRules.isNotEmpty()) {
            sb.append("body{").append(bodyRules.joinToString(";")).append(";}\n")
        }

        val blockRules = mutableListOf<String>()
        if (paragraphSpacing != UNSET) {
            blockRules += "margin-bottom:${num(paragraphSpacing)}em"
        }
        if (textAlign == ALIGN_JUSTIFY) {
            blockRules += "text-align:justify"
            blockRules += "text-justify:inter-ideograph"
        }
        if (blockRules.isNotEmpty()) {
            sb.append("p,li,blockquote{").append(blockRules.joinToString(";")).append(";}\n")
        }
        return sb.toString()
    }

    companion object {
        /** 「跟随样式表默认」哨兵：该字段不输出 CSS 覆盖。 */
        const val UNSET = -1f

        const val DEFAULT_FONT_SCALE = 100
        const val FONT_SCALE_MIN = 85
        const val FONT_SCALE_MAX = 150
        const val LINE_HEIGHT_MIN = 1.4f
        const val LINE_HEIGHT_MAX = 2.0f
        const val PARAGRAPH_SPACING_MIN = 0.8f
        const val PARAGRAPH_SPACING_MAX = 2.0f
        const val WIDTH_STEP_MAX = 3
        const val FONT_FAMILY_MAX = 1
        const val TEXT_ALIGN_MAX = 1

        const val FONT_FAMILY_SERIF = 1
        const val ALIGN_JUSTIFY = 1

        /** 字号四档（百分比）：小 / 默认 / 大 / 特大。 */
        val FONT_SCALES = listOf(85, 100, 120, 150)
        /** 行宽四档对应的 `max-width`（em）；索引 0 = 默认，不输出。 */
        val WIDTH_EM = listOf(0, 24, 32, 40)

        /** `fontScale = 100` 时对齐既有硬编码的 `textZoom = 125`。 */
        const val BASE_TEXT_ZOOM = 125

        val DEFAULT = ReaderTypography()

        /** 字号百分比 → `WebSettings.textZoom`。 */
        fun textZoomFor(fontScale: Int): Int =
            (BASE_TEXT_ZOOM * clamp(fontScale, FONT_SCALE_MIN, FONT_SCALE_MAX) / 100.0)
                .roundToInt()

        /** 把任意输入收进合法区间（非法字号也强制成合法值，不让 DataStore 脏数据漏到 UI）。 */
        fun clamp(value: Int, min: Int, max: Int): Int = value.coerceIn(min, max)

        fun clampFloat(value: Float, min: Float, max: Float): Float = value.coerceIn(min, max)

        /** Float → 紧凑字符串（最多 2 位小数），避免 CSS 里出现 `1.6000001`。 */
        fun num(v: Float): String {
            val r = round(v * 100f) / 100f
            return if (r == r.toLong().toFloat()) r.toLong().toString() else r.toString()
        }
    }
}

/** DataStore 六个 `typo_*` key 的原始文本视图（纯 JVM，便于单测解析容错）。 */
data class ReaderTypographyRaw(
    val fontScale: String? = null,
    val lineHeight: String? = null,
    val widthStep: String? = null,
    val paragraphSpacing: String? = null,
    val fontFamily: String? = null,
    val textAlign: String? = null
)

/**
 * 排版配置的纯解析 / 序列化。
 *
 * 红线：**宽容 + clamp**——DataStore 里出现缺失、空串、非数字、越界值时一律回落到
 * [ReaderTypography.DEFAULT] 对应字段，绝不把脏值传给 WebView 或 Slider。
 */
object ReaderTypographyCodec {

    fun decode(raw: ReaderTypographyRaw): ReaderTypography = ReaderTypography(
        fontScale = clampIntOr(
            raw.fontScale,
            ReaderTypography.FONT_SCALE_MIN,
            ReaderTypography.FONT_SCALE_MAX,
            ReaderTypography.DEFAULT_FONT_SCALE
        ),
        lineHeight = clampFloatOrUnset(
            raw.lineHeight,
            ReaderTypography.LINE_HEIGHT_MIN,
            ReaderTypography.LINE_HEIGHT_MAX
        ),
        widthStep = clampIntOr(
            raw.widthStep, 0, ReaderTypography.WIDTH_STEP_MAX, 0
        ),
        paragraphSpacing = clampFloatOrUnset(
            raw.paragraphSpacing,
            ReaderTypography.PARAGRAPH_SPACING_MIN,
            ReaderTypography.PARAGRAPH_SPACING_MAX
        ),
        fontFamily = clampIntOr(
            raw.fontFamily, 0, ReaderTypography.FONT_FAMILY_MAX, 0
        ),
        textAlign = clampIntOr(
            raw.textAlign, 0, ReaderTypography.TEXT_ALIGN_MAX, 0
        )
    )

    fun encode(t: ReaderTypography): ReaderTypographyRaw = ReaderTypographyRaw(
        fontScale = t.fontScale.toString(),
        lineHeight = t.lineHeight.toString(),
        widthStep = t.widthStep.toString(),
        paragraphSpacing = t.paragraphSpacing.toString(),
        fontFamily = t.fontFamily.toString(),
        textAlign = t.textAlign.toString()
    )

    private fun clampIntOr(raw: String?, min: Int, max: Int, fallback: Int): Int {
        val v = raw?.trim()?.toIntOrNull() ?: return fallback
        return v.coerceIn(min, max)
    }

    private fun clampFloatOrUnset(raw: String?, min: Float, max: Float): Float {
        val v = raw?.trim()?.toFloatOrNull() ?: return ReaderTypography.UNSET
        if (v.isNaN() || v.isInfinite()) return ReaderTypography.UNSET
        if (v == ReaderTypography.UNSET) return ReaderTypography.UNSET
        return v.coerceIn(min, max)
    }
}
