package com.i.miniread.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.i.miniread.BuildConfig
import com.i.miniread.R
import com.i.miniread.util.ReaderTypography

/**
 * 阅读页排版调节面板（issue #19）。
 *
 * 形态：阅读页内容区顶部叠放的一块内联面板，**不占底栏**（底栏仍是 standard 6 键 /
 * E-Ink 7 键，且 E-Ink 保持 48dp，见 AGENTS.md §8 坑 8/17）。
 *
 * 红线：
 * - **无动画**：外层由调用方直接 `if (visible) { ... }` 组合，E-Ink 上任何渐变都是可见闪烁；
 *   本文件内不出现 AnimatedVisibility / animate*AsState。
 * - **编辑态 `rememberSaveable`**：面板可能被系统配置变化回收，普通 remember 会丢当前草稿
 *   （同隐藏设置页 §8 坑 17）。
 * - **只写尺寸、不写颜色**由 `ReaderTypography.buildCss()` 保证；本文件只负责收集输入。
 * - **E-Ink 只换样式、不重排**：差异全走 `val eink = BuildConfig.IS_EINK` 条件参数，
 *   不改分组 / 间距节奏 / 文案。
 * - 所有可见文案与 `contentDescription` 走 `stringResource`（§6）。
 */
@Composable
fun ReaderTypographyPanel(
    current: ReaderTypography,
    onChange: (ReaderTypography) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eink = BuildConfig.IS_EINK

    // ===== 草稿态（rememberSaveable：配置变化 / 离开组合后仍保留）=====
    var fontScaleStep by rememberSaveable(current) {
        mutableIntStateOf(
            ReaderTypography.FONT_SCALES.indexOf(current.fontScale).coerceAtLeast(1)
        )
    }
    var lineHeight by rememberSaveable(current) {
        mutableFloatStateOf(
            if (current.lineHeight == ReaderTypography.UNSET) 0f else current.lineHeight
        )
    }
    var widthStep by rememberSaveable(current) { mutableIntStateOf(current.widthStep) }
    var paragraphSpacing by rememberSaveable(current) {
        mutableFloatStateOf(
            if (current.paragraphSpacing == ReaderTypography.UNSET) 0f else current.paragraphSpacing
        )
    }
    var fontFamily by rememberSaveable(current) { mutableIntStateOf(current.fontFamily) }
    var textAlign by rememberSaveable(current) { mutableIntStateOf(current.textAlign) }

    // 0 = 「跟随默认」哨兵：Slider 最左端，不写具体值、不输出 CSS 覆盖
    fun emit() {
        onChange(
            ReaderTypography(
                fontScale = ReaderTypography.FONT_SCALES[fontScaleStep],
                lineHeight = if (lineHeight <= 0f) ReaderTypography.UNSET
                else ReaderTypography.clampFloat(
                    lineHeight,
                    ReaderTypography.LINE_HEIGHT_MIN,
                    ReaderTypography.LINE_HEIGHT_MAX
                ),
                widthStep = widthStep,
                paragraphSpacing = if (paragraphSpacing <= 0f) ReaderTypography.UNSET
                else ReaderTypography.clampFloat(
                    paragraphSpacing,
                    ReaderTypography.PARAGRAPH_SPACING_MIN,
                    ReaderTypography.PARAGRAPH_SPACING_MAX
                ),
                fontFamily = fontFamily,
                textAlign = textAlign
            )
        )
    }

    val surfaceShape = if (eink) RectangleShape else RoundedCornerShape(12.dp)
    val containerColor = if (eink) Color.White else MaterialTheme.colorScheme.surface
    val contentColor = if (eink) Color.Black else MaterialTheme.colorScheme.onSurface
    val sectionColor = if (eink) Color.Black else MaterialTheme.colorScheme.primary
    val border = if (eink) BorderStroke(2.dp, Color.Black) else null

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = surfaceShape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = border
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.reader_typography_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.reader_typography_reset))
                }
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.reader_typography_close))
                }
            }
            Spacer(Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // ---- 字号：四档 ----
                TypographySection(stringResource(R.string.reader_typography_font_size), sectionColor) {
                    val labels = listOf(
                        stringResource(R.string.reader_typography_font_size_small),
                        stringResource(R.string.reader_typography_font_size_normal),
                        stringResource(R.string.reader_typography_font_size_large),
                        stringResource(R.string.reader_typography_font_size_xlarge)
                    )
                    StepRow(labels, fontScaleStep, eink) {
                        fontScaleStep = it
                        emit()
                    }
                }

                // ---- 行距：1.4–2.0，0 = 跟随默认 ----
                TypographySection(stringResource(R.string.reader_typography_line_height), sectionColor) {
                    TypographySlider(
                        value = lineHeight,
                        valueRange = 0f..ReaderTypography.LINE_HEIGHT_MAX,
                        steps = 5,
                        unsetLabel = stringResource(R.string.reader_typography_follow_default),
                        format = { if (it <= 0f) null else ReaderTypography.num(it) },
                        eink = eink,
                        onValueChange = { lineHeight = it },
                        onValueChangeFinished = { emit() }
                    )
                }

                // ---- 行宽：四档（默认 / 窄 / 中 / 宽）----
                TypographySection(stringResource(R.string.reader_typography_width), sectionColor) {
                    val labels = listOf(
                        stringResource(R.string.reader_typography_width_default),
                        stringResource(R.string.reader_typography_width_narrow),
                        stringResource(R.string.reader_typography_width_medium),
                        stringResource(R.string.reader_typography_width_wide)
                    )
                    StepRow(labels, widthStep, eink) {
                        widthStep = it
                        emit()
                    }
                }

                // ---- 段落间距：0.8–2.0，0 = 跟随默认 ----
                TypographySection(
                    stringResource(R.string.reader_typography_paragraph_spacing),
                    sectionColor
                ) {
                    TypographySlider(
                        value = paragraphSpacing,
                        valueRange = 0f..ReaderTypography.PARAGRAPH_SPACING_MAX,
                        steps = 5,
                        unsetLabel = stringResource(R.string.reader_typography_follow_default),
                        format = { if (it <= 0f) null else "${ReaderTypography.num(it)}em" },
                        eink = eink,
                        onValueChange = { paragraphSpacing = it },
                        onValueChangeFinished = { emit() }
                    )
                }

                // ---- 字体族 ----
                TypographySection(stringResource(R.string.reader_typography_font_family), sectionColor) {
                    StepRow(
                        listOf(
                            stringResource(R.string.reader_typography_font_sans),
                            stringResource(R.string.reader_typography_font_serif)
                        ),
                        fontFamily,
                        eink
                    ) {
                        fontFamily = it
                        emit()
                    }
                }

                // ---- 对齐 ----
                TypographySection(stringResource(R.string.reader_typography_align), sectionColor) {
                    StepRow(
                        listOf(
                            stringResource(R.string.reader_typography_align_left),
                            stringResource(R.string.reader_typography_align_justify)
                        ),
                        textAlign,
                        eink
                    ) {
                        textAlign = it
                        emit()
                    }
                }
            }
        }
    }
}

