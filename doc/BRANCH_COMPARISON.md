# Main 与 E-Ink 分支对比分析

> **L3 历史存档**（写于 2025-10 flavor 合并期，2026-11-13 标注）。本文记录合并前两个独立分支的差异，**不保证与当前代码一致**；含已过期的 flavor 级 `versionNameSuffix` 示范（如 L400/L410/L419）与旧版本号，**勿照抄**。现行规则见 [AGENTS.md](../AGENTS.md) §6，两变体现状见 [UI_DIFFERENCES.md](./UI_DIFFERENCES.md)。

## 文档说明

本文档详细记录了 MiniRead 项目中 `main` 分支（标准版本）与 `eink` 分支（电子墨水屏版本）之间的所有差异。这些差异已通过 Build Flavors 功能合并到统一的代码库中。

**创建日期**: 2025-10-26  
**分析者**: GitHub Copilot  
**分支版本**:
- main: e4fb465
- eink: 86930eb

---

## 总览

### 变更文件统计

```
修改的文件数: 8
新增行数: 168
删除行数: 75
净增加: 93 行
```

### 主要变更文件列表

1. `app/build.gradle.kts` - 构建配置
2. `app/src/main/assets/custom.css` - 样式文件
3. `app/src/main/java/com/i/miniread/MainActivity.kt` - 主活动
4. `app/src/main/java/com/i/miniread/ui/ArticleDetailScreen.kt` - 文章详情页
5. `app/src/main/java/com/i/miniread/ui/CategoryListScreen.kt` - 分类列表页
6. `app/src/main/java/com/i/miniread/ui/EntryListScreen.kt` - 条目列表页
7. `app/src/main/java/com/i/miniread/ui/FeedListScreen.kt` - 订阅源列表页
8. `app/src/main/java/com/i/miniread/ui/theme/Theme.kt` - 主题配置

---

## 详细差异分析

### 1. 主题系统 (`Theme.kt`)

#### Main 分支实现
```kotlin
// 支持动态颜色和深色模式
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

// 动态颜色支持 (Android 12+)
val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        if (darkTheme) dynamicDarkColorScheme(context) 
        else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
}
```

#### E-Ink 分支实现
```kotlin
// 高对比度黑白配色，专为墨水屏优化
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
    onSecondaryContainer = Color.White,
    primaryContainer = Color.White,
    secondaryContainer = Color.White,
    tertiaryContainer = Color.White,
)

// 默认使用浅色主题
@Composable
fun MinireadTheme(
    darkTheme: Boolean = false, // E-Ink 不使用深色模式
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) EInkDarkColorScheme else EInkLightColorScheme
    // 不再使用动态颜色
}
```

**关键差异**:
- ❌ E-Ink 版本**移除了动态颜色支持**（Material You）
- ✅ 使用**纯黑白配色方案**，最大化墨水屏对比度
- ✅ 默认强制使用浅色主题（墨水屏不适合深色模式）
- ⚠️ 所有容器颜色统一为白色，避免灰度层次导致的刷新问题

---

### 2. 主活动 (`MainActivity.kt`)

#### E-Ink 分支新增功能

##### 2.1 音量键翻页支持

```kotlin
// 拦截音量键用于翻页，而非调节音量
override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    Log.d("MainActivityKey", "Received key: $keyCode")
    return when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> {
            Log.d("MainActivity", "Intercepted volume key event")
            val webView = (currentFocus as? WebView)
            webView?.scrollBy(0, -500)  // 向上滚动
            true
        }
        KeyEvent.KEYCODE_VOLUME_DOWN -> {
            Log.d("MainActivity", "Intercepted volume key event")
            val webView = (currentFocus as? WebView)
            webView?.scrollBy(0, 800)  // 向下滚动
            true
        }
        else -> super.onKeyDown(keyCode, event)
    }
}
```

