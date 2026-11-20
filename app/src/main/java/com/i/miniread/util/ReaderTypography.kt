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
    /**
     * 字号百分比，85..150；100 = 默认（textZoom 125）。
     *
     * v5：UI 直接显示百分比数值（`85% / 100% / 120% / 150%`），即 [FONT_SCALES] 的字面值——
     * 因为 `textZoom = 125 × fontScale/100` 是纯比例，跨手机/平板都精确成立；显示成 px 会在
     * 另一类设备上说谎（手机默认 17.6px / 平板默认 22.5px，差 28%）。
     */
    val fontScale: Int = DEFAULT_FONT_SCALE,
    /** 行距倍数，1.4..2.0；[UNSET] = 跟随 CSS（手机 1.6 / 平板 1.8）。 */
    val lineHeight: Float = UNSET,
    /**
     * 行宽档位 0..3（**存储语义**）：0 = 不限宽（全宽，不输出 `max-width`），
     * 1/2/3 = 55% / 70% / 85%（窄 / 中 / 宽，相对**可用宽**）。
     *
     * v6（2026-11-20，推翻 v5 的 em 模型）：em 是**相对字号**的单位，在平板上被
     * `tablet_styles.css` 的 `body{font-size:18px/19px}` 与 WebView 的 text autosizing
     * 双重扰动——实测「宽」档（23em）在 800×1280dp 平板上只渲染出 **280dp / 占 WebView 40%**，
     * 而「全宽」是 667dp / 95%，一档之差 2.4 倍，用户观感「选宽反而不宽」。
     * 改为**百分比**后 `max-width` 只参与布局计算、不被字号模型影响，四档在任何设备上
     * 都稳定落在 55%/70%/85%/100% 的等比位置。
     *
     * 索引 0 仍是「不限」以保持默认值观感逐像素不变、旧持久化值零迁移；
     * UI 的展示顺序由 [WIDTH_STEP_ORDER] 决定（窄 → 中 → 宽 → 全宽），存储顺序不变。
     */
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
        val widthPercent = WIDTH_PERCENT.getOrElse(widthStep) { 0 }
        if (widthPercent > 0) {
            bodyRules += "max-width:${widthPercent}%"
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

        /**
         * 字号四档（百分比）。v5 起这四档**同时就是 UI 显示值**（`85% / 100% / 120% / 150%`）：
         * 它是 `textZoom = 125 × fontScale/100` 的输入比例，跨设备精确，不需要任何新常量。
         */
        val FONT_SCALES = listOf(85, 100, 120, 150)

        /**
         * 行宽四档对应的 `max-width`（**百分比**，相对 `body` 的包含块 = WebView 视口宽）；
         * 索引 0 = 不限（不输出 `max-width`，交给 `tablet_styles.css` 的 800/900px 上限）。
         *
         * v6（2026-11-20，推翻 v5 的 `em` 表 `[0, 18, 21, 23]`）：
         *
         * **为什么 em 不行**（真机实测，800×1280dp 平板、WebView 宽 700dp）：
         * - 「全宽」文字 = 667dp（占 95%）
         * - 「宽」23em 文字 = **280dp（占 40%）** ← 一档之差 2.4 倍，用户报「选宽反而不宽」
         * 反解实测得每 em ≈ 24.3px，而按 `body{font-size:18px} × textZoom 1.25` 只有 22.5px。
         * 偏差来自三处叠加：`body` 自身有 32px(平板)/48px(≥840px) 的 `padding` 且
         * `box-sizing:border-box` 会让 `max-width` 含 padding、WebView 的 text autosizing、
         * 以及 `em` 天然随字号缩放。**只要用字号相关单位，就永远测不准也调不稳**。
         *
         * **为什么百分比对**：`max-width:%` 相对包含块宽（WebView 视口），只参与布局计算，
         * 完全不经过字体渲染链路 → 四档在任何设备/任何字号下都稳定落在等比位置。
         * `box-sizing:border-box` 会让 `max-width` 含 `body` 的 padding，所以「100%」时
         * 文字区 = 视口宽 − 2×padding，与现状（不限宽）观感一致。
         *
         * **档位取值**：55 / 70 / 85，间距 15%，四档两两可辨；
         * 85% 到 100% 之间留 15% 的余量，让「宽」与「全宽」有明确区别又不至于过窄。
         * ⚠️ 单调性天然成立（百分比与字号、设备无关），旧 v5 那套「按 cap 反解」的
         * 安全界计算**不再需要**——这正是换单位最大的收益。
         */
        val WIDTH_PERCENT = listOf(0, 55, 70, 85)

        /**
         * 行宽档位的**展示序**（UI 从左到右 = 从窄到全宽）→ 存储索引。
         *
         * 用户反馈④的根因是旧 UI 从左到右排「默认 / 窄 / 中 / 宽」，而「默认」其实是
         * 「不限宽」（最宽）→ 选「宽」反而比选「默认」窄。v5 把「不限」显示在最右端，
         * 只加展示序映射、**不动存储语义**（默认 `widthStep = 0` 仍输出空 CSS，观感零变化）。
         */
        val WIDTH_STEP_ORDER = listOf(1, 2, 3, 0)

        /** 展示序索引 → 存储 `widthStep`；越界一律回落「不限」（与 Codec 的宽容口径一致）。 */
        fun widthStepForDisplayIndex(displayIndex: Int): Int =
            WIDTH_STEP_ORDER.getOrElse(displayIndex) { 0 }

        /** 存储 `widthStep` → 展示序索引；未知值一律显示为最后一档「全宽」。 */
        fun displayIndexForWidthStep(step: Int): Int =
            WIDTH_STEP_ORDER.indexOf(step).let { if (it < 0) WIDTH_STEP_ORDER.lastIndex else it }

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
