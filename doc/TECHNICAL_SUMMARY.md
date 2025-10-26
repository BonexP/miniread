# 技术实现总结

## 文档说明

本文档提供 MiniRead 项目 Build Flavors 架构的完整技术实现说明，包括架构设计、代码组织、构建配置和最佳实践。

**创建日期**: 2025-10-26  
**技术栈**: Android, Kotlin, Jetpack Compose, Gradle  
**架构模式**: Build Flavors (Product Flavors)

---

## 架构概述

### 1. Build Flavors 架构

MiniRead 使用 Android Build Flavors 功能来支持两个产品变体：

```
┌─────────────────────────────────────────┐
│         MiniRead 项目                    │
├─────────────────────────────────────────┤
│                                          │
│  ┌──────────────┐    ┌──────────────┐  │
│  │   Standard   │    │    E-Ink     │  │
│  │   (标准版)    │    │  (墨水屏版)   │  │
│  └──────────────┘    └──────────────┘  │
│         │                    │          │
│         └────────┬───────────┘          │
│                  │                      │
│         ┌────────▼────────┐             │
│         │   Shared Code   │             │
│         │   (共享代码)     │             │
│         └─────────────────┘             │
│                                          │
└─────────────────────────────────────────┘
```

### 2. 核心设计原则

#### 原则 1: 共享优先 (Shared First)
```kotlin
// ✅ 推荐：共享代码 + 运行时判断
@Composable
fun ArticleScreen() {
    if (BuildConfig.IS_EINK) {
        // E-Ink 特定逻辑
    } else {
        // 标准版逻辑
    }
}

// ❌ 避免：创建重复的 flavor 特定文件
// standard/ArticleScreen.kt
// eink/ArticleScreen.kt
```

#### 原则 2: 最小差异 (Minimal Difference)
- 只在功能完全不同时才创建 flavor 特定实现
- 使用 BuildConfig 字段进行运行时判断
- 通过资源覆盖处理简单差异

#### 原则 3: API 一致性 (API Consistency)
- 两个 flavor 提供相同的公共 API
- 确保可以无缝切换 flavor
- 避免 flavor 特定的功能泄漏到公共 API

---

## 目录结构

### 完整的项目结构