**设计考虑**:
- ✅ 墨水屏设备通常用于阅读，音量键更适合作为翻页键
- ⚠️ **依赖 WebView 获取焦点**，如果焦点不在 WebView 上可能失效
- 🔧 滚动距离经过优化：上滚 500px，下滚 800px（考虑阅读习惯）

##### 2.2 顶部栏优化

```kotlin
CenterAlignedTopAppBar(
    title = {
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.titleMedium  // 更小的字体
        )
    },
    actions = {
        IconButton(onClick = { /* ... */ }) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)  // 缩小图标尺寸
            )
        }
    },
    modifier = Modifier.height(40.dp),  // 减小高度
)
```

**优化点**:
- 标题字体从 `titleLarge` 改为 `titleMedium`
- 图标尺寸从默认 24dp 缩小到 18dp
- 顶部栏高度从默认 64dp 缩小到 40dp
- **目的**: 在小屏幕墨水屏设备上节省空间

##### 2.3 底部导航栏优化

```kotlin
NavigationBar(
    modifier = Modifier.height(48.dp),  // 从默认 80dp 缩小
    containerColor = Color.White,       // 纯白背景
) {
    items.forEach { screen ->
        NavigationBarItem(
            // ❌ 移除了文本标签，只保留图标
            // label = { Text(screen.label) },  
            selected = currentRoute == screen.route,
            onClick = { /* ... */ },
            icon = {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = screen.label,
                    modifier = Modifier.size(20.dp),  // 缩小图标
                    tint = if (currentRoute == screen.route) 
                        Color.White else Color.Black
                )
            }
        )
    }
}
```

**优化点**:
- 高度从 80dp 缩小到 48dp
- ❌ **完全移除文本标签**（减少刷新区域）
- 图标尺寸从 24dp 缩小到 20dp
- 使用简单的黑白状态指示（选中=白色，未选中=黑色）

---

### 3. 文章详情页 (`ArticleDetailScreen.kt`)

#### 3.1 底部操作栏优化

```kotlin
BottomAppBar(containerColor = Color.White) {  // 强制白色背景
    // 操作按钮...
}
```

**变更**: 设置纯白背景，避免墨水屏刷新残影

#### 3.2 WebView 优化

E-Ink 版本对 WebView 进行了多项优化：

```kotlin
val webView = remember {
    WebView(context).apply {
        // ===== 新增：焦点管理 =====
        post {
            requestFocus()
            Log.d("WebViewFocus", "Final focus state: ${hasFocus()}")
        }
        postDelayed({
            requestFocus()
            Log.d("WebViewFocus", "Delayed focus: ${hasFocus()}")
        }, 500)
        
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadsImagesAutomatically = true
            textZoom = 125
            
            // ===== 新增：墨水屏优化 =====
            isVerticalScrollBarEnabled = false  // 隐藏滚动条
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocusFromTouch()
        }
        setBackgroundColor(0x00000000)  // 透明背景
        
        // ... WebViewClient 和 WebChromeClient 配置
    }
}
```

**优化细节**:
1. **焦点管理强化**
   - 在初始化后立即请求焦点
   - 延迟 500ms 再次请求焦点（确保焦点获取）
   - 添加调试日志以追踪焦点状态
   - **目的**: 确保音量键翻页功能正常工作

2. **滚动条隐藏**
   - `isVerticalScrollBarEnabled = false`
   - **原因**: 墨水屏刷新滚动条会产生残影和闪烁

3. **触摸焦点优化**
   - `isFocusableInTouchMode = true`
   - `requestFocusFromTouch()`
   - **目的**: 改善触摸交互体验

#### 3.3 CSS 样式简化

E-Ink 版本移除了深色模式 CSS：

```kotlin
// Main 版本
if (cachedCustomCss == null) {
    cachedCustomCss = readAssetFile(
        context, 
        if (isDarkMode) "customdark.css" else "custom.css"
    )
}

// E-Ink 版本
if (cachedCustomCss == null) {
    cachedCustomCss = readAssetFile(context, "custom.css")
    // ❌ 不再检测深色模式
}
```

