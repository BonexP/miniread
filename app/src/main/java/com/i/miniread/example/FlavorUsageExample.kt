package com.i.miniread.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.i.miniread.BuildConfig
import com.i.miniread.eink.EInkConfig
import com.i.miniread.eink.EInkOptimized
import com.i.miniread.eink.EInkUtils

/**
 * Build Flavors 使用示例
 *
 * 这个文件展示了如何在实际开发中使用 Build Flavors 功能
 *
 * 注意：此文件在 IDE 中可能会显示一些错误（红色波浪线），这是正常的。
 * 因为 BuildConfig 和 flavor 特定的类只有在构建后才会生成。
 *
 * 解决方法：
 * 1. 在 Build Variants 面板中选择一个具体的变体（如 standardDebug）
 * 2. 同步项目（Sync Project with Gradle Files）
 * 3. 构建项目（Build → Make Project）
 *
 * 之后错误提示就会消失，代码会正常工作。
 */

/**
 * 示例 1：基础 Flavor 检测
 */
@Composable
fun FlavorInfoExample() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "当前版本: ${BuildConfig.FLAVOR_TYPE}",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "是否为 E-Ink 版: ${BuildConfig.IS_EINK}",
            style = MaterialTheme.typography.bodyMedium
        )

        if (BuildConfig.IS_EINK) {
            Text(
                text = "✓ E-Ink 优化已启用",
                color = EInkConfig.Colors.Secondary
            )
        }
    }
}

/**
 * 示例 2：自适应动画
 * 在 E-Ink 版本中自动禁用动画，在标准版中启用
 */
@Composable
fun AdaptiveAnimationExample() {
    var isVisible by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { isVisible = !isVisible }) {
            Text(if (isVisible) "隐藏" else "显示")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 根据 Flavor 自动调整动画
        AnimatedVisibility(
            visible = isVisible,
            enter = if (BuildConfig.IS_EINK) {
                // E-Ink: 无动画
                androidx.compose.animation.EnterTransition.None
            } else {
                // 标准版: 淡入动画
                fadeIn()
            },
            exit = if (BuildConfig.IS_EINK) {
                // E-Ink: 无动画
                androidx.compose.animation.ExitTransition.None
            } else {
                // 标准版: 淡出动画
                fadeOut()
            }
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(EInkConfig.Colors.Primary)
            )
        }
    }
}

/**
 * 示例 3：使用 E-Ink 优化包装器
 */
@Composable
fun EInkOptimizedExample() {
    EInkOptimized {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(EInkConfig.Colors.Background)
                .padding(16.dp)
        ) {
            Text(
                text = "E-Ink 优化的文本",
                color = EInkConfig.Colors.OnSurface,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "这个组件会自动应用 E-Ink 优化配置",
                color = EInkConfig.Colors.Secondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * 示例 4：条件渲染
 * 根据 Flavor 显示不同的 UI
 */
@Composable
fun ConditionalRenderingExample() {
    if (BuildConfig.IS_EINK) {
        // E-Ink 版本：简化的界面
        EInkSimplifiedUI()
    } else {
        // 标准版：完整的界面
        StandardFullUI()
    }
}

@Composable
private fun EInkSimplifiedUI() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EInkConfig.Colors.Background)
            .padding(16.dp)
    ) {
        Text(
            text = "简化界面（E-Ink 优化）",
            color = EInkConfig.Colors.OnSurface,
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "为电子墨水屏优化的简洁界面",
            color = EInkConfig.Colors.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StandardFullUI() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "完整界面（标准版）",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "包含所有功能的完整界面",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )

        // 更多复杂 UI 元素...
    }
}

/**
 * 示例 5：E-Ink 设备功能
 */
class EInkDeviceExample {

    fun demonstrateEInkFeatures() {
        // 检查是否是 E-Ink 设备
        if (EInkUtils.isEInkDevice()) {
            println("✓ 运行在 E-Ink 设备上")

            // 优化文本渲染
            EInkUtils.optimizeTextRendering()

            // 执行屏幕刷新
            EInkUtils.refreshScreen(fullRefresh = false)

            // 访问 E-Ink 配置
            val refreshMode = EInkConfig.defaultRefreshMode
            val disableAnimations = EInkConfig.DISABLE_ANIMATIONS

            println("刷新模式: $refreshMode")
            println("动画禁用: $disableAnimations")
        } else {
            println("运行在标准设备上")
        }
    }

    fun pageChangeExample(currentPage: Int) {
        // 每隔一定页数执行全屏刷新以清除残影
        if (BuildConfig.IS_EINK &&
            currentPage % EInkConfig.FULL_REFRESH_INTERVAL == 0) {
            EInkUtils.refreshScreen(fullRefresh = true)
        }
    }
}

/**
 * 示例 6：自适应颜色方案
 */
@Composable
fun AdaptiveColorSchemeExample() {
    // 使用 Flavor 特定的颜色
    val primaryColor = EInkConfig.Colors.Primary
    val backgroundColor = EInkConfig.Colors.Background
    val textColor = EInkConfig.Colors.OnSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Text(
            text = "自适应颜色方案",
            color = primaryColor,
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "这些颜色会根据当前 Flavor 自动调整：\n" +
                  "• 标准版：彩色主题\n" +
                  "• E-Ink 版：黑白优化",
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 示例 7：构建时配置
 */
object BuildConfigExample {

    fun printBuildInfo() {
        println("""
            ===== Build Configuration =====
            Flavor Type: ${BuildConfig.FLAVOR_TYPE}
            Is E-Ink: ${BuildConfig.IS_EINK}
            Version Name: ${BuildConfig.VERSION_NAME}
            Version Code: ${BuildConfig.VERSION_CODE}
            Application ID: ${BuildConfig.APPLICATION_ID}
            Build Type: ${BuildConfig.BUILD_TYPE}
            Debug: ${BuildConfig.DEBUG}
            ===============================
        """.trimIndent())
    }

    fun getFeatureFlags(): Map<String, Boolean> {
        return mapOf(
            "animations_enabled" to !EInkConfig.DISABLE_ANIMATIONS,
            "eink_optimization" to BuildConfig.IS_EINK,
            "text_contrast_enhancement" to EInkConfig.TEXT_CONTRAST_ENHANCEMENT,
            "is_debug" to BuildConfig.DEBUG
        )
    }
}

