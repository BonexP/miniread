# 详细迁移说明文档

## 文档说明

本文档提供从独立的 main 和 eink 分支迁移到统一 Build Flavors 架构的详细步骤和指导。

**创建日期**: 2025-10-26  
**适用场景**: 合并独立分支到 Build Flavors  
**预计时间**: 2-4 小时

---

## 迁移概述

### 迁移目标

将以下结构：

```
独立分支模式:
├── main 分支 (标准版)
│   └── 完整的应用代码
└── eink 分支 (E-Ink 版)
    └── 完整的应用代码（有部分修改）
```

迁移到：

```
Build Flavors 模式:
└── dev-merge 分支
    ├── src/main/ (共享代码 85%)
    ├── src/standard/ (标准版特定 7%)
    └── src/eink/ (E-Ink 版特定 8%)
```

### 迁移优势

- ✅ 单一代码库，统一维护
- ✅ 减少代码重复
- ✅ 简化 bug 修复（只需修复一次）
- ✅ 统一的版本管理
- ✅ 同时支持两个产品变体

### 迁移风险

- ⚠️ 需要重构现有代码
- ⚠️ 可能引入新的 bug
- ⚠️ 需要重新测试两个版本
- ⚠️ CI/CD 配置需要更新

---

## 前置准备

### 1. 环境检查

```bash
# 检查 Git 版本
git --version  # 需要 2.0+

# 检查 Gradle 版本
./gradlew --version  # 需要 7.0+

# 检查 Android Studio 版本
# 需要 Android Studio Flamingo (2022.2.1) 或更高版本

# 检查 Kotlin 版本
# 需要 Kotlin 1.9.0+
```

### 2. 备份现有分支

```bash
# 备份 main 分支
git checkout main
git branch backup-main-$(date +%Y%m%d)

# 备份 eink 分支
git checkout eink
git branch backup-eink-$(date +%Y%m%d)

# 列出所有分支（验证备份）
git branch -a
```

### 3. 创建迁移工作分支

```bash
# 基于最新的 main 分支创建
git checkout main
git pull origin main
git checkout -b migration-to-flavors

# 或基于现有的 dev-merge 分支
git checkout dev-merge
git pull origin dev-merge
```

---

## 迁移步骤

### 阶段 1: 构建配置 (30 分钟)

#### 1.1 配置 Build Flavors

编辑 `app/build.gradle.kts`:

```kotlin
android {
    namespace = "com.i.miniread"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.i.miniread"
        minSdk = 25
        targetSdk = 34
        // ... 其他配置
    }

    // ===== 添加 Build Flavors 配置 =====
    
    // 步骤 1: 定义 Flavor 维度
    flavorDimensions += "version"

    // 步骤 2: 定义产品 Flavors
    productFlavors {
        create("standard") {
            dimension = "version"
            applicationIdSuffix = ""
            versionNameSuffix = "-standard"
            buildConfigField("String", "FLAVOR_TYPE", "\"standard\"")
            buildConfigField("boolean", "IS_EINK", "false")
            resValue("string", "app_name", "MiniRead")
        }

        create("eink") {
            dimension = "version"
            applicationIdSuffix = ".eink"
            versionNameSuffix = "-eink"
            buildConfigField("String", "FLAVOR_TYPE", "\"eink\"")
            buildConfigField("boolean", "IS_EINK", "true")
            resValue("string", "app_name", "MiniRead E-Ink")
        }
    }

    // 步骤 3: 启用 BuildConfig
    buildFeatures {
        buildConfig = true
        compose = true
    }
}
```

#### 1.2 同步项目

```bash
# 在 Android Studio 中
File → Sync Project with Gradle Files

# 或使用命令行
./gradlew --refresh-dependencies
```

#### 1.3 验证配置

```bash
# 列出所有构建变体
./gradlew tasks --all | grep assemble

# 应该看到：
# assembleStandardDebug
# assembleStandardRelease
# assembleEinkDebug
# assembleEinkRelease
```

---

### 阶段 2: 目录结构创建 (15 分钟)

#### 2.1 创建 Flavor 特定目录

```bash
cd app/src

# 创建 standard flavor 目录
mkdir -p standard/java/com/i/miniread/eink
mkdir -p standard/res/values

# 创建 eink flavor 目录
mkdir -p eink/java/com/i/miniread/eink
mkdir -p eink/res/values
```

#### 2.2 验证目录结构