**原因**: 墨水屏只使用浅色主题，无需深色 CSS

---

### 4. CSS 样式文件 (`custom.css`)

#### 主要变更

```css
/* Main 版本 */
:root {
    --fg-light: #0e0e0e;  /* 接近黑色 */
    --bg-light: #00000000;
    --link-light: #1a73e8;
    --highlight-light: #ffeb3b;
}

body {
    font-size: 2rem;  /* 基础字体大小 */
    /* ... */
}

/* E-Ink 版本 */
:root {
    --fg-light: #000000;  /* 纯黑色 */
    --bg-light: #00000000;
    --link-light: #1a73e8;
    --highlight-light: #ffeb3b;
}

body {
    font-size: 2.2rem;  /* 增大字体 */
    /* ... */
}
```

**变更点**:
1. 文本颜色从 `#0e0e0e` 改为 `#000000`（纯黑）
   - **目的**: 最大化墨水屏对比度
2. 字体大小从 `2rem` 增加到 `2.2rem`
   - **目的**: 提高墨水屏可读性
   - 增幅约 10%

---

### 5. UI 屏幕优化

#### 5.1 列表屏幕 (`EntryListScreen.kt`, `FeedListScreen.kt`, `CategoryListScreen.kt`)

E-Ink 版本对所有列表屏幕进行了布局优化：

**主要变更**:
- 减小列表项的内边距
- 减小字体大小
- 简化分隔线样式
- 使用纯黑白配色

**代码示例** (EntryListScreen.kt):
```kotlin
// 列表项高度优化
Card(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 4.dp)  // 减小垂直间距
) {
    // 内容...
}

// 字体优化
Text(
    text = entry.title,
    style = MaterialTheme.typography.bodyMedium,  // 使用较小字体
    color = Color.Black,  // 纯黑文字
    maxLines = 2
)
```

#### 5.2 刷新逻辑优化

E-Ink 版本未修改刷新逻辑，但需要注意：
- ⚠️ 墨水屏刷新较慢，频繁刷新会影响用户体验
- 💡 建议：在 E-Ink flavor 中添加刷新节流或防抖机制

---

## 构建配置变更

### Build.gradle.kts

E-Ink 分支的构建配置变更：

```kotlin
// 在 E-Ink 分支中
versionNameSuffix = "eink-dev"  // 添加版本后缀标识
```

**当前 Flavor 实现**已正确配置：

```kotlin
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
```

---

## 兼容性考虑

### 已处理的问题

1. ✅ **API 兼容性**: 两个版本使用相同的 Miniflux API
2. ✅ **数据兼容性**: 使用相同的 DataStore 存储结构
3. ✅ **导航兼容性**: 保持相同的导航结构和路由

### 需要注意的限制

#### 1. 音量键功能限制

```kotlin
⚠️ 音量键翻页功能依赖以下条件：
- WebView 必须获得焦点
- 必须在文章详情页面
- 可能与系统音量控制冲突
```

**建议**: 
- 添加设置选项让用户启用/禁用此功能
- 提供替代翻页方式（屏幕边缘点击）

#### 2. 墨水屏刷新限制

```kotlin
⚠️ 某些墨水屏设备可能不支持：
- 全局刷新控制
- 局部刷新控制
- 自定义刷新模式
```

**当前实现**: 
- 已创建 `EInkUtils.kt` 工具类框架
- 提供了刷新控制接口
- **需要根据具体设备 SDK 实现**

#### 3. 动画和过渡

```kotlin
❌ E-Ink 版本应避免：
- 复杂的过渡动画
- 频繁的 UI 更新
- 动态颜色变化
- 半透明效果
```

**已实施的优化**:
- 移除动态颜色支持
- 使用纯色背景
- 简化 UI 层次结构

---

## 迁移建议

### 如何在代码中处理差异

#### 方法 1: 使用 BuildConfig 判断