```
miniread/
├── app/
│   ├── build.gradle.kts                    # Build Flavors 配置
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/                           # 共享代码（85%）
│       │   ├── java/com/i/miniread/
│       │   │   ├── MainActivity.kt
│       │   │   ├── SettingsFragment.kt
│       │   │   ├── network/
│       │   │   │   └── MinifluxApi.kt
│       │   │   ├── viewmodel/
│       │   │   │   └── MinifluxViewModel.kt
│       │   │   ├── ui/
│       │   │   │   ├── ArticleDetailScreen.kt
│       │   │   │   ├── CategoryListScreen.kt
│       │   │   │   ├── EntryListScreen.kt
│       │   │   │   ├── FeedListScreen.kt
│       │   │   │   ├── LoginScreen.kt
│       │   │   │   ├── SubFeedScreen.kt
│       │   │   │   ├── TodayEntryListScreen.kt
│       │   │   │   └── theme/
│       │   │   │       ├── Color.kt
│       │   │   │       ├── Theme.kt
│       │   │   │       └── Type.kt
│       │   │   ├── util/
│       │   │   │   ├── DataStoreManager.kt
│       │   │   │   └── DomainHelper.kt
│       │   │   └── example/
│       │   │       └── FlavorUsageExample.kt
│       │   ├── res/
│       │   │   ├── drawable/
│       │   │   ├── values/
│       │   │   │   ├── colors.xml
│       │   │   │   ├── strings.xml
│       │   │   │   └── themes.xml
│       │   │   ├── mipmap-*/                # App icons
│       │   │   └── xml/
│       │   ├── assets/
│       │   │   ├── custom.css               # E-Ink 优化的 CSS
│       │   │   ├── normalize.css
│       │   │   └── template.html
│       │   └── AndroidManifest.xml
│       │
│       ├── standard/                        # 标准版特定代码（7%）
│       │   ├── java/com/i/miniread/eink/
│       │   │   ├── EInkConfig.kt           # 空实现/兼容层
│       │   │   └── EInkUtils.kt            # 空实现/兼容层
│       │   └── res/values/
│       │       ├── colors.xml              # 彩色主题资源
│       │       └── strings.xml             # "MiniRead"
│       │
│       ├── eink/                            # E-Ink 版特定代码（8%）
│       │   ├── java/com/i/miniread/eink/
│       │   │   ├── EInkConfig.kt           # E-Ink 配置实现
│       │   │   └── EInkUtils.kt            # E-Ink 工具实现
│       │   └── res/values/
│       │       ├── colors.xml              # 黑白主题资源
│       │       └── strings.xml             # "MiniRead E-Ink"
│       │
│       ├── androidTest/                     # Instrumented tests
│       │   └── java/com/i/miniread/
│       │       └── ExampleInstrumentedTest.kt
│       └── test/                            # Unit tests
│           └── java/com/i/miniread/
│               └── ExampleUnitTest.kt
│
├── gradle/
│   └── libs.versions.toml                   # 依赖版本管理
├── build.gradle.kts                         # 项目级构建配置
├── settings.gradle.kts
├── gradle.properties
└── doc/                                     # 文档目录
    ├── README.md
    ├── BRANCH_COMPARISON.md
    ├── UI_DIFFERENCES.md
    ├── WORK_LOG.md
    ├── TECHNICAL_SUMMARY.md (当前文档)
    ├── DETAILED_MIGRATION.md
    ├── DEVICE_COMPATIBILITY.md
    ├── BUILD_FLAVORS_GUIDE.md
    ├── QUICK_REFERENCE.md
    ├── MIGRATION_GUIDE.md
    └── CHECKLIST.md
```

### 代码分布统计

| 目录 | 代码量 | 占比 | 说明 |
|-----|-------|------|------|
| `src/main/` | ~85% | 主体 | 所有 flavor 共享 |
| `src/standard/` | ~7% | 少量 | 兼容层实现 |
| `src/eink/` | ~8% | 少量 | E-Ink 优化 |

---

## 构建配置详解

### 1. build.gradle.kts 完整配置

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
}

val versionname by extra("dev")
val versioncode by extra("0.2.2")
val versionnumber by extra(26)

