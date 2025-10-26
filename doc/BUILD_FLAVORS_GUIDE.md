# MiniRead Build Flavors 开发指南

## 概述

MiniRead 项目使用 Android Build Flavors 功能来支持多个产品变体。目前支持两个版本：

- **Standard（标准版）**：面向普通智能手机和平板设备的全功能版本
- **E-Ink（电子墨水屏版）**：专为电子墨水屏设备（如墨水屏手机、阅读器）优化的版本

## 项目结构

```
app/src/
├── main/                          # 共享代码和资源
│   ├── java/                      # 所有 flavor 共享的代码
│   ├── res/                       # 共享的资源文件
│   └── AndroidManifest.xml        # 基础 Manifest
│
├── standard/                      # 标准版专属
│   ├── java/
│   │   └── com/i/miniread/eink/  # E-Ink 兼容层（空实现）
│   │       ├── EInkConfig.kt
│   │       └── EInkUtils.kt
│   └── res/
│       └── values/
│           ├── colors.xml         # 标准版颜色主题
│           └── strings.xml        # 标准版专属字符串
│
└── eink/                          # 墨水屏版专属
    ├── java/
    │   └── com/i/miniread/eink/  # E-Ink 优化实现
    │       ├── EInkConfig.kt     # E-Ink 配置
    │       └── EInkUtils.kt      # E-Ink 工具类
    └── res/
        └── values/
            ├── colors.xml         # E-Ink 优化的黑白配色
            └── strings.xml        # E-Ink 版专属字符串
```

## Build Variants 配置说明

### Flavor Dimensions

项目使用一个维度 `version`，包含两个 product flavors：

- `standard`：标准版
- `eink`：电子墨水屏版

### 构建变体 (Build Variants)

结合 Build Types（debug/release），将产生以下构建变体：

1. `standardDebug` - 标准版调试构建
2. `standardRelease` - 标准版发布构建
3. `einkDebug` - E-Ink 版调试构建
4. `einkRelease` - E-Ink 版发布构建

### BuildConfig 字段

每个 flavor 都会生成以下 BuildConfig 字段：

```kotlin
// 标准版
BuildConfig.FLAVOR_TYPE = "standard"
BuildConfig.IS_EINK = false

// E-Ink 版
BuildConfig.FLAVOR_TYPE = "eink"
BuildConfig.IS_EINK = true
```

## 开发最佳实践

### 1. 共享代码原则

- **共享优先**：将所有通用代码放在 `main/` 目录
- **最小差异**：只在 flavor 特定目录中放置必须不同的代码
- **接口统一**：使用相同的类名和方法签名，确保 API 兼容

### 2. 代码组织

#### 使用依赖注入或工厂模式

```kotlin
// 在 main/ 中定义接口
interface DisplayOptimizer {
    fun optimizeForDisplay()
    fun getAnimationSpec(): AnimationSpec<Float>
}

// 在各自 flavor 中实现
// standard/ 中
class StandardDisplayOptimizer : DisplayOptimizer { ... }

// eink/ 中
class EInkDisplayOptimizer : DisplayOptimizer { ... }
```

#### 使用 BuildConfig 判断

```kotlin
// 在 main/ 中的共享代码
if (BuildConfig.IS_EINK) {
    // E-Ink 特殊处理
} else {
    // 标准版处理
}
```

### 3. 资源管理

#### 颜色资源

- 使用 `flavor_` 前缀表示 flavor 特定的颜色
- E-Ink 版优先使用黑白灰色调，避免彩色
- 标准版可以使用全彩配色

#### 字符串资源

- 相同功能使用相同的资源 ID
- 仅在描述有差异时才在各自 flavor 中覆盖

#### 图标和图片

- E-Ink 版使用高对比度、黑白的图标
- 标准版使用彩色图标

### 4. E-Ink 版本优化要点

#### 动画优化
```kotlin
// 禁用或简化动画
val animationSpec = if (EInkConfig.DISABLE_ANIMATIONS) {
    snap()
} else {
    spring()
}
```

#### 颜色优化
```kotlin
// 使用 E-Ink 优化的颜色方案
val backgroundColor = if (BuildConfig.IS_EINK) {
    EInkConfig.Colors.Background
} else {
    MaterialTheme.colorScheme.background
}
```

#### 刷新控制
```kotlin
// 定期执行全屏刷新以清除残影
if (BuildConfig.IS_EINK && pageCount % EInkConfig.FULL_REFRESH_INTERVAL == 0) {
    EInkUtils.refreshScreen(fullRefresh = true)
}
```

### 5. 测试策略

#### 本地测试
```bash
# 测试标准版
./gradlew :app:testStandardDebugUnitTest

# 测试 E-Ink 版
./gradlew :app:testEinkDebugUnitTest
```