```bash
tree app/src -L 3
```

期望输出：
```
app/src/
├── main/
│   ├── java/
│   └── res/
├── standard/
│   ├── java/
│   │   └── com/i/miniread/eink/
│   └── res/
│       └── values/
└── eink/
    ├── java/
    │   └── com/i/miniread/eink/
    └── res/
        └── values/
```

---

### 阶段 3: 代码迁移 (90 分钟)

#### 3.1 识别差异代码

使用以下命令查找 main 和 eink 分支的差异：

```bash
# 检出两个分支到不同目录（在工作区外）
cd /tmp
git clone <repository-url> main-branch
cd main-branch && git checkout main

cd /tmp
git clone <repository-url> eink-branch
cd eink-branch && git checkout eink

# 使用 diff 工具比较
diff -r /tmp/main-branch/app/src /tmp/eink-branch/app/src > differences.txt
```

或在 Git 中：

```bash
git diff origin/main origin/eink -- app/src > differences.txt
```

#### 3.2 分析差异文件

根据之前的分析，主要差异在：

1. `MainActivity.kt` - 音量键处理
2. `Theme.kt` - 主题配色
3. `ArticleDetailScreen.kt` - WebView 优化
4. `*.Screen.kt` - UI 布局
5. `custom.css` - 样式文件

#### 3.3 迁移策略

**策略 A: 在共享代码中使用条件判断**

适用于：大部分代码相同，只有少量差异

```kotlin
// 示例：MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // ✅ 使用 BuildConfig 判断
        if (BuildConfig.IS_EINK) {
            return when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    val webView = (currentFocus as? WebView)
                    webView?.scrollBy(0, -500)
                    true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val webView = (currentFocus as? WebView)
                    webView?.scrollBy(0, 800)
                    true
                }
                else -> super.onKeyDown(keyCode, event)
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
```

**策略 B: 创建 Flavor 特定文件**

适用于：实现完全不同的功能

```kotlin
// standard/java/com/i/miniread/eink/EInkUtils.kt
object EInkUtils {
    fun refreshScreen(mode: RefreshMode) {
        // 标准版：空实现
    }
}

// eink/java/com/i/miniread/eink/EInkUtils.kt
object EInkUtils {
    fun refreshScreen(mode: RefreshMode) {
        // E-Ink 版：实际实现
        // 调用设备 SDK...
    }
}
```

#### 3.4 具体迁移步骤

##### 步骤 1: 迁移 MainActivity.kt

```bash
# 1. 检出 main 分支的文件到 src/main/
git checkout origin/main -- app/src/main/java/com/i/miniread/MainActivity.kt

# 2. 从 eink 分支提取差异
git show origin/eink:app/src/main/java/com/i/miniread/MainActivity.kt > /tmp/eink-MainActivity.kt

# 3. 手动合并差异，添加 BuildConfig 判断
```

编辑 `src/main/java/com/i/miniread/MainActivity.kt`:

```kotlin
import com.i.miniread.BuildConfig

class MainActivity : ComponentActivity() {
    // ===== E-Ink 特定功能 =====
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // ⚠️ 注意：仅在 E-Ink 版本启用音量键翻页
        // 需要 WebView 获得焦点才能工作
        if (BuildConfig.IS_EINK) {
            return when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    val webView = (currentFocus as? WebView)
                    if (webView != null) {
                        webView.scrollBy(0, -500)
                        Log.d("MainActivity", "Volume up - scroll up")
                        true
                    } else {
                        Log.w("MainActivity", "WebView not focused")
                        super.onKeyDown(keyCode, event)
                    }
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val webView = (currentFocus as? WebView)
                    if (webView != null) {
                        webView.scrollBy(0, 800)
                        Log.d("MainActivity", "Volume down - scroll down")
                        true
                    } else {
                        Log.w("MainActivity", "WebView not focused")
                        super.onKeyDown(keyCode, event)
                    }
                }
                else -> super.onKeyDown(keyCode, event)
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    // ===== UI 尺寸调整 =====
    @Composable
    fun MainContent(...) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            // ✅ E-Ink 使用较小字体
                            style = if (BuildConfig.IS_EINK)
                                MaterialTheme.typography.titleMedium
                            else
                                MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(onClick = { /* ... */ }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                // ✅ E-Ink 使用较小图标
                                modifier = if (BuildConfig.IS_EINK)
                                    Modifier.size(18.dp)
                                else
                                    Modifier
                            )
                        }
                    },
                    // ✅ E-Ink 使用较小高度
                    modifier = if (BuildConfig.IS_EINK)
                        Modifier.height(40.dp)
                    else
                        Modifier
                )
            },
            // ... 其他代码
        )
    }
}
```

