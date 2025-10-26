# 迁移现有代码到 Flavor 架构

本文档帮助你将现有代码迁移到新的 Build Flavors 架构。

## 概述

MiniRead 现在使用 Build Flavors 来支持多个产品变体。你可能需要调整一些现有代码以充分利用这个架构。

## 迁移步骤

### 1. 检查 BuildConfig 引用

**之前**：
```kotlin
// 旧代码可能直接检查包名或其他方式
if (packageName.contains("eink")) {
    // E-Ink 逻辑
}
```

**现在**：
```kotlin
import com.i.miniread.BuildConfig

if (BuildConfig.IS_EINK) {
    // E-Ink 逻辑
}
```

### 2. 使用 E-Ink 工具类

**之前**：
```kotlin
// 可能直接在代码中写 E-Ink 优化逻辑
val textColor = if (isEInkMode) Color.Black else MaterialTheme.colors.onSurface
```

**现在**：
```kotlin
import com.i.miniread.eink.EInkConfig

val textColor = EInkConfig.Colors.OnSurface
// 在 E-Ink flavor 中自动使用黑色，在标准 flavor 中使用主题色
```

### 3. 动画处理

**之前**：
```kotlin
// 可能使用全局开关
val animationEnabled = settings.enableAnimation

AnimatedVisibility(
    visible = isVisible,
    enter = if (animationEnabled) fadeIn() else EnterTransition.None
) { ... }
```

**现在**：
```kotlin
import com.i.miniread.BuildConfig

AnimatedVisibility(
    visible = isVisible,
    enter = if (BuildConfig.IS_EINK) {
        EnterTransition.None
    } else {
        fadeIn()
    }
) { ... }

// 或者使用配置类
val animationSpec = EInkConfig.getAnimationSpec<Float>()
```

### 4. 颜色主题

**之前**：
```kotlin
// 可能在代码中硬编码颜色
val backgroundColor = if (einkMode) Color.White else Color(0xFF6200EE)
```

**现在**：
```kotlin
// 在 colors.xml 中定义 flavor 特定颜色
val backgroundColor = colorResource(R.color.flavor_background)

// 或使用 EInkConfig
import com.i.miniread.eink.EInkConfig
val backgroundColor = EInkConfig.Colors.Background
```

### 5. 应用名称

**之前**：
```kotlin
// 在 strings.xml 中硬编码
<string name="app_name">MiniRead</string>
```

**现在**：
```kotlin
// 在 build.gradle.kts 中定义
resValue("string", "app_name", "MiniRead")          // standard
resValue("string", "app_name", "MiniRead E-Ink")    // eink
```

### 6. 屏幕刷新控制

**之前**：
```kotlin
// 可能直接调用设备 SDK
if (isEinkDevice) {
    // 调用某个特定设备的 SDK
    OnyxSDK.refreshScreen()
}
```

**现在**：
```kotlin
import com.i.miniread.eink.EInkUtils

// 统一的接口，在 flavor 中实现具体逻辑
EInkUtils.refreshScreen(fullRefresh = false)
```

## 重构示例

### 示例 1：阅读器界面

**之前**：
```kotlin
@Composable
fun ReaderScreen(entry: Entry) {
    val einkMode = remember { isEInkDevice() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (einkMode) Color.White else MaterialTheme.colors.background)
    ) {
        Text(
            text = entry.title,
            color = if (einkMode) Color.Black else MaterialTheme.colors.onBackground,
            fontSize = if (einkMode) 18.sp else 16.sp
        )
        
        // 内容
        HtmlContent(
            html = entry.content,
            enableAnimation = !einkMode
        )
    }
}
```

**现在**：
```kotlin
import com.i.miniread.eink.EInkConfig
import com.i.miniread.eink.EInkOptimized

@Composable
fun ReaderScreen(entry: Entry) {
    EInkOptimized {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EInkConfig.Colors.Background)
        ) {
            Text(
                text = entry.title,
                color = EInkConfig.Colors.OnSurface,
                fontSize = 18.sp  // 可以在各自 flavor 的 dimens.xml 中定义
            )
            
            HtmlContent(html = entry.content)
        }
    }
}
```

### 示例 2：列表滚动

