package com.i.miniread.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.i.miniread.BuildConfig

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

/**
 * E-Ink 优化的配色方案
 * 专为电子墨水屏设备设计，使用纯黑白配色以最大化对比度
 * 
 * ⚠️ 注意事项：
 * - 墨水屏不适合使用深色模式，始终使用浅色主题
 * - 所有容器统一为白色背景，避免灰度层次导致的刷新问题
 * - 使用纯黑色文字以提供最佳对比度
 */
private val EInkLightColorScheme = lightColorScheme(
    primary = Color.Black,
    secondary = Color.DarkGray,
    tertiary = Color.Gray,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    primaryContainer = Color.White,
    secondaryContainer = Color.White,
    tertiaryContainer = Color.White,
    onSecondaryContainer = Color.Black,
    onPrimaryContainer = Color.Black,
    onTertiaryContainer = Color.Black
)

/**
 * E-Ink 深色配色方案（备用）
 * 虽然墨水屏不推荐深色模式，但保留此方案以防万一需要
 */
private val EInkDarkColorScheme = darkColorScheme(
    primary = Color.Black,
    secondary = Color.DarkGray,
    tertiary = Color.Gray,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

/**
 * MiniRead 主题
 * 
 * 根据 Build Flavor 自动选择合适的主题：
 * - Standard: 支持动态颜色（Material You）和深色模式
 * - E-Ink: 使用固定的黑白配色，强制浅色主题
 * 
 * @param darkTheme 是否使用深色主题（E-Ink 版本忽略此参数）
 * @param dynamicColor 是否使用动态颜色（E-Ink 版本不支持）
 * @param content 主题内容
 */
@Composable
fun MinireadTheme(
    // ⚠️ E-Ink 版本默认不使用深色模式
    darkTheme: Boolean = if (BuildConfig.IS_EINK) false else isSystemInDarkTheme(),
    // ⚠️ E-Ink 版本不使用动态颜色
    dynamicColor: Boolean = !BuildConfig.IS_EINK,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val colorScheme = when {
        // E-Ink 版本：始终使用固定的黑白配色方案
        // 原因：墨水屏需要高对比度，且不适合深色模式
        BuildConfig.IS_EINK -> {
            EInkLightColorScheme
        }
        
        // 标准版：支持动态颜色（Android 12+）
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        // 标准版：使用预定义的深色/浅色主题
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}