##### 步骤 2: 迁移 Theme.kt

```kotlin
// src/main/java/com/i/miniread/ui/theme/Theme.kt
import com.i.miniread.BuildConfig

// ===== E-Ink 配色方案 =====
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
)

// ===== 标准版配色方案 =====
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

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
        // E-Ink 版本始终使用固定的黑白配色
        BuildConfig.IS_EINK -> {
            EInkLightColorScheme
        }
        
        // 标准版支持动态颜色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) 
            else dynamicLightColorScheme(context)
        }
        
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

##### 步骤 3: 迁移 ArticleDetailScreen.kt

```kotlin
// src/main/java/com/i/miniread/ui/ArticleDetailScreen.kt
import com.i.miniread.BuildConfig

@Composable
fun ArticleActionsBar(...) {
    BottomAppBar(
        // ✅ E-Ink 版本使用白色背景
        containerColor = if (BuildConfig.IS_EINK) 
            Color.White 
            else MaterialTheme.colorScheme.surface
    ) {
        // 操作按钮...
    }
}

@Composable
fun ArticleWebView(...) {
    val webView = remember {
        WebView(context).apply {
            // ===== E-Ink 优化：强化焦点管理 =====
            if (BuildConfig.IS_EINK) {
                // ⚠️ 注意：确保 WebView 获得焦点以支持音量键翻页
                post {
                    requestFocus()
                    Log.d("WebViewFocus", "Initial focus: ${hasFocus()}")
                }
                postDelayed({
                    requestFocus()
                    Log.d("WebViewFocus", "Delayed focus: ${hasFocus()}")
                }, 500)
            }
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                // ... 其他设置
                
                // ===== E-Ink 优化：隐藏滚动条 =====
                // ⚠️ 原因：墨水屏刷新滚动条会产生残影
                isVerticalScrollBarEnabled = !BuildConfig.IS_EINK
                
                if (BuildConfig.IS_EINK) {
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocusFromTouch()
                }
            }
        }
    }
}

// ===== CSS 加载优化 =====
fun loadHtmlContentAsync(context: Context, content: String, onHtmlReady: (String) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        if (cachedCustomCss == null) {
            // ⚠️ E-Ink 版本不使用深色 CSS
            cachedCustomCss = if (BuildConfig.IS_EINK) {
                readAssetFile(context, "custom.css")
            } else {
                val isDarkMode = (context.resources.configuration.uiMode and
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                readAssetFile(context, if (isDarkMode) "customdark.css" else "custom.css")
            }
        }
        // ... 其他代码
    }
}
```

##### 步骤 4: 迁移其他 UI 屏幕

对于列表屏幕（EntryListScreen.kt, FeedListScreen.kt, CategoryListScreen.kt），可以通过资源文件处理尺寸差异：

```xml
<!-- src/main/res/values/dimens.xml -->
<resources>
    <dimen name="list_item_vertical_padding">8dp</dimen>
    <dimen name="list_item_horizontal_padding">16dp</dimen>
</resources>

<!-- src/eink/res/values/dimens.xml -->
<resources>
    <dimen name="list_item_vertical_padding">4dp</dimen>
    <dimen name="list_item_horizontal_padding">12dp</dimen>
</resources>
```

然后在代码中使用：

```kotlin
@Composable
fun EntryListScreen(...) {
    LazyColumn(
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.list_item_horizontal_padding),
            vertical = dimensionResource(R.dimen.list_item_vertical_padding)
        )
    ) {
        // 列表项...
    }
}
```

##### 步骤 5: 创建 Flavor 特定工具类

```kotlin
// standard/java/com/i/miniread/eink/EInkConfig.kt
package com.i.miniread.eink

import androidx.compose.ui.graphics.Color

/**
 * E-Ink 配置 - 标准版空实现
 * 
 * ⚠️ 注意：标准版不需要 E-Ink 优化，提供空实现以保持 API 兼容性
 */