@Composable
private fun TypographySection(
    title: String,
    titleColor: Color,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = titleColor,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
    content()
}

/**
 * 档位按钮行：E-Ink 用「直角 + 2dp 黑框 + 纯黑白」，standard 用 M3 默认按钮。
 * 行高按 48dp 保证触控目标。
 */
@Composable
private fun StepRow(
    labels: List<String>,
    selected: Int,
    eink: Boolean,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            val shape = if (eink) RectangleShape else RoundedCornerShape(8.dp)
            if (eink) {
                // 直角 + 2dp 黑框 + 纯黑白：与 hidden 设置页「次要按钮」同一套线条语言（§8 坑 17）
                OutlinedButton(
                    onClick = { onSelect(index) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = shape,
                    border = BorderStroke(2.dp, Color.Black),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) Color.Black else Color.White,
                        contentColor = if (isSelected) Color.White else Color.Black
                    )
                ) { Text(label, style = MaterialTheme.typography.labelMedium) }
            } else {
                if (isSelected) {
                    Button(
                        onClick = { onSelect(index) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = shape
                    ) { Text(label, style = MaterialTheme.typography.labelMedium) }
                } else {
                    OutlinedButton(
                        onClick = { onSelect(index) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = shape
                    ) { Text(label, style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}

/**
 * 连续滑杆：`value == 0f` 表示「跟随默认」（不输出 CSS 覆盖）。
 * 拖动过程中只改草稿，`onValueChangeFinished` 才写盘，避免每帧写 DataStore。
 */
@Composable
private fun TypographySlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    unsetLabel: String,
    format: (Float) -> String?,
    eink: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val display = format(value) ?: unsetLabel
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.weight(1f).height(48.dp),
            colors = if (eink) {
                SliderDefaults.colors(
                    thumbColor = Color.Black,
                    activeTrackColor = Color.Black,
                    inactiveTrackColor = Color.White,
                    activeTickColor = Color.White,
                    inactiveTickColor = Color.Black
                )
            } else {
                SliderDefaults.colors()
            }
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 32.dp)
                .then(if (eink) Modifier.border(1.dp, Color.Black) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = display,
                style = MaterialTheme.typography.labelMedium,
                color = if (eink) Color.Black else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