**之前**：
```kotlin
@Composable
fun EntryList(entries: List<Entry>) {
    val einkMode = LocalEInkMode.current
    val scrollBehavior = if (einkMode) {
        // E-Ink 设备使用简单滚动
        ScrollBehavior.Simple
    } else {
        // 标准版使用平滑滚动
        ScrollBehavior.Smooth
    }
    
    LazyColumn(/* ... */) {
        items(entries) { entry ->
            EntryItem(
                entry = entry,
                animateItems = !einkMode
            )
        }
    }
}
```

**现在**：
```kotlin
import com.i.miniread.BuildConfig
import com.i.miniread.eink.EInkConfig

@Composable
fun EntryList(entries: List<Entry>) {
    LazyColumn(
        flingBehavior = if (EInkConfig.DISABLE_ANIMATIONS) {
            ScrollableDefaults.flingBehavior()
        } else {
            rememberSnapFlingBehavior(lazyListState = rememberLazyListState())
        }
    ) {
        items(entries) { entry ->
            EntryItem(entry = entry)
        }
    }
}
```

### 示例 3：网络图片加载

**之前**：
```kotlin
@Composable
fun FeedIcon(iconUrl: String) {
    val einkMode = LocalEInkMode.current
    
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(iconUrl)
            .crossfade(!einkMode)  // E-Ink 禁用淡入
            .build(),
        contentDescription = "Feed Icon"
    )
}
```

**现在**：
```kotlin
import com.i.miniread.eink.EInkConfig

@Composable
fun FeedIcon(iconUrl: String) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(iconUrl)
            .crossfade(!EInkConfig.DISABLE_ANIMATIONS)
            .build(),
        contentDescription = "Feed Icon"
    )
}
```

## 迁移检查清单

完成迁移后，请检查以下项目：

### 代码层面
- [ ] 移除硬编码的 E-Ink 判断逻辑
- [ ] 使用 `BuildConfig.IS_EINK` 替代自定义的设备检测
- [ ] 使用 `EInkConfig` 和 `EInkUtils` 替代直接的条件判断
- [ ] 确保所有 flavor 都能正常编译

### 资源层面
- [ ] 将 flavor 特定的颜色移到各自的 `colors.xml`
- [ ] 将 flavor 特定的字符串移到各自的 `strings.xml`
- [ ] 检查图标和图片是否需要 flavor 特定版本

### 测试层面
- [ ] 运行所有 flavor 的单元测试
- [ ] 手动测试每个 flavor 的核心功能
- [ ] 检查 E-Ink 版的显示效果

### 构建层面
- [ ] 尝试构建所有 Build Variants
- [ ] 检查 APK 大小是否合理
- [ ] 验证 Application ID 是否正确

## 常见问题

### Q: 现有代码中有很多 E-Ink 相关的 if-else，都要改吗？

A: 不一定。如果逻辑简单且集中，可以继续使用 `BuildConfig.IS_EINK`。但如果逻辑复杂或分散在多处，建议封装到 `EInkConfig` 或 `EInkUtils` 中。

### Q: 我应该把什么代码放在 flavor 目录中？

A: 只放置必须不同的代码。例如：
- ✅ E-Ink 设备 SDK 的调用代码
- ✅ Flavor 特定的配置类
- ❌ 只是有简单条件判断的共享代码（应该放在 main 中）

### Q: 如何处理第三方库的 E-Ink 优化？

A: 在各自 flavor 的代码中创建包装类：

```kotlin
// eink/java/.../ImageLoader.kt
object ImageLoader {
    fun load(url: String) {
        Coil.imageLoader(context).enqueue(
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(false)  // E-Ink 禁用动画
                .build()
        )
    }
}

// standard/java/.../ImageLoader.kt
object ImageLoader {
    fun load(url: String) {
        Coil.imageLoader(context).enqueue(
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)  // 标准版启用动画
                .build()
        )
    }
}
```

### Q: 迁移后如何确保向后兼容？

A: 
1. 保留 `BuildConfig.IS_EINK` 作为公共 API
2. 提供统一的工具类（`EInkConfig`, `EInkUtils`）
3. 在两个 flavor 中都实现相同的接口

## 获取帮助

如果在迁移过程中遇到问题：
1. 查看 [BUILD_FLAVORS_GUIDE.md](./BUILD_FLAVORS_GUIDE.md) 了解最佳实践
2. 参考 [QUICK_REFERENCE.md](./QUICK_REFERENCE.md) 获取代码示例
3. 检查现有 flavor 代码的实现

---

**文档版本**: 1.0  
**最后更新**: 2025-10-26