android {
    namespace = "com.i.miniread"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.i.miniread"
        minSdk = 25
        targetSdk = 34
        versionCode = versionnumber
        versionName = versioncode

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        versionNameSuffix = versionname
    }

    // ===== Build Flavors 配置 =====
    
    // 1. 定义 Flavor 维度
    flavorDimensions += "version"

    // 2. 定义产品 Flavors
    productFlavors {
        // 标准版
        create("standard") {
            dimension = "version"
            applicationIdSuffix = ""              // com.i.miniread
            versionNameSuffix = "-standard"       // 0.2.2-standard-dev

            // BuildConfig 字段
            buildConfigField("String", "FLAVOR_TYPE", "\"standard\"")
            buildConfigField("boolean", "IS_EINK", "false")

            // 动态资源值
            resValue("string", "app_name", "MiniRead")
        }

        // E-Ink 版
        create("eink") {
            dimension = "version"
            applicationIdSuffix = ".eink"         // com.i.miniread.eink
            versionNameSuffix = "-eink"           // 0.2.2-eink-dev

            // BuildConfig 字段
            buildConfigField("String", "FLAVOR_TYPE", "\"eink\"")
            buildConfigField("boolean", "IS_EINK", "true")

            // 动态资源值
            resValue("string", "app_name", "MiniRead E-Ink")
        }
    }

    // 3. 构建类型
    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = true
            isDefault = true
        }
        
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            isJniDebuggable = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // 4. 启用 BuildConfig 生成
    buildFeatures {
        buildConfig = true
        compose = true
    }

    // 5. 编译选项
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// 依赖配置
dependencies {
    // Android 核心
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // 导航
    implementation(libs.androidx.navigation.compose)

    // 网络
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // 数据存储
    implementation(libs.androidx.datastore.preferences)

    // 图片加载
    implementation(libs.coil.compose)

    // 测试
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // 调试
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
```

### 2. 构建变体 (Build Variants)

结合 Build Types 和 Product Flavors，生成 4 个构建变体：

| Build Variant | Application ID | 版本名 | 用途 |
|--------------|---------------|--------|------|
| **standardDebug** | `com.i.miniread` | `0.2.2-standard-dev` | 标准版开发 |
| **standardRelease** | `com.i.miniread` | `0.2.2-standard` | 标准版发布 |
| **einkDebug** | `com.i.miniread.eink` | `0.2.2-eink-dev` | E-Ink 版开发 |
| **einkRelease** | `com.i.miniread.eink` | `0.2.2-eink` | E-Ink 版发布 |

### 3. BuildConfig 字段使用

构建时自动生成 `BuildConfig.java`:

```kotlin
// 生成的 BuildConfig.java (标准版)
package com.i.miniread;

public final class BuildConfig {
    public static final boolean DEBUG = true;
    public static final String APPLICATION_ID = "com.i.miniread";
    public static final String BUILD_TYPE = "debug";
    public static final String FLAVOR = "standard";
    public static final int VERSION_CODE = 26;
    public static final String VERSION_NAME = "0.2.2-standard-dev";
    
    // 自定义字段
    public static final String FLAVOR_TYPE = "standard";
    public static final boolean IS_EINK = false;
}

// 生成的 BuildConfig.java (E-Ink 版)
package com.i.miniread;

public final class BuildConfig {
    public static final boolean DEBUG = true;
    public static final String APPLICATION_ID = "com.i.miniread.eink";
    public static final String BUILD_TYPE = "debug";
    public static final String FLAVOR = "eink";
    public static final int VERSION_CODE = 26;
    public static final String VERSION_NAME = "0.2.2-eink-dev";
    
    // 自定义字段
    public static final String FLAVOR_TYPE = "eink";
    public static final boolean IS_EINK = true;
}
```

在代码中使用：

```kotlin
import com.i.miniread.BuildConfig

// 方式 1: 使用 IS_EINK
if (BuildConfig.IS_EINK) {
    // E-Ink 特定代码
    applyEInkOptimizations()
} else {
    // 标准版代码
    applyStandardFeatures()
}

// 方式 2: 使用 FLAVOR_TYPE
when (BuildConfig.FLAVOR_TYPE) {
    "standard" -> configureStandardTheme()
    "eink" -> configureEInkTheme()
}

// 方式 3: 使用 FLAVOR
when (BuildConfig.FLAVOR) {
    "standard" -> { /* ... */ }
    "eink" -> { /* ... */ }
}
```

---

## 代码组织策略

### 1. Flavor 特定代码放置规则

#### 规则 1: 默认放在 main/

```kotlin
// ✅ 推荐：放在 src/main/
// src/main/java/com/i/miniread/ui/ArticleScreen.kt
@Composable
fun ArticleScreen() {
    // 使用 BuildConfig 判断
    val textSize = if (BuildConfig.IS_EINK) 2.2f else 2.0f
    
    // 共享的 UI 代码
    ArticleContent(textSize = textSize)
}
```

#### 规则 2: 完全不同的实现才分离

```kotlin
// 适用场景：两个 flavor 的实现完全不同
// 无法通过简单的 if/else 处理

// src/standard/java/com/i/miniread/eink/EInkUtils.kt
object EInkUtils {
    fun refreshScreen(mode: RefreshMode) {
        // 标准版：空实现
    }
}

// src/eink/java/com/i/miniread/eink/EInkUtils.kt
object EInkUtils {
    fun refreshScreen(mode: RefreshMode) {
        // E-Ink 版：实际实现
        when (mode) {
            RefreshMode.FULL -> triggerFullRefresh()
            RefreshMode.PARTIAL -> triggerPartialRefresh()
        }
    }
}
```

#### 规则 3: 使用资源覆盖

```xml
<!-- src/main/res/values/dimens.xml -->
<resources>
    <dimen name="toolbar_height">64dp</dimen>
    <dimen name="nav_bar_height">80dp</dimen>
</resources>

<!-- src/eink/res/values/dimens.xml -->
<resources>
    <dimen name="toolbar_height">40dp</dimen>
    <dimen name="nav_bar_height">48dp</dimen>
</resources>
```

### 2. 典型的代码模式

#### 模式 1: 条件执行

```kotlin
@Composable
fun MyScreen() {
    if (BuildConfig.IS_EINK) {
        // E-Ink 优化代码
        DisableAnimations()
        UseHighContrast()
    }
    
    // 共享代码
    ScreenContent()
}
```

#### 模式 2: 条件配置

```kotlin
@Composable
fun ConfigurableButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (BuildConfig.IS_EINK) 
                Color.White else MaterialTheme.colorScheme.primary,
            contentColor = if (BuildConfig.IS_EINK)
                Color.Black else MaterialTheme.colorScheme.onPrimary
        ),
        elevation = if (BuildConfig.IS_EINK)
            ButtonDefaults.buttonElevation(0.dp)
            else ButtonDefaults.buttonElevation()
    ) {
        Text("按钮")
    }
}
```

#### 模式 3: 策略模式

```kotlin
// 定义接口
interface DisplayOptimizer {
    fun optimizeForDisplay()
    fun shouldUseAnimation(): Boolean
}