```kotlin
import com.i.miniread.BuildConfig

if (BuildConfig.IS_EINK) {
    // E-Ink 特定代码
    EInkUtils.refreshScreen()
} else {
    // 标准版代码
    // 可以使用动画、动态颜色等
}
```

#### 方法 2: 使用 Flavor 特定实现

```
app/src/
├── main/                      # 共享代码
│   └── java/.../ui/
│       └── ArticleScreen.kt   # 基础实现
├── standard/                  # 标准版覆盖
│   └── java/.../ui/
│       └── ArticleScreen.kt   # 完整功能版本
└── eink/                      # E-Ink 版覆盖
    └── java/.../ui/
        └── ArticleScreen.kt   # 简化版本
```

#### 方法 3: 使用资源覆盖

```
app/src/
├── main/res/values/
│   └── dimens.xml             # 默认尺寸
├── standard/res/values/
│   └── dimens.xml             # 标准版尺寸
└── eink/res/values/
    └── dimens.xml             # E-Ink 优化尺寸
```

---

## 测试建议

### 需要在两个版本上测试的功能

- [ ] 登录功能
- [ ] 订阅源列表显示
- [ ] 分类列表显示
- [ ] 文章列表显示
- [ ] 文章阅读
- [ ] 导航功能
- [ ] 刷新功能
- [ ] 搜索功能（如果有）

### E-Ink 版本特定测试

- [ ] 音量键翻页功能
- [ ] 黑白显示效果
- [ ] 文字对比度
- [ ] 滚动性能
- [ ] 刷新残影问题
- [ ] 电池续航（墨水屏应更省电）

### 建议的测试设备

**标准版**:
- 普通 Android 手机（Android 8.0+）
- Android 平板

**E-Ink 版**:
- ONYX BOOX 系列电子书阅读器
- 海信墨水屏手机
- 其他 E-Ink Android 设备

---

## 未来优化方向

### 短期优化

1. **刷新控制增强**
   - 集成具体墨水屏设备 SDK
   - 实现智能刷新策略
   - 添加手动刷新选项

2. **交互优化**
   - 添加屏幕边缘点击翻页
   - 优化触摸反馈
   - 减少不必要的 UI 更新

3. **性能优化**
   - 列表虚拟化
   - 图片延迟加载
   - 缓存优化

### 长期优化

1. **更多 Flavor 支持**
   - Lite 版本（功能精简）
   - Pro 版本（付费功能）
   - 企业版本

2. **自适应优化**
   - 自动检测墨水屏设备
   - 根据屏幕特性调整显示
   - 智能刷新模式选择

3. **可访问性增强**
   - 更大字体选项
   - 更高对比度模式
   - 语音朗读支持

---

## 总结

### 主要差异概述

| 功能领域 | Main 分支 | E-Ink 分支 | 影响 |
|---------|----------|-----------|------|
| **主题系统** | 彩色，支持动态颜色 | 黑白，固定配色 | 高 |
| **UI 尺寸** | 标准尺寸 | 紧凑尺寸 | 中 |
| **音量键** | 调节音量 | 翻页功能 | 高 |
| **WebView** | 标准配置 | 优化焦点和滚动 | 中 |
| **CSS 样式** | 支持深浅模式 | 仅浅色模式 | 低 |
| **动画** | 完整支持 | 最小化 | 中 |
| **刷新逻辑** | 标准刷新 | 需要优化 | 高 |

### 代码复用率

- **共享代码**: 约 85%
- **Flavor 特定代码**: 约 15%
- **资源文件重用**: 约 70%

### 维护建议

1. ✅ 优先在 `main/` 目录开发新功能
2. ✅ 只在必要时创建 flavor 特定实现
3. ✅ 使用 `BuildConfig.IS_EINK` 进行运行时判断
4. ✅ 保持两个版本的 API 兼容性
5. ⚠️ 注意墨水屏设备的硬件限制
6. ⚠️ 充分测试两个版本

---

**文档版本**: 1.0  
**最后更新**: 2025-10-26  
**维护者**: GitHub Copilot