object EInkConfig {
    object Colors {
        val Background = Color.Unspecified
        val Surface = Color.Unspecified
        val Primary = Color.Unspecified
        val OnPrimary = Color.Unspecified
        val OnBackground = Color.Unspecified
        val OnSurface = Color.Unspecified
    }
    
    const val ENABLE_ANIMATIONS = true
    
    enum class RefreshMode {
        FULL, PARTIAL, AUTO
    }
}

// standard/java/com/i/miniread/eink/EInkUtils.kt
package com.i.miniread.eink

/**
 * E-Ink 工具类 - 标准版空实现
 */
object EInkUtils {
    fun refreshScreen(mode: EInkConfig.RefreshMode = EInkConfig.RefreshMode.AUTO) {
        // 标准版不需要刷新控制
    }
    
    fun isEInkDevice(): Boolean = false
    
    fun supportsRefreshControl(): Boolean = false
}
```

```kotlin
// eink/java/com/i/miniread/eink/EInkConfig.kt
package com.i.miniread.eink

import androidx.compose.ui.graphics.Color

/**
 * E-Ink 配置 - E-Ink 版实现
 * 
 * 提供 E-Ink 设备的优化配置
 */
object EInkConfig {
    /**
     * E-Ink 优化的颜色方案
     * ⚠️ 使用纯黑白以最大化对比度
     */
    object Colors {
        val Background = Color.White
        val Surface = Color.White
        val Primary = Color.Black
        val OnPrimary = Color.White
        val OnBackground = Color.Black
        val OnSurface = Color.Black
    }
    
    /**
     * 是否启用动画
     * ⚠️ E-Ink 设备应禁用动画以避免残影
     */
    const val ENABLE_ANIMATIONS = false
    
    /**
     * 刷新模式
     */
    enum class RefreshMode {
        FULL,    // 全局刷新 - 清除残影但较慢
        PARTIAL, // 局部刷新 - 快速但可能有残影
        AUTO     // 自动选择
    }
}

// eink/java/com/i/miniread/eink/EInkUtils.kt
package com.i.miniread.eink

import android.os.Build
import android.util.Log

/**
 * E-Ink 工具类 - E-Ink 版实现
 * 
 * ⚠️ 注意：不同 E-Ink 设备的 API 可能不同
 * 当前实现仅提供框架，需要根据具体设备集成 SDK
 */
object EInkUtils {
    private const val TAG = "EInkUtils"
    
    /**
     * 刷新屏幕
     * 
     * ⚠️ 警告：当前为框架实现，需要集成设备特定的 SDK
     * 
     * @param mode 刷新模式
     */
    fun refreshScreen(mode: EInkConfig.RefreshMode = EInkConfig.RefreshMode.AUTO) {
        val deviceType = detectDeviceType()
        
        when (deviceType) {
            "ONYX" -> {
                // TODO: 集成 ONYX SDK
                Log.d(TAG, "ONYX refresh not implemented")
            }
            "Hisense" -> {
                // TODO: 集成海信 SDK
                Log.d(TAG, "Hisense refresh not implemented")
            }
            else -> {
                Log.w(TAG, "Unknown E-Ink device: $deviceType")
            }
        }
    }
    
    /**
     * 检测是否为 E-Ink 设备
     */
    fun isEInkDevice(): Boolean {
        return detectDeviceType() != null
    }
    
    /**
     * 检查设备是否支持刷新控制
     * 
     * ⚠️ 注意：即使检测到设备，也不保证 API 可用
     */
    fun supportsRefreshControl(): Boolean {
        val deviceType = detectDeviceType()
        // TODO: 实际检查 SDK 是否可用
        return deviceType != null
    }
    
    /**
     * 检测设备类型
     */
    private fun detectDeviceType(): String? {
        return when {
            Build.MANUFACTURER.equals("ONYX", ignoreCase = true) -> "ONYX"
            Build.MANUFACTURER.equals("Hisense", ignoreCase = true) -> "Hisense"
            // 添加更多设备检测
            else -> null
        }
    }
}
```

---

### 阶段 4: 资源文件迁移 (30 分钟)

#### 4.1 迁移颜色资源

```xml
<!-- standard/res/values/colors.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Material Design 3 彩色主题 -->
    <color name="flavor_primary">#6750A4</color>
    <color name="flavor_secondary">#625B71</color>
    <color name="flavor_tertiary">#7D5260</color>
    <color name="flavor_background">#FFFBFE</color>
    <color name="flavor_surface">#FFFBFE</color>
</resources>