// 工厂函数
fun createDisplayOptimizer(): DisplayOptimizer {
    return if (BuildConfig.IS_EINK) {
        EInkDisplayOptimizer()
    } else {
        StandardDisplayOptimizer()
    }
}

// 使用
val optimizer = remember { createDisplayOptimizer() }
if (optimizer.shouldUseAnimation()) {
    AnimatedContent { /* ... */ }
} else {
    StaticContent { /* ... */ }
}
```

#### 模式 4: Composable 包装器

```kotlin
// E-Ink 优化的 Composable 包装器
@Composable
fun EInkOptimized(
    content: @Composable () -> Unit
) {
    if (BuildConfig.IS_EINK) {
        // E-Ink 优化
        CompositionLocalProvider(
            LocalRippleTheme provides NoRippleTheme
        ) {
            DisposableEffect(Unit) {
                EInkUtils.refreshScreen(RefreshMode.PARTIAL)
                onDispose { }
            }
            content()
        }
    } else {
        content()
    }
}

// 使用
EInkOptimized {
    MyComplexUI()
}
```

---

## 资源管理

### 1. 资源优先级

资源合并优先级（从高到低）：

```
1. flavor + buildType (如 einkDebug)
2. buildType (如 debug)
3. flavor (如 eink)
4. main
5. 依赖库
```

### 2. 颜色资源

#### Standard Flavor 颜色

```xml
<!-- src/standard/res/values/colors.xml -->
<resources>
    <!-- Material Design 3 彩色主题 -->
    <color name="flavor_primary">#6750A4</color>
    <color name="flavor_secondary">#625B71</color>
    <color name="flavor_tertiary">#7D5260</color>
    <color name="flavor_background">#FFFBFE</color>
    <color name="flavor_surface">#FFFBFE</color>
    <color name="flavor_on_primary">#FFFFFF</color>
    <color name="flavor_on_background">#1C1B1F</color>
</resources>
```

#### E-Ink Flavor 颜色

```xml
<!-- src/eink/res/values/colors.xml -->
<resources>
    <!-- 高对比度黑白主题 -->
    <color name="eink_black">#000000</color>
    <color name="eink_white">#FFFFFF</color>
    <color name="eink_dark_gray">#444444</color>
    <color name="eink_gray">#888888</color>
    <color name="eink_light_gray">#CCCCCC</color>
    
    <!-- 映射到 flavor 资源 -->
    <color name="flavor_primary">#000000</color>
    <color name="flavor_secondary">#444444</color>
    <color name="flavor_tertiary">#888888</color>
    <color name="flavor_background">#FFFFFF</color>
    <color name="flavor_surface">#FFFFFF</color>
    <color name="flavor_on_primary">#FFFFFF</color>
    <color name="flavor_on_background">#000000</color>
