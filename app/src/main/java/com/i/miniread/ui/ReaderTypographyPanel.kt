package com.i.miniread.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.i.miniread.BuildConfig
import com.i.miniread.R
import com.i.miniread.util.ReaderTypography
import kotlin.math.roundToInt

/** 数值步进的步长（行距 / 段落间距都是 0.1）。 */
private const val STEP_VALUE = 0.1f

/**
 * 阅读页排版调节面板（issue #19 v3）。
 *
 * 由调用方挂在正文 `Box` 的**尾兄弟节点**、作为覆盖式浮层呈现（竖屏顶部下拉 / 横屏侧边滑出，
 * 见 AGENTS.md §8 坑 20）；本文件只负责浮层内部：六个符号字段标题 + 按档位数统一的控件。
 *
 * 红线：
 * - **无动画**：外层调用方直接 `if (visible) { ... }` 组合，本文件内不出现
 *   `AnimatedVisibility` / `animate*AsState`（坑 18/20，E-Ink 残影）。
 * - **编辑态 `rememberSaveable`**：六个草稿全部用 `rememberSaveable` 持有（坑 17）。
 * - **控件按档位数统一**：数值增减（行距 / 段距）与四档字号用 `+/−` 步进器（行距 / 段距保留
 *   `UNSET`「跟随默认」）；四档行宽用四档按钮（窄面板 2×2）；两档（字体族 / 对齐）用 `Switch`。
 *   v3：字号滑块反直觉 → 索引步进、行宽回按钮，数据链路（`FONT_SCALES` / `emit` / textZoom）零改动。
 * - **字段名符号化**：`A / ↔ / ↕ / ¶ / Aa / ≡` 以文本字形呈现，每个用
 *   `Modifier.clearAndSetSemantics { contentDescription = stringResource(...) }` 挂可访问名（坑 11）。
 * - **E-Ink 只换样式、不重排**：差异全走 `val eink = BuildConfig.IS_EINK` 条件参数，
 *   纯黑白 + 直角 + 2dp 黑框，不改分组 / 间距节奏 / 文案 / 档位数。
 * - **文案与 contentDescription 全走 `stringResource`**（§6，中英 key 逐字一致）。
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
            // indexOf 命中 0（小号）时必须保留 0；只有不在四档列表里（-1）才回落默认档 1。
            ReaderTypography.FONT_SCALES.indexOf(current.fontScale).let { if (it < 0) 1 else it }
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

    // 0 = 「跟随默认」哨兵：步进器最左端，不写具体值、不输出 CSS 覆盖
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

    // 六个字段名可访问名：符号本身对读屏无意义，复用现有字段名 key（§6，两文件已一一对应）。
    // 在 @Composable 作用域提取，供下方各 section lambda 引用。
    val fontSizeLabel = stringResource(R.string.reader_typography_font_size)
    val lineHeightLabel = stringResource(R.string.reader_typography_line_height)
    val widthLabel = stringResource(R.string.reader_typography_width)
    val paragraphLabel = stringResource(R.string.reader_typography_paragraph_spacing)
    val fontFamilyLabel = stringResource(R.string.reader_typography_font_family)
    val alignLabel = stringResource(R.string.reader_typography_align)

    Surface(
        // v4：面板**按内容包裹高度**（不再 fillMaxSize 撑满浮层壳）。浮层壳给的是 `heightIn(max=…)`，
        // 内容矮时面板就矮（竖屏六段单行刚好一屏，不留大片空白），内容超高时由壳的上限封顶、
        // 内层 `verticalScroll` 接管滚动。根 Surface 即 E-Ink 的 2dp 黑框浮层边框。
        modifier = modifier,
        shape = surfaceShape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // v4：滚动容器改为**整块内容**（标题行 + 六段），而不是只包六段。
                // 这样竖屏内容矮时 Surface 按内容包裹（不留大片空白），内容超高时才由
                // 浮层壳的 `heightIn(max=…)` 封顶、本列负责滚动。
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // 标题行 v4 收紧：横屏浮层可用宽仅 ~400dp，默认 TextButton 每侧 24dp 内边距会把
            // 「恢复默认 / 关闭」挤出可用区；改用紧凑 contentPadding 让标题与两个动作同排放下。
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.reader_typography_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onReset,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(stringResource(R.string.reader_typography_reset), maxLines = 1)
                }
                TextButton(
                    onClick = onClose,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(stringResource(R.string.reader_typography_close), maxLines = 1)
                }
            }
            Spacer(Modifier.height(4.dp))

            // 六个可调项（顺序即展示顺序，标题为符号 + contentDescription）。只在这里定义一次，
            // 单列 / 两列共用同一份，避免两套排布各写一份控件逻辑后漂移（坑 20）。
            val sections: List<@Composable () -> Unit> = listOf(
                // ---- 字号：四档离散 → +/− 索引步进器（v3：替代反直觉的 Slider）----
                {
                    TypographySection(
                        symbol = stringResource(R.string.reader_typography_symbol_font_size),
                        label = fontSizeLabel,
                        titleColor = sectionColor
                    ) {
                        TypographyIndexStepper(
                            index = fontScaleStep,
                            labels = listOf(
                                stringResource(R.string.reader_typography_font_size_small),
                                stringResource(R.string.reader_typography_font_size_normal),
                                stringResource(R.string.reader_typography_font_size_large),
                                stringResource(R.string.reader_typography_font_size_xlarge)
                            ),
                            fieldLabel = fontSizeLabel,
                            eink = eink,
                            onIndexChange = { fontScaleStep = it; emit() }
                        )
                    }
                },
                // ---- 行距：数值增减 1.4–2.0，0 = 跟随默认 ----
                {
                    TypographySection(
                        symbol = stringResource(R.string.reader_typography_symbol_line_height),
                        label = lineHeightLabel,
                        titleColor = sectionColor
                    ) {
                        TypographyStepper(
                            value = lineHeight,
                            min = ReaderTypography.LINE_HEIGHT_MIN,
                            max = ReaderTypography.LINE_HEIGHT_MAX,
                            suffix = "",
                            fieldLabel = lineHeightLabel,
                            eink = eink,
                            onValueChange = { lineHeight = it; emit() }
                        )
                    }
                },
                // ---- 行宽：四档（默认/窄/中/宽）→ 四档按钮（v3：窄面板 2×2 兜底）----
                {
                    TypographySection(
                        symbol = stringResource(R.string.reader_typography_symbol_width),
                        label = widthLabel,
                        titleColor = sectionColor
                    ) {
                        TypographySegmentedButtons(
                            step = widthStep,
                            labels = listOf(
                                stringResource(R.string.reader_typography_width_default),
                                stringResource(R.string.reader_typography_width_narrow),
                                stringResource(R.string.reader_typography_width_medium),
                                stringResource(R.string.reader_typography_width_wide)
                            ),
                            fieldLabel = widthLabel,
                            eink = eink,
                            onStepChange = { widthStep = it; emit() }
                        )
                    }
                },
                // ---- 段落间距：数值增减 0.8–2.0，0 = 跟随默认 ----
                {
                    TypographySection(
                        symbol = stringResource(R.string.reader_typography_symbol_paragraph_spacing),
                        label = paragraphLabel,
                        titleColor = sectionColor
                    ) {
                        TypographyStepper(
                            value = paragraphSpacing,
                            min = ReaderTypography.PARAGRAPH_SPACING_MIN,
                            max = ReaderTypography.PARAGRAPH_SPACING_MAX,
                            suffix = stringResource(R.string.reader_typography_unit_em),
                            fieldLabel = paragraphLabel,
                            eink = eink,
                            onValueChange = { paragraphSpacing = it; emit() }
                        )
                    }
                },
                // ---- 字体族：两档 → Switch ----
                {
                    TypographySection(
                        symbol = stringResource(R.string.reader_typography_symbol_font_family),
                        label = fontFamilyLabel,
                        titleColor = sectionColor
                    ) {
                        TypographyTwoWaySwitch(
                            checked = fontFamily == ReaderTypography.FONT_FAMILY_SERIF,
                            leftLabel = stringResource(R.string.reader_typography_font_sans),
                            rightLabel = stringResource(R.string.reader_typography_font_serif),
                            label = fontFamilyLabel,
                            eink = eink,
                            onCheckedChange = {
                                fontFamily = if (it) ReaderTypography.FONT_FAMILY_SERIF else 0
                                emit()
                            }
                        )
                    }
                },
                // ---- 对齐：两档 → Switch ----
                {
                    TypographySection(
                        symbol = stringResource(R.string.reader_typography_symbol_align),
                        label = alignLabel,
                        titleColor = sectionColor
                    ) {
                        TypographyTwoWaySwitch(
                            checked = textAlign == ReaderTypography.ALIGN_JUSTIFY,
                            leftLabel = stringResource(R.string.reader_typography_align_left),
                            rightLabel = stringResource(R.string.reader_typography_align_justify),
                            label = alignLabel,
                            eink = eink,
                            onCheckedChange = {
                                textAlign = if (it) ReaderTypography.ALIGN_JUSTIFY else 0
                                emit()
                            }
                        )
                    }
                }
            )

            // 浮层壳（调用方）按 docking 边给宽/高上限；外层 Column 已改为滚动容器并包裹内容，
            // 这里只按可用宽度决定列数（用 `BoxWithConstraints` 读宽度，不再需要高度分配）。
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // v3：两列阈值降到 480dp —— 浮层壳上限 600dp，600dp 阈值在覆盖面板里恒不可达，
                // 横屏六段只能单列堆叠（诊断①的结构性放大器）。窄列每段控件仍能自适应换行 / 2×2 兜底。
                val useTwoColumns = maxWidth >= 480.dp
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (useTwoColumns) {
                        // 逐对成行；奇数项时最后一格留空，保持左右列对齐
                        sections.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                pair.forEach { section ->
                                    Box(modifier = Modifier.weight(1f)) { section() }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    } else {
                        sections.forEach { section -> section() }
                    }
                }
            }
        }
    }
}

/**
 * 符号字段标题 + 控件（v4 起改为**单行**：符号在左、控件占满剩余宽度）。
 *
 * 符号对读屏无意义，用 `clearAndSetSemantics` 把可见字形替换为字段名，
 * 让辅助技术读出「字号 / 行距 / ...」而不是单个字母（§6 / 坑 11）。
 *
 * ⚠️ v4（2026-11-19 真机实测）：此前是「符号标题一行 + 控件一行」的两行结构，
 * 六段合计约 600dp，竖屏/横屏面板一屏只能看到 3.5 段，用户报「显示不全」。
 * 改为单行后六段约 6×56dp ≈ 336dp，配合面板的确定高度可一屏看全（见 AGENTS §8 坑 20）。
 * 单行仍只用 `Row` 一个直接子节点，两列模式 `Box(weight(1f)){ section() }` 不会层叠（v3 的 VA-01 修法保持）。
 */