<!-- eink/res/values/colors.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- 高对比度黑白主题 -->
    <color name="eink_black">#000000</color>
    <color name="eink_white">#FFFFFF</color>
    <color name="eink_dark_gray">#444444</color>
    <color name="eink_gray">#888888</color>
    <color name="eink_light_gray">#CCCCCC</color>
    
    <color name="flavor_primary">#000000</color>
    <color name="flavor_secondary">#444444</color>
    <color name="flavor_tertiary">#888888</color>
    <color name="flavor_background">#FFFFFF</color>
    <color name="flavor_surface">#FFFFFF</color>
</resources>
```

#### 4.2 迁移字符串资源

```xml
<!-- standard/res/values/strings.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- app_name 由 resValue 动态生成 -->
    <string name="flavor_description">完整功能的标准版本</string>
    <string name="flavor_tagline">轻量级 Miniflux 阅读器</string>
</resources>

<!-- eink/res/values/strings.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- app_name 由 resValue 动态生成 -->
    <string name="flavor_description">为电子墨水屏优化</string>
    <string name="flavor_tagline">墨水屏阅读体验</string>
</resources>
```

#### 4.3 迁移 CSS 文件

```bash
# 从 eink 分支提取优化的 CSS
git show origin/eink:app/src/main/assets/custom.css > app/src/main/assets/custom.css
```

编辑 `custom.css`:

```css
/* E-Ink 优化的 CSS */
:root {
    --fg-light: #000000;  /* 纯黑色文字 */
    --bg-light: #00000000; /* 透明背景 */
    --link-light: #1a73e8;
    --highlight-light: #ffeb3b;
}

body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto;
    font-size: 2.2rem;  /* E-Ink 版本增大 10% */
    line-height: 1.6;
    color: var(--fg-light);
    background-color: var(--bg-light);
}

/* 其他样式... */
```

---

### 阶段 5: 测试和验证 (60 分钟)

#### 5.1 构建测试

```bash
# 清理构建
./gradlew clean

# 构建所有变体
./gradlew assembleDebug
./gradlew assembleRelease

# 检查构建产物
ls -lh app/build/outputs/apk/**/*.apk
```

#### 5.2 安装测试

```bash
# 安装标准版
./gradlew installStandardDebug

# 安装 E-Ink 版（需要先卸载标准版或使用不同设备）
./gradlew installEinkDebug

# 或使用 adb 直接安装
adb install app/build/outputs/apk/standard/debug/app-standard-debug.apk
adb install app/build/outputs/apk/eink/debug/app-eink-debug.apk
```

#### 5.3 功能测试清单

**标准版测试**:
- [ ] 应用启动正常
- [ ] 登录功能正常
- [ ] 订阅源列表显示
- [ ] 文章列表显示
- [ ] 文章阅读
- [ ] 导航功能
- [ ] 刷新功能
- [ ] 深色模式切换
- [ ] 动态颜色（Android 12+）
- [ ] 所有按钮和交互正常

**E-Ink 版测试**:
- [ ] 应用启动正常
- [ ] 登录功能正常
- [ ] 订阅源列表显示（黑白）
- [ ] 文章列表显示（黑白）
- [ ] 文章阅读（黑白）
- [ ] 导航功能
- [ ] 刷新功能
- [ ] 音量键翻页功能
- [ ] WebView 焦点正常
- [ ] 滚动条已隐藏
- [ ] UI 尺寸适当（更紧凑）
- [ ] 文字对比度良好
- [ ] 无明显残影问题

#### 5.4 性能测试

```bash
# 启动时间测试
adb shell am start -W com.i.miniread/.MainActivity
adb shell am start -W com.i.miniread.eink/.MainActivity

# 内存使用测试
adb shell dumpsys meminfo com.i.miniread
adb shell dumpsys meminfo com.i.miniread.eink

# APK 大小对比
du -h app/build/outputs/apk/**/*.apk
```

#### 5.5 单元测试

```bash
# 运行所有测试
./gradlew test

# 运行特定 flavor 测试
./gradlew testStandardDebugUnitTest
./gradlew testEinkDebugUnitTest

# 查看测试报告
open app/build/reports/tests/testStandardDebugUnitTest/index.html
open app/build/reports/tests/testEinkDebugUnitTest/index.html
```

---

## 迁移后清理

### 1. 更新文档

```bash
# 更新 README.md
# 添加 Build Flavors 说明