</resources>
```

### 3. 尺寸资源

```xml
<!-- src/main/res/values/dimens.xml -->
<resources>
    <!-- 默认尺寸 -->
    <dimen name="toolbar_height">64dp</dimen>
    <dimen name="nav_bar_height">80dp</dimen>
    <dimen name="list_item_padding">16dp</dimen>
    <dimen name="list_item_spacing">8dp</dimen>
</resources>

<!-- src/eink/res/values/dimens.xml -->
<resources>
    <!-- E-Ink 优化尺寸 -->
    <dimen name="toolbar_height">40dp</dimen>
    <dimen name="nav_bar_height">48dp</dimen>
    <dimen name="list_item_padding">12dp</dimen>
    <dimen name="list_item_spacing">4dp</dimen>
</resources>
```

### 4. 字符串资源

```xml
<!-- src/standard/res/values/strings.xml -->
<resources>
    <!-- app_name 由 resValue 动态生成 -->
    <string name="flavor_description">完整功能的标准版本</string>
</resources>

<!-- src/eink/res/values/strings.xml -->
<resources>
    <!-- app_name 由 resValue 动态生成 -->
    <string name="flavor_description">为电子墨水屏优化的版本</string>
</resources>
```

---

## 依赖管理

### 1. 共享依赖

所有 flavor 共享的依赖：

```kotlin
dependencies {
    // 所有 flavor 都需要的依赖
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.bom)
    implementation(libs.retrofit)
    // ...
}
```

### 2. Flavor 特定依赖

```kotlin
dependencies {
    // 标准版特定依赖
    "standardImplementation"(libs.google.play.services)
    "standardImplementation"(libs.firebase.analytics)
    
    // E-Ink 版特定依赖
    "einkImplementation"(libs.onyx.sdk)
    "einkImplementation"(libs.hisense.sdk)
}
```

### 3. Build Type 特定依赖

```kotlin
dependencies {
    // 调试版依赖
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.leakcanary)
    
    // 发布版依赖
    releaseImplementation(libs.androidx.profileinstaller)
}
```

---

## 测试策略

### 1. 单元测试

```kotlin
// src/test/java/com/i/miniread/FlavorTest.kt
class FlavorTest {
    @Test
    fun `test buildconfig fields`() {
        // 测试 BuildConfig 字段存在
        assertNotNull(BuildConfig.FLAVOR_TYPE)
        assertNotNull(BuildConfig.IS_EINK)
        
        // 验证值的正确性（需要为每个 flavor 运行）
        assertTrue(
            BuildConfig.FLAVOR_TYPE == "standard" ||
            BuildConfig.FLAVOR_TYPE == "eink"
        )
    }
}
```

### 2. Flavor 特定测试

```kotlin
// src/standardTest/java/com/i/miniread/StandardFeatureTest.kt
class StandardFeatureTest {
    @Test
    fun `test standard flavor features`() {
        assertFalse(BuildConfig.IS_EINK)
        assertEquals("standard", BuildConfig.FLAVOR_TYPE)
        
        // 测试标准版特定功能
    }
}

// src/einkTest/java/com/i/miniread/EInkFeatureTest.kt
class EInkFeatureTest {
    @Test
    fun `test eink flavor features`() {
        assertTrue(BuildConfig.IS_EINK)
        assertEquals("eink", BuildConfig.FLAVOR_TYPE)
        
        // 测试 E-Ink 版特定功能
    }
}
```

### 3. Instrumented 测试

```kotlin
// src/androidTest/java/com/i/miniread/FlavorInstrumentedTest.kt
@RunWith(AndroidJUnit4::class)
class FlavorInstrumentedTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testFlavorSpecificUI() {
        composeTestRule.setContent {
            MinireadTheme {
                MainScreen()
            }
        }
        