@Composable
private fun TypographySection(
    symbol: String,
    label: String,
    titleColor: Color,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .width(28.dp)
                .clearAndSetSemantics { contentDescription = label }
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

/**
 * 数值增减步进器：`− | 值 | +`，`value == 0f` 表示「跟随默认」（不输出 CSS 覆盖）。
 * 到边界时按钮不置灰（灰在 E-Ink 上是脏块），仅忽略越界点击。
 */
@Composable
private fun TypographyStepper(
    value: Float,
    min: Float,
    max: Float,
    suffix: String,
    fieldLabel: String,
    eink: Boolean,
    onValueChange: (Float) -> Unit
) {
    // stringResource 只能在 @Composable 作用域调用，先提取再用于 semantics / display
    val unsetLabel = stringResource(R.string.reader_typography_follow_default)
    val increaseDesc = stringResource(R.string.reader_typography_increase, fieldLabel)
    val decreaseDesc = stringResource(R.string.reader_typography_decrease, fieldLabel)
    val plusSymbol = stringResource(R.string.reader_typography_symbol_increase)
    val minusSymbol = stringResource(R.string.reader_typography_symbol_decrease)
    val display = if (value <= 0f) unsetLabel else "${ReaderTypography.num(value)}$suffix"

    val buttonShape = if (eink) RectangleShape else RoundedCornerShape(8.dp)
    val buttonBorder = if (eink) {
        BorderStroke(2.dp, Color.Black)
    } else {
        // standard 也描边：无边框 + 透明底会让按钮在浅色主题下「融进白底」看不见（真机反馈）。
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }
    val buttonColors = if (eink) {
        ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        )
    } else {
        // standard：显式对比色，避免 M3 默认 outlined 按钮（透明底 + 低对比 outline）
        // 在浅色主题下「白底融白底」看不见 +/−（真机反馈）。
        ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    // ⚠️ v4 真机修复（2026-11-19）：`Modifier.size(48.dp)` 已把宽度锁死为 48dp，而 M3 1.2.1 的
    //    `ButtonDefaults.ContentPadding` 是 horizontal 24dp + 24dp → 内容可用宽 = 0，
    //    `+`/`−` 字形被压成 0 宽**压根没画出来**（真机只有空框、点击却生效）。
    //    必须显式 `contentPadding = PaddingValues(0.dp)`；48dp 命中区由 size 保证（§4 触控红线不变）。
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedButton(
            onClick = { onValueChange(steppedValue(value, -1, min, max)) },
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = decreaseDesc },
            shape = buttonShape,
            border = buttonBorder,
            colors = buttonColors,
            contentPadding = PaddingValues(0.dp)
        ) { Text(minusSymbol, style = MaterialTheme.typography.titleMedium) }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .then(if (eink) Modifier.border(1.dp, Color.Black) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = display,
                style = MaterialTheme.typography.labelMedium,
                color = if (eink) Color.Black else MaterialTheme.colorScheme.onSurface
            )
        }

        OutlinedButton(
            onClick = { onValueChange(steppedValue(value, 1, min, max)) },
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = increaseDesc },
            shape = buttonShape,
            border = buttonBorder,
            colors = buttonColors,
            contentPadding = PaddingValues(0.dp)
        ) { Text(plusSymbol, style = MaterialTheme.typography.titleMedium) }
    }
}