# 更新开发文档
# 说明如何切换 flavor

# 更新发布文档
# 说明如何构建不同版本
```

### 2. 更新 CI/CD

```yaml
# .github/workflows/build.yml
jobs:
  build:
    strategy:
      matrix:
        flavor: [standard, eink]
        buildType: [debug, release]
    steps:
      - name: Build ${{ matrix.flavor }} ${{ matrix.buildType }}
        run: ./gradlew assemble${{ matrix.flavor }}${{ matrix.buildType }}
```

### 3. 清理备份分支

```bash
# 确认迁移成功后
# 可以删除备份分支（可选）
git branch -D backup-main-20251026
git branch -D backup-eink-20251026
```

---

## 常见问题和解决方案

### 问题 1: BuildConfig 未生成

**症状**: 编译错误，找不到 `BuildConfig.IS_EINK`

**解决**:
```kotlin
// 确保启用了 buildConfig
android {
    buildFeatures {
        buildConfig = true
    }
}

// 同步项目
// File → Sync Project with Gradle Files

// 重新构建
// Build → Rebuild Project
```

### 问题 2: 资源冲突

**症状**: 构建错误，资源重复定义

**解决**:
```kotlin
// 方案 1: 使用不同的资源名称
// main: main_color
// eink: eink_color

// 方案 2: 允许覆盖（推荐）
// eink/colors.xml 会覆盖 main/colors.xml 中的同名资源
```

### 问题 3: 依赖冲突

**症状**: 某些依赖在特定 flavor 中不可用

**解决**:
```kotlin
dependencies {
    // 标准版依赖
    "standardImplementation"(libs.google.services)
    
    // E-Ink 版依赖
    "einkImplementation"(libs.eink.sdk)
}
```

### 问题 4: Application ID 冲突

**症状**: 无法同时安装两个版本

**解决**:
```kotlin
// 确保 E-Ink 版有不同的 applicationIdSuffix
create("eink") {
    applicationIdSuffix = ".eink"  // ✅ 必须设置
}
```

### 问题 5: 代码在一个 flavor 中工作，另一个中不工作

**症状**: 功能在标准版正常，E-Ink 版异常

**调试**:
```kotlin
// 添加日志
Log.d("Flavor", "Current flavor: ${BuildConfig.FLAVOR_TYPE}")
Log.d("Flavor", "IS_EINK: ${BuildConfig.IS_EINK}")

// 检查条件判断
if (BuildConfig.IS_EINK) {
    Log.d("Flavor", "E-Ink specific code executed")
}
```

---

## 回滚计划

如果迁移失败需要回滚：

```bash
# 1. 切换回备份分支
git checkout backup-main-20251026

# 2. 创建新的工作分支
git checkout -b rollback-migration

# 3. 强制推送到远程（谨慎操作）
git push -f origin rollback-migration

# 4. 或者直接恢复 main 分支
git branch -D main
git checkout -b main backup-main-20251026
git push -f origin main  # ⚠️ 危险操作，需要团队同意
```

---

## 迁移检查清单

### 构建配置
- [ ] `flavorDimensions` 已定义
- [ ] `productFlavors` 已配置
- [ ] `buildConfigField` 已设置
- [ ] `resValue` 已配置
- [ ] `buildConfig` 功能已启用

### 代码迁移
- [ ] MainActivity.kt 已迁移
- [ ] Theme.kt 已迁移
- [ ] ArticleDetailScreen.kt 已迁移
- [ ] 其他 UI 屏幕已迁移
- [ ] EInkConfig.kt 已创建
- [ ] EInkUtils.kt 已创建
- [ ] 所有 BuildConfig 判断已添加
- [ ] 所有注释已添加

### 资源迁移
- [ ] colors.xml 已迁移
- [ ] strings.xml 已迁移
- [ ] dimens.xml 已创建（如需要）
- [ ] custom.css 已更新

### 测试
- [ ] 标准版可以构建
- [ ] E-Ink 版可以构建
- [ ] 标准版功能正常
- [ ] E-Ink 版功能正常
- [ ] 单元测试通过
- [ ] 性能可接受

### 文档和配置
- [ ] README 已更新
- [ ] 开发文档已更新
- [ ] CI/CD 已更新
- [ ] 版本号已更新

---

**文档版本**: 1.0  
**最后更新**: 2025-10-26  
**维护者**: GitHub Copilot
