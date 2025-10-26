# Build Flavors 快速参考

## 快速命令

### 构建命令
```bash
# 查看所有 Build Variants
./gradlew tasks --all | grep assemble

# 构建标准版 Debug
./gradlew assembleStandardDebug

# 构建标准版 Release
./gradlew assembleStandardRelease

# 构建 E-Ink 版 Debug
./gradlew assembleEinkDebug

# 构建 E-Ink 版 Release
./gradlew assembleEinkRelease

# 构建所有变体
./gradlew assembleDebug
./gradlew assembleRelease
```

### 安装命令
```bash
# 安装并运行标准版 Debug
./gradlew installStandardDebug
adb shell am start -n com.i.miniread/.MainActivity

# 安装并运行 E-Ink 版 Debug
./gradlew installEinkDebug
adb shell am start -n com.i.miniread.eink/.MainActivity
```

### 测试命令
```bash
# 运行标准版单元测试
./gradlew testStandardDebugUnitTest

# 运行 E-Ink 版单元测试
./gradlew testEinkDebugUnitTest

# 运行所有单元测试
./gradlew testDebugUnitTest
```

### 清理命令
```bash
# 清理构建产物
./gradlew clean

# 清理并重新构建
./gradlew clean assembleDebug
```

## 代码片段

### 判断当前 Flavor
```kotlin
import com.i.miniread.BuildConfig

val isEInk = BuildConfig.IS_EINK
val flavorType = BuildConfig.FLAVOR_TYPE

when (flavorType) {
    "standard" -> {
        // 标准版逻辑
    }
    "eink" -> {
        // E-Ink 版逻辑
    }
}
```

### 使用 E-Ink 工具类
```kotlin
import com.i.miniread.eink.EInkConfig
import com.i.miniread.eink.EInkUtils

// 检查是否是 E-Ink 设备
if (EInkUtils.isEInkDevice()) {
    // 应用 E-Ink 优化
    EInkUtils.optimizeTextRendering()
}

// 使用 E-Ink 颜色
val backgroundColor = EInkConfig.Colors.Background
val textColor = EInkConfig.Colors.OnSurface

// 获取适合的动画规格
val animationSpec = EInkConfig.getAnimationSpec<Float>()

// 触发屏幕刷新
EInkUtils.refreshScreen(fullRefresh = true)
```

### Compose UI 优化
```kotlin
import androidx.compose.runtime.Composable
import com.i.miniread.eink.EInkConfig
import com.i.miniread.eink.EInkOptimized

@Composable
fun MyScreen() {
    EInkOptimized {
        // 自动应用 E-Ink 优化的内容
        Text(
            text = "Hello E-Ink",
            color = EInkConfig.Colors.OnSurface
        )
    }
}
```

### 条件动画
```kotlin
import androidx.compose.animation.AnimatedVisibility
import com.i.miniread.BuildConfig

// 根据 flavor 决定是否启用动画
AnimatedVisibility(
    visible = isVisible,
    enter = if (BuildConfig.IS_EINK) {
        // E-Ink 版：无动画
        EnterTransition.None
    } else {
        // 标准版：淡入动画
        fadeIn()
    }
) {
    // 内容
}
```

## 目录结构速查

```
app/src/
├── main/              → 共享代码（所有 flavor）
├── standard/          → 标准版专属
│   ├── java/         → 标准版 Kotlin/Java 代码
│   └── res/          → 标准版资源
└── eink/             → E-Ink 版专属
    ├── java/         → E-Ink 版 Kotlin/Java 代码
    └── res/          → E-Ink 版资源
```

## 资源命名约定

### 颜色
- `flavor_*` - Flavor 特定颜色
- `eink_*` - E-Ink 专用颜色

### 字符串
- `flavor_*` - Flavor 特定字符串
- `eink_*` - E-Ink 专用字符串

### 尺寸
- `flavor_*` - Flavor 特定尺寸
- `eink_*` - E-Ink 专用尺寸

## 输出文件位置

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

## ApplicationId 对照表

| Flavor   | Build Type | Application ID              |
|----------|------------|-----------------------------|
| standard | debug      | com.i.miniread              |
| standard | release    | com.i.miniread              |
| eink     | debug      | com.i.miniread.eink         |
| eink     | release    | com.i.miniread.eink         |

## 常用资源访问

### 在代码中访问
```kotlin
// 字符串
val flavorDesc = getString(R.string.flavor_description)

// 颜色
val color = ContextCompat.getColor(context, R.color.flavor_primary)

// 在 Compose 中
val color = colorResource(R.color.flavor_primary)
val text = stringResource(R.string.flavor_description)
```

### 在 XML 中访问
```xml
<!-- 使用颜色 -->
<TextView
    android:textColor="@color/flavor_primary"
    android:text="@string/flavor_description" />
```

## 调试技巧

### 查看当前 Flavor 信息
```kotlin
Log.d("Flavor", """
    Flavor Type: ${BuildConfig.FLAVOR_TYPE}
    Is E-Ink: ${BuildConfig.IS_EINK}
    Version Name: ${BuildConfig.VERSION_NAME}
    Application ID: ${BuildConfig.APPLICATION_ID}
""".trimIndent())
```

### Android Studio Build Variants 面板
1. View -> Tool Windows -> Build Variants
2. 选择需要的构建变体
3. Sync Project

## 版本发布检查清单

- [ ] 更新版本号（versionCode 和 versionName）
- [ ] 检查签名配置
- [ ] 运行所有 flavor 的测试
- [ ] 构建所有 release 变体
- [ ] 测试安装和卸载
- [ ] 检查 APK 大小
- [ ] 更新 CHANGELOG

## 完整示例代码

查看实际使用示例：[FlavorUsageExample.kt](../app/src/main/java/com/i/miniread/example/FlavorUsageExample.kt)

该文件包含以下示例：
- ✅ 基础 Flavor 检测
- ✅ 自适应动画
- ✅ E-Ink 优化包装器使用
- ✅ 条件渲染
- ✅ E-Ink 设备功能
- ✅ 自适应颜色方案
- ✅ 构建时配置

## 需要帮助？

查看完整文档：[BUILD_FLAVORS_GUIDE.md](./BUILD_FLAVORS_GUIDE.md)