/**
 * 两档拨片：左标签 | Switch | 右标签。E-Ink 复用坑 17 的黑白轨钮配方，选中侧标签反色
 * （黑底白字）表达，不引入灰。
 */
@Composable
private fun TypographyTwoWaySwitch(
    checked: Boolean,
    leftLabel: String,
    rightLabel: String,
    label: String,
    eink: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val switchColors = if (eink) {
        SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Color.Black,
            checkedBorderColor = Color.Black,
            uncheckedThumbColor = Color.Black,
            uncheckedTrackColor = Color.White,
            uncheckedBorderColor = Color.Black
        )
    } else {
        SwitchDefaults.colors()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TwoWayLabel(text = leftLabel, active = !checked, eink = eink, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = switchColors,
            modifier = Modifier.semantics { contentDescription = label }
        )
        TwoWayLabel(text = rightLabel, active = checked, eink = eink, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TwoWayLabel(
    text: String,
    active: Boolean,
    eink: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = if (eink) RectangleShape else RoundedCornerShape(8.dp)
    if (eink) {
        Box(
            modifier = modifier
                .height(40.dp)
                .border(2.dp, Color.Black, shape)
                .background(if (active) Color.Black else Color.White, shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (active) Color.White else Color.Black,
                maxLines = 1
            )
        }
    } else {
        Box(
            modifier = modifier
                .height(40.dp)
                .background(
                    if (active) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * 离散档位索引步进器（v3 字号控件）：`− | 当前档位文本 | +`，与数值步进器同构。
 * 档位文本用 `labelMedium` 保证 MD3 字号层级清晰；到边界忽略点击、不置灰（坑 18）。
 * 48dp 触控目标、E-Ink 纯黑白 2dp 框、semantics 配方与 [TypographyStepper] 一致。
 */
@Composable
private fun TypographyIndexStepper(
    index: Int,
    labels: List<String>,
    fieldLabel: String,
    eink: Boolean,
    onIndexChange: (Int) -> Unit
) {
    val increaseDesc = stringResource(R.string.reader_typography_increase, fieldLabel)
    val decreaseDesc = stringResource(R.string.reader_typography_decrease, fieldLabel)
    val plusSymbol = stringResource(R.string.reader_typography_symbol_increase)
    val minusSymbol = stringResource(R.string.reader_typography_symbol_decrease)
    val safeIndex = index.coerceIn(labels.indices)
    val display = labels[safeIndex]

    val buttonShape = if (eink) RectangleShape else RoundedCornerShape(8.dp)
    val buttonBorder = if (eink) {
        BorderStroke(2.dp, Color.Black)
    } else {
        // standard 也描边：无边框 + 透明底会让按钮在浅色主题下「融进白底」看不见（真机反馈）。
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }
    val buttonColors = if (eink) {
        ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        )
    } else {
        // standard：显式对比色，避免浅色主题下「白底融白底」看不见 +/−（同 TypographyStepper）。
        ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedButton(
            onClick = { if (safeIndex > 0) onIndexChange(safeIndex - 1) },
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = decreaseDesc },
            shape = buttonShape,
            border = buttonBorder,
            colors = buttonColors,
            // ⚠️ 同 TypographyStepper：size(48dp) 锁宽后必须清零 M3 默认 24dp×2 内容内边距，
            //    否则 `−` 字形可用宽为 0、不渲染（v4 真机修复）。
            contentPadding = PaddingValues(0.dp)
        ) { Text(minusSymbol, style = MaterialTheme.typography.titleMedium) }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .then(if (eink) Modifier.border(1.dp, Color.Black) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = display,
                style = MaterialTheme.typography.labelMedium,
                color = if (eink) Color.Black else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }

        OutlinedButton(
            onClick = { if (safeIndex < labels.lastIndex) onIndexChange(safeIndex + 1) },
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = increaseDesc },
            shape = buttonShape,
            border = buttonBorder,
            colors = buttonColors,
            contentPadding = PaddingValues(0.dp)
        ) { Text(plusSymbol, style = MaterialTheme.typography.titleMedium) }
    }
}

/**
 * 四档分段按钮（v3 行宽控件）：四档等宽、48dp 高，选中态 standard `primaryContainer` /
 * E-Ink 黑底白字。窄面板（每档宽 < 64dp）降为 2×2 网格，避免英文标签折行；
 * 每档 `maxLines = 1` 兜底。文案与 contentDescription 全走资源（坑 11）。
 */
@Composable
private fun TypographySegmentedButtons(
    step: Int,
    labels: List<String>,
    fieldLabel: String,
    eink: Boolean,
    onStepChange: (Int) -> Unit
) {
    val safeStep = step.coerceIn(labels.indices)
    val buttonShape = if (eink) RectangleShape else RoundedCornerShape(8.dp)
    val buttonBorder = if (eink) {
        BorderStroke(2.dp, Color.Black)
    } else {
        // standard 也描边：无边框 + 透明底会让按钮在浅色主题下「融进白底」看不见（真机反馈）。
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // 四档按钮每档最小 64dp（技能 component min-width）+ spacedBy(4dp)×3 → 268dp；
        // 低于此宽度用 2×2，避免英文 Default/Narrow/Medium/Wide 在单档里折行。
        val twoRows = maxWidth < 268.dp
        val chunks: List<List<Int>> = if (twoRows) {
            listOf(listOf(0, 1), listOf(2, 3))
        } else {
            listOf(labels.indices.toList())
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            chunks.forEach { rowIndices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rowIndices.forEach { thisStep ->
                        val text = labels[thisStep]
                        val selected = thisStep == safeStep
                        val colors = if (eink) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) Color.Black else Color.White,
                                contentColor = if (selected) Color.White else Color.Black
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                contentColor = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        // stringResource 只能在 @Composable 作用域调用：先提取，再用于 semantics lambda。
                        val optionDesc = stringResource(
                            R.string.reader_typography_width_option,
                            fieldLabel,
                            text
                        )
                        OutlinedButton(
                            onClick = { onStepChange(thisStep) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .semantics { contentDescription = optionDesc },
                            shape = buttonShape,
                            border = buttonBorder,
                            colors = colors,
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 数值增减一步：`0f`（跟随默认）↔ 合法区间，0 是 MIN 左侧的额外一档。 */
private fun steppedValue(current: Float, delta: Int, min: Float, max: Float): Float {
    val next = when {
        delta > 0 && current <= 0f -> min
        delta > 0 -> (current + STEP_VALUE).coerceAtMost(max)
        delta < 0 && current <= min -> 0f
        else -> (current - STEP_VALUE).coerceAtLeast(min)
    }
    // 消除 1.4000001 这类浮点漂移
    return (next * 100f).roundToInt() / 100f
}