#### 构建测试
```bash
# 构建所有变体
./gradlew :app:assembleDebug

# 构建特定变体
./gradlew :app:assembleStandardDebug
./gradlew :app:assembleEinkRelease
```

## 开发工作流

### 切换 Build Variant

在 Android Studio 中：
1. 点击 View -> Tool Windows -> Build Variants
2. 在 Build Variants 面板中选择目标变体
3. 同步项目后即可运行该变体

### 添加新功能

#### 场景 1：所有 flavor 共享的功能
1. 在 `src/main/` 中实现
2. 确保兼容所有 flavor

#### 场景 2：flavor 特定功能
1. 在对应 flavor 目录中实现
2. 在其他 flavor 中提供兼容实现（可以是空实现）

#### 场景 3：部分差异的功能
1. 在 `src/main/` 中实现基础功能
2. 使用 `BuildConfig.IS_EINK` 等条件判断
3. 调用 flavor 特定的工具类

### 调试技巧

#### 查看当前 Flavor
```kotlin
Log.d("BuildFlavor", "Current flavor: ${BuildConfig.FLAVOR_TYPE}")
Log.d("BuildFlavor", "Is E-Ink: ${BuildConfig.IS_EINK}")
```

#### 条件断点
在调试时，可以设置条件断点只在特定 flavor 中触发：
```
BuildConfig.IS_EINK == true
```

## 发布流程

### 标准版发布
```bash
# 生成签名的标准版 APK
./gradlew :app:assembleStandardRelease

# 输出位置
app/build/outputs/apk/standard/release/app-standard-release.apk
```

### E-Ink 版发布
```bash
# 生成签名的 E-Ink 版 APK
./gradlew :app:assembleEinkRelease

# 输出位置
app/build/outputs/apk/eink/release/app-eink-release.apk
```

### 版本命名规范

- 标准版：`MiniRead-v1.0.0-standard.apk`
- E-Ink 版：`MiniRead-v1.0.0-eink.apk`

## 常见问题

### Q1: 如何判断当前运行的是哪个 flavor？
```kotlin
val isEInk = BuildConfig.IS_EINK
val flavorName = BuildConfig.FLAVOR_TYPE
```

### Q2: 如何在共享代码中处理 flavor 差异？
使用依赖注入、工厂模式或 BuildConfig 条件判断。

### Q3: 资源冲突如何解决？
Flavor 特定的资源会覆盖 main 中的同名资源。确保资源 ID 一致。

### Q4: 如何添加新的 flavor？
1. 在 `build.gradle.kts` 中添加新的 productFlavor
2. 创建对应的源集目录
3. 实现必要的 flavor 特定代码

### Q5: E-Ink 版如何集成设备 SDK？
在 `eink/` 目录下添加设备厂商的 SDK（如 ONYX SDK），并在 EInkUtils 中调用。

## E-Ink 设备适配建议

### 主流 E-Ink 设备 SDK

1. **ONYX BOOX**
   - SDK: ONYX SDK
   - 支持：局部刷新、全局刷新、手写笔交互

2. **iReader**
   - SDK: iReader SDK
   - 支持：刷新模式控制、前光调节

3. **Boyue**
   - SDK: Boyue SDK
   - 支持：屏幕刷新控制

### 集成示例

```kotlin
// 在 eink/java/com/i/miniread/eink/EInkUtils.kt 中
import android.onyx.sdk.api.device.epd.EpdController

fun refreshScreen(fullRefresh: Boolean = false) {
    try {
        if (fullRefresh) {
            // 使用 ONYX SDK 执行全屏刷新
            EpdController.invalidate(null, EpdController.MODE_GC16)
        } else {
            // 使用 ONYX SDK 执行快速刷新
            EpdController.invalidate(null, EpdController.MODE_A2)
        }
    } catch (e: Exception) {
        // 如果不是 ONYX 设备，忽略错误
        Log.w("EInkUtils", "Not running on supported E-Ink device", e)
    }
}
```

## 维护清单

定期检查以下内容：

- [ ] 两个 flavor 的 API 兼容性
- [ ] 资源文件的一致性
- [ ] BuildConfig 字段的正确性
- [ ] 构建脚本的有效性
- [ ] 测试覆盖率
- [ ] 文档更新

## 参考资料

- [Android Build Flavors 官方文档](https://developer.android.com/studio/build/build-variants)
- [Gradle Plugin DSL Reference](https://developer.android.com/reference/tools/gradle-api)
- [E-Ink 优化最佳实践](https://developer.android.com/guide/topics/ui/look-and-feel/themes)

---

**文档版本**: 1.0  
**最后更新**: 2025-10-26  
**维护者**: MiniRead 开发团队