        if (BuildConfig.IS_EINK) {
            // 验证 E-Ink UI
            composeTestRule
                .onNodeWithText("MiniRead E-Ink")
                .assertExists()
        } else {
            // 验证标准 UI
            composeTestRule
                .onNodeWithText("MiniRead")
                .assertExists()
        }
    }
}
```

### 4. 测试命令

```bash
# 运行所有测试
./gradlew test

# 运行特定 flavor 的测试
./gradlew testStandardDebugUnitTest
./gradlew testEinkDebugUnitTest

# 运行 Instrumented 测试
./gradlew connectedStandardDebugAndroidTest
./gradlew connectedEinkDebugAndroidTest
```

---

## 最佳实践

### 1. 代码最佳实践

#### ✅ DO (推荐)

```kotlin
// 1. 使用 BuildConfig 进行运行时判断
if (BuildConfig.IS_EINK) {
    // E-Ink 代码
}

// 2. 使用资源覆盖处理简单差异
val height = dimensionResource(R.dimen.toolbar_height)

// 3. 创建清晰的接口
interface PlatformAdapter {
    fun optimize()
}

// 4. 添加详细注释
if (BuildConfig.IS_EINK) {
    // ⚠️ 注意：E-Ink 设备不支持动画
    // 使用静态内容代替
    StaticContent()
}

// 5. 使用 remember 缓存 BuildConfig 检查
@Composable
fun MyScreen() {
    val isEInk = remember { BuildConfig.IS_EINK }
    // 使用 isEInk...
}
```

#### ❌ DON'T (避免)

```kotlin
// 1. 避免硬编码 flavor 名称
if (BuildConfig.FLAVOR == "eink") { /* ... */ }  // ❌
if (BuildConfig.IS_EINK) { /* ... */ }  // ✅

// 2. 避免重复的 flavor 特定文件
// standard/MainActivity.kt  // ❌
// eink/MainActivity.kt      // ❌
// main/MainActivity.kt      // ✅ 使用 BuildConfig 判断

// 3. 避免在公共 API 中暴露 flavor 细节
fun processData(isEInk: Boolean) { /* ... */ }  // ❌
fun processData() {  // ✅
    val isEInk = BuildConfig.IS_EINK
    // ...
}

// 4. 避免在每次使用时检查 BuildConfig
@Composable
fun MyList() {
    items.forEach { item ->
        if (BuildConfig.IS_EINK) { /* ... */ }  // ❌ 每次循环都检查
    }
}

// 改进：
@Composable
fun MyList() {
    val isEInk = remember { BuildConfig.IS_EINK }  // ✅ 只检查一次
    items.forEach { item ->
        if (isEInk) { /* ... */ }
    }
}
```

### 2. 性能最佳实践

#### Compose 优化

```kotlin
// 1. 缓存 BuildConfig 检查
@Composable
fun OptimizedScreen() {
    val isEInk = remember { BuildConfig.IS_EINK }
    val colorScheme = remember(isEInk) {
        if (isEInk) eInkColors else standardColors
    }
    
    // 使用缓存的值
}

// 2. 避免不必要的重组
@Composable
fun ListItem(item: Item) {
    // ✅ 在外层检查
    val textColor = if (BuildConfig.IS_EINK) Color.Black 
                    else MaterialTheme.colorScheme.onSurface
    
    Text(
        text = item.title,
        color = textColor
    )
}
```

#### 资源加载优化

```kotlin
// 1. 延迟加载 flavor 特定资源
val heavyResource by lazy {
    if (BuildConfig.IS_EINK) {
        loadEInkResource()
    } else {
        loadStandardResource()
    }
}

// 2. 使用资源系统自动选择
val drawable = ContextCompat.getDrawable(context, R.drawable.icon)
// 系统会自动选择正确的 flavor 资源
```

### 3. 维护最佳实践

#### 版本控制

```kotlin
// 1. 使用语义化版本
versionName = "0.2.2-${flavor}-${buildType}"

// 2. 每个 flavor 使用不同的 Application ID
standard: com.i.miniread
eink: com.i.miniread.eink
```

#### 文档

```kotlin
/**
 * E-Ink 优化的 WebView 配置
 * 
 * ⚠️ 注意事项：
 * - 隐藏滚动条以避免残影
 * - 强制请求焦点以支持音量键翻页
 * - 某些设备可能不支持所有优化
 * 
 * @param context Android Context
 * @return 配置好的 WebView
 */
fun createEInkWebView(context: Context): WebView {
    // 实现...
}
```

---

## 构建和发布

### 1. 本地构建

```bash
# 构建所有变体
./gradlew build

# 构建特定变体
./gradlew assembleStandardDebug
./gradlew assembleStandardRelease
./gradlew assembleEinkDebug
./gradlew assembleEinkRelease

# 安装到设备
./gradlew installStandardDebug
./gradlew installEinkDebug
```

### 2. 生成的 APK

```
app/build/outputs/apk/
├── standard/
│   ├── debug/
│   │   └── app-standard-debug.apk
│   └── release/
│       └── app-standard-release.apk
└── eink/
    ├── debug/
    │   └── app-eink-debug.apk
    └── release/
        └── app-eink-release.apk
```

### 3. 签名配置

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### 4. CI/CD 集成

```yaml
# .github/workflows/build.yml
name: Build All Flavors

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      # 构建所有变体
      - name: Build Standard Debug
        run: ./gradlew assembleStandardDebug
      
      - name: Build Standard Release
        run: ./gradlew assembleStandardRelease
      
      - name: Build E-Ink Debug
        run: ./gradlew assembleEinkDebug
      
      - name: Build E-Ink Release
        run: ./gradlew assembleEinkRelease
      
      # 上传构建产物
      - uses: actions/upload-artifact@v3
        with:
          name: apks
          path: app/build/outputs/apk/**/*.apk
```

---

## 故障排查

### 常见问题

#### 1. BuildConfig 未生成

**症状**: 无法导入 `BuildConfig` 类

**解决方案**:
```kotlin
// 1. 确保启用了 buildConfig
android {
    buildFeatures {
        buildConfig = true  // ✅ 必须启用
    }
}

// 2. 同步项目
File → Sync Project with Gradle Files

// 3. 重新构建
Build → Rebuild Project
```

#### 2. 资源冲突

**症状**: 资源文件合并错误

**解决方案**:
```kotlin
// 使用不同的资源名称
// main/res/values/colors.xml
<color name="main_primary">#6750A4</color>

// eink/res/values/colors.xml
<color name="eink_primary">#000000</color>

// 或使用相同名称（允许覆盖）
// eink 会覆盖 main 的资源
```

#### 3. Flavor 切换后代码报错

**症状**: 切换 Build Variant 后代码显示错误

**解决方案**:
```bash
# 1. 清理项目
./gradlew clean

# 2. 同步项目
File → Sync Project with Gradle Files

# 3. 重新构建
Build → Rebuild Project

# 4. 重启 Android Studio（如果还有问题）
```

---

## 总结

### 架构优势

1. ✅ **代码复用**: 85% 的代码在两个 flavor 间共享
2. ✅ **统一维护**: 单一代码库，减少维护成本
3. ✅ **灵活配置**: 通过 BuildConfig 和资源系统灵活定制
4. ✅ **类型安全**: 编译时确定 flavor 特定行为
5. ✅ **易于扩展**: 可以轻松添加新的 flavor

### 关键要点

1. **优先共享代码**，只在必要时分离
2. **使用 BuildConfig** 进行运行时判断
3. **利用资源系统**处理简单差异
4. **保持 API 一致性**，避免 flavor 泄漏
5. **充分测试**每个 flavor
6. **详细文档**说明 flavor 特定行为

---

**文档版本**: 1.0  
**最后更新**: 2025-10-26  
**维护者**: GitHub Copilot
