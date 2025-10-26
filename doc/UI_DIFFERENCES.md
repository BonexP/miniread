# UI/界面布局/配色详细对比文档

## 文档说明

本文档专注于 MiniRead 项目中标准版（Main）与电子墨水屏版（E-Ink）在用户界面、布局和配色方面的详细对比。

**创建日期**: 2025-10-26  
**分析范围**: UI 组件、布局、颜色、字体、间距、交互

---

## 配色方案对比

### 标准版配色 (Standard)

#### 主色调
```kotlin
// 基于 Material Design 3
private val LightColorScheme = lightColorScheme(
    primary = Purple40,           // #6750A4 (紫色)
    secondary = PurpleGrey40,     // #625B71 (紫灰)
    tertiary = Pink40,            // #7D5260 (粉色)
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,           // #D0BCFF (浅紫)
    secondary = PurpleGrey80,     // #CCC2DC (浅紫灰)
    tertiary = Pink80,            // #EFB8C8 (浅粉)
)
```

#### 特性
- ✅ 支持**动态颜色** (Material You)
- ✅ 支持**深色/浅色模式**自动切换
- ✅ 丰富的颜色层次
- ✅ 符合现代 Android 设计规范

#### 视觉效果
```
背景: 白色或深灰色（根据主题）
文字: 深灰色或浅灰色（根据主题）
强调: 彩色（紫色系）
链接: 蓝色 (#1a73e8)
```

### 电子墨水屏版配色 (E-Ink)

#### 主色调
```kotlin
// 高对比度黑白配色
private val EInkLightColorScheme = lightColorScheme(
    primary = Color.Black,        // #000000 (纯黑)
    secondary = Color.DarkGray,   // #444444 (深灰)
    tertiary = Color.Gray,        // #888888 (灰色)
    background = Color.White,     // #FFFFFF (纯白)
    surface = Color.White,        // #FFFFFF (纯白)
    onPrimary = Color.White,      // #FFFFFF (纯白)
    onSecondary = Color.White,    // #FFFFFF (纯白)
    onTertiary = Color.White,     // #FFFFFF (纯白)
    onBackground = Color.Black,   // #000000 (纯黑)
    onSurface = Color.Black,      // #000000 (纯黑)
    primaryContainer = Color.White,
    secondaryContainer = Color.White,
    tertiaryContainer = Color.White,
)
```

#### 特性
- ❌ **不支持**动态颜色
- ❌ **不支持**深色模式
- ✅ 纯黑白配色，**最大化对比度**
- ✅ 避免灰度层次，**减少刷新残影**

#### 视觉效果
```
背景: 纯白色 (#FFFFFF)
文字: 纯黑色 (#000000)
强调: 黑色（无彩色）
链接: 黑色（可通过下划线区分）
分隔线: 浅灰色
```

### 配色对比表

| 元素 | 标准版 (Light) | 标准版 (Dark) | E-Ink 版 |
|-----|--------------|--------------|---------|
| **背景色** | #FFFBFE (浅灰白) | #1C1B1F (深灰黑) | #FFFFFF (纯白) |
| **表面色** | #FFFBFE (浅灰白) | #1C1B1F (深灰黑) | #FFFFFF (纯白) |
| **主要文字** | #1C1B1F (深灰黑) | #E6E1E5 (浅灰) | #000000 (纯黑) |
| **次要文字** | #49454F (中灰) | #CAC4D0 (中灰) | #444444 (深灰) |
| **主色调** | #6750A4 (紫色) | #D0BCFF (浅紫) | #000000 (黑色) |
| **强调色** | #7D5260 (粉色) | #EFB8C8 (浅粉) | #888888 (灰色) |
| **链接色** | #1a73e8 (蓝色) | #8AB4F8 (浅蓝) | #000000 (黑色) |
| **分隔线** | #E7E0EC (浅灰) | #49454F (深灰) | #CCCCCC (浅灰) |

---

## 布局尺寸对比

### 导航栏尺寸

#### 顶部应用栏 (Top App Bar)

**标准版**:
```kotlin
CenterAlignedTopAppBar(
    title = { 
        Text(
            text = stringResource(id = R.string.app_name),
            // 默认使用 titleLarge: 22sp
        )
    },
    actions = {
        IconButton(onClick = { /* ... */ }) {
            Icon(
                imageVector = Icons.Default.Refresh,
                // 默认图标大小: 24dp
            )
        }
    }
    // 默认高度: 64dp
)
```

**E-Ink 版**:
```kotlin
CenterAlignedTopAppBar(
    title = { 
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.titleMedium  // 16sp
        )
    },
    actions = {
        IconButton(onClick = { /* ... */ }) {
            Icon(
                imageVector = Icons.Default.Refresh,
                modifier = Modifier.size(18.dp)  // 缩小到 18dp
            )
        }
    },
    modifier = Modifier.height(40.dp)  // 缩小到 40dp
)
```

**对比**:
| 元素 | 标准版 | E-Ink 版 | 减少 |
|-----|-------|---------|------|
| 标题字体 | 22sp | 16sp | -27% |
| 图标大小 | 24dp | 18dp | -25% |
| 栏高度 | 64dp | 40dp | -37.5% |

#### 底部导航栏 (Bottom Navigation Bar)

**标准版**:
```kotlin
NavigationBar {
    NavigationBarItem(
        label = { 
            Text(screen.label)  // 显示文本标签
            // 默认字体: 12sp
        },
        icon = { 
            Icon(
                imageVector = Icons.Default.Menu,
                // 默认大小: 24dp
            )
        }
    )
}
// 默认高度: 80dp
```

**E-Ink 版**:
```kotlin
NavigationBar(
    modifier = Modifier.height(48.dp),  // 缩小高度
    containerColor = Color.White,        // 强制白色背景
) {
    NavigationBarItem(
        // ❌ 移除了文本标签
        icon = { 
            Icon(
                imageVector = Icons.Default.Menu,
                modifier = Modifier.size(20.dp),  // 缩小图标
                tint = if (selected) Color.White else Color.Black
            )
        }
    )
}
```

**对比**:
| 元素 | 标准版 | E-Ink 版 | 变化 |
|-----|-------|---------|------|
| 栏高度 | 80dp | 48dp | -40% |
| 图标大小 | 24dp | 20dp | -17% |
| 文本标签 | 显示 (12sp) | ❌ 隐藏 | 完全移除 |
| 背景色 | 主题色 | 纯白 | 固定色 |
| 选中指示 | 填充色 | 图标反色 | 简化 |

### 列表布局

#### 订阅源列表 (Feed List)

**标准版**:
```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp)  // 标准内边距
) {
    items(feeds) { feed ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)  // 垂直间距
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp),  // 内部填充
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = feed.title,
                    style = MaterialTheme.typography.titleMedium,  // 16sp
                    maxLines = 1
                )
                Text(
                    text = "${feed.unreadCount}",
                    style = MaterialTheme.typography.bodyMedium,  // 14sp
                    color = MaterialTheme.colorScheme.primary  // 彩色
                )
            }
        }
    }
}
```

**E-Ink 版**:
```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(12.dp)  // 减小内边距
) {
    items(feeds) { feed ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),  // 减小垂直间距
            colors = CardDefaults.cardColors(
                containerColor = Color.White  // 强制白色
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp),  // 减小内部填充
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = feed.title,
                    style = MaterialTheme.typography.bodyMedium,  // 14sp
                    maxLines = 1,
                    color = Color.Black  // 纯黑文字
                )
                Text(
                    text = "${feed.unreadCount}",
                    style = MaterialTheme.typography.bodySmall,  // 12sp
                    color = Color.Black  // 黑色而非彩色
                )
            }
        }
    }
}
```

**对比**:
| 元素 | 标准版 | E-Ink 版 | 变化 |
|-----|-------|---------|------|
| 列表外边距 | 16dp | 12dp | -25% |
| 列表项垂直间距 | 8dp | 4dp | -50% |
| 列表项内边距 | 16dp | 12dp | -25% |
| 标题字体 | 16sp | 14sp | -12.5% |
| 计数字体 | 14sp | 12sp | -14.3% |
| 计数颜色 | 彩色 | 黑色 | 去除色彩 |

#### 文章列表 (Entry List)

**标准版**:
```kotlin
LazyColumn {
    items(entries) { entry ->
        ListItem(
            headlineContent = {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,  // 16sp
                    maxLines = 2
                )
            },
            supportingContent = {
                Text(
                    text = "${entry.feed?.title} • ${formatDate(entry.publishedAt)}",
                    style = MaterialTheme.typography.bodySmall,  // 12sp
                    color = MaterialTheme.colorScheme.onSurfaceVariant  // 彩色
                )
            },
            leadingContent = {
                Icon(
                    imageVector = if (entry.status == "read") 
                        Icons.Default.CheckCircle 
                        else Icons.Default.Circle,
                    contentDescription = null,
                    tint = if (entry.status == "read") 
                        MaterialTheme.colorScheme.primary  // 彩色
                        else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* ... */ }
        )
        HorizontalDivider()  // Material 3 分隔线
    }
}
```

**E-Ink 版**:
```kotlin
LazyColumn {
    items(entries) { entry ->
        ListItem(
            headlineContent = {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,  // 14sp
                    maxLines = 2,
                    color = Color.Black
                )
            },
            supportingContent = {
                Text(
                    text = "${entry.feed?.title} • ${formatDate(entry.publishedAt)}",
                    style = MaterialTheme.typography.bodySmall,  // 12sp
                    color = Color.DarkGray  // 深灰而非彩色
                )
            },
            leadingContent = {
                Icon(
                    imageVector = if (entry.status == "read") 
                        Icons.Default.CheckCircle 
                        else Icons.Default.Circle,
                    contentDescription = null,
                    tint = Color.Black,  // 统一黑色
                    modifier = Modifier.size(16.dp)  // 缩小图标
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* ... */ }
        )
        Divider(color = Color.LightGray, thickness = 1.dp)  // 简化分隔线
    }
}
```

**对比**:
| 元素 | 标准版 | E-Ink 版 | 变化 |
|-----|-------|---------|------|
| 标题字体 | 16sp | 14sp | -12.5% |
| 副标题字体 | 12sp | 12sp | 相同 |
| 标题颜色 | 主题色 | 纯黑 | 去色彩 |
| 副标题颜色 | 主题变体 | 深灰 | 去色彩 |
| 图标大小 | 20dp | 16dp | -20% |
| 图标颜色 | 状态色 | 纯黑 | 去色彩 |
| 分隔线样式 | Material 3 | 简单线条 | 简化 |

### 文章详情页布局

#### 底部操作栏 (Bottom Action Bar)

**标准版**:
```kotlin
BottomAppBar {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 多个操作按钮
        IconButton(onClick = { /* ... */ }) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "标记为已读",
                // 默认大小: 24dp
                tint = MaterialTheme.colorScheme.primary  // 彩色
            )
        }
        // ... 其他按钮
    }
}
// 默认高度: 80dp
```

**E-Ink 版**:
```kotlin
BottomAppBar(
    containerColor = Color.White  // 强制白色背景
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = { /* ... */ }) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,  // E-Ink 不需要详细描述
                modifier = Modifier.size(22.dp),  // 略微缩小
                tint = Color.Black  // 纯黑色
            )
        }
        // ... 其他按钮
    }
}
// 高度保持默认 80dp（考虑到触摸目标大小）
```

**对比**:
| 元素 | 标准版 | E-Ink 版 | 变化 |
|-----|-------|---------|------|
| 背景色 | 主题色 | 纯白 | 固定 |
| 图标大小 | 24dp | 22dp | -8% |
| 图标颜色 | 彩色 | 黑色 | 去色彩 |
| 栏高度 | 80dp | 80dp | 保持 |

#### WebView 内容区域

**标准版**:
```kotlin
WebView(context).apply {
    settings.apply {
        textZoom = 125  // 125% 缩放
        isVerticalScrollBarEnabled = true  // 显示滚动条
    }
    setBackgroundColor(0x00000000)  // 透明背景
}
```

**E-Ink 版**:
```kotlin
WebView(context).apply {
    settings.apply {
        textZoom = 125  // 保持 125% 缩放
        isVerticalScrollBarEnabled = false  // ❌ 隐藏滚动条
        isFocusable = true
        isFocusableInTouchMode = true
    }
    setBackgroundColor(0x00000000)  // 透明背景
    
    // 强制请求焦点
    requestFocus()
}
```

**对比**:
| 元素 | 标准版 | E-Ink 版 | 原因 |
|-----|-------|---------|------|
| 文字缩放 | 125% | 125% | 保持可读性 |
| 滚动条 | 显示 | 隐藏 | 避免刷新残影 |
| 焦点管理 | 自动 | 强制 | 支持音量键 |
| 背景色 | 透明 | 透明 | 相同 |

---

## 字体排版对比

### 应用内字体

#### 标准版字体设置

```kotlin
val Typography = Typography(
    displayLarge = TextStyle(
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = -0.25.sp
    ),
    displayMedium = TextStyle(
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)
```

#### E-Ink 版使用的字体

E-Ink 版本使用**相同的字体定义**，但在具体使用时倾向于：
- 使用较小的字体级别（如 titleMedium 代替 titleLarge）
- 减少字体大小以节省空间
- 但保持足够的可读性

### 文章内容字体 (CSS)

#### 标准版 CSS 字体

```css
body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, 
                 "Helvetica Neue", Arial, sans-serif;
    font-size: 2rem;        /* 约 32px */
    line-height: 1.6;       /* 行高 1.6 倍 */
    color: #0e0e0e;         /* 接近黑色 */
}

h1 { font-size: 2.5rem; }   /* 约 40px */
h2 { font-size: 2.2rem; }   /* 约 35.2px */
h3 { font-size: 2rem; }     /* 约 32px */

p {
    margin-bottom: 1rem;
    text-align: justify;    /* 两端对齐 */
}

code {
    font-family: "Courier New", Courier, monospace;
    font-size: 1.8rem;      /* 略小于正文 */
}
```

#### E-Ink 版 CSS 字体

```css
body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, 
                 "Helvetica Neue", Arial, sans-serif;
    font-size: 2.2rem;      /* 约 35.2px - 增大 10% */
    line-height: 1.6;       /* 保持 1.6 倍 */
    color: #000000;         /* 纯黑色 */
}

h1 { font-size: 2.7rem; }   /* 约 43.2px */
h2 { font-size: 2.4rem; }   /* 约 38.4px */
h3 { font-size: 2.2rem; }   /* 约 35.2px */

p {
    margin-bottom: 1rem;
    text-align: justify;    /* 两端对齐 */
}

code {
    font-family: "Courier New", Courier, monospace;
    font-size: 2rem;        /* 增大以保持可读性 */
}
```

**对比表**:
| 元素 | 标准版 | E-Ink 版 | 增幅 |
|-----|-------|---------|------|
| 正文 | 2.0rem (32px) | 2.2rem (35.2px) | +10% |
| H1 | 2.5rem (40px) | 2.7rem (43.2px) | +8% |
| H2 | 2.2rem (35.2px) | 2.4rem (38.4px) | +9% |
| H3 | 2.0rem (32px) | 2.2rem (35.2px) | +10% |
| 代码 | 1.8rem (28.8px) | 2.0rem (32px) | +11% |
| 文字颜色 | #0e0e0e | #000000 | 更深 |

---

## 交互反馈对比

### 点击反馈

#### 标准版
- ✅ 使用 Material Design 涟漪效果 (Ripple)
- ✅ 支持触摸反馈动画
- ✅ 按钮有按下状态的颜色变化
- ✅ 卡片有悬浮效果

#### E-Ink 版
- ⚠️ 最小化动画效果
- ⚠️ 减少颜色变化（避免刷新残影）
- ✅ 保留基本的按压状态
- ✅ 使用简单的边框变化代替复杂动画

**建议**: 
```kotlin
// E-Ink 版本的点击效果应该简化
Button(
    onClick = { /* ... */ },
    colors = ButtonDefaults.buttonColors(
        containerColor = Color.White,
        contentColor = Color.Black
    ),
    border = BorderStroke(1.dp, Color.Black),  // 使用边框
    // 避免使用 elevation（阴影会导致刷新问题）
) {
    Text("按钮")
}
```

### 导航过渡

#### 标准版
```kotlin
NavHost(
    navController = navController,
    startDestination = Screen.Feeds.route,
    enterTransition = { fadeIn() + slideIntoContainer(...) },
    exitTransition = { fadeOut() + slideOutOfContainer(...) }
)
```

#### E-Ink 版建议
```kotlin
NavHost(
    navController = navController,
    startDestination = Screen.Feeds.route,
    // E-Ink 版本应该禁用或简化过渡动画
    enterTransition = { EnterTransition.None },
    exitTransition = { ExitTransition.None }
)
```

### 列表滚动

#### 标准版
- ✅ 支持快速滚动
- ✅ 有惯性滚动效果
- ✅ 支持过度滚动效果 (overscroll)

#### E-Ink 版
- ⚠️ 建议限制滚动速度
- ⚠️ 减少惯性滚动距离
- ❌ 禁用过度滚动效果

**实现示例**:
```kotlin
if (BuildConfig.IS_EINK) {
    // E-Ink 版本的滚动优化
    LazyColumn(
        flingBehavior = rememberScrollFlingBehavior(
            decay = exponentialDecay(
                frictionMultiplier = 2f  // 增加摩擦力，减慢滚动
            )
        )
    ) {
        // 列表内容
    }
}
```

---

## 间距和密度对比

### 标准版间距系统

```kotlin
// Material Design 3 标准间距
val spacing = object {
    val tiny = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val xlarge = 32.dp
}

// 实际使用
Column(
    modifier = Modifier.padding(spacing.medium)  // 16dp
) {
    Text("标题")
    Spacer(modifier = Modifier.height(spacing.small))  // 8dp
    Text("内容")
}
```

### E-Ink 版间距系统

```kotlin
// E-Ink 优化的间距（更紧凑）
val einkSpacing = object {
    val tiny = 2.dp      // 减半
    val small = 4.dp     // 减半
    val medium = 12.dp   // 减少 25%
    val large = 16.dp    // 减少 33%
    val xlarge = 24.dp   // 减少 25%
}

// 实际使用
Column(
    modifier = Modifier.padding(einkSpacing.medium)  // 12dp
) {
    Text("标题")
    Spacer(modifier = Modifier.height(einkSpacing.small))  // 4dp
    Text("内容")
}
```

### 间距对比表

| 级别 | 标准版 | E-Ink 版 | 减少 | 用途 |
|-----|-------|---------|------|------|
| **tiny** | 4dp | 2dp | -50% | 最小间距 |
| **small** | 8dp | 4dp | -50% | 小间距 |
| **medium** | 16dp | 12dp | -25% | 标准间距 |
| **large** | 24dp | 16dp | -33% | 大间距 |
| **xlarge** | 32dp | 24dp | -25% | 超大间距 |

**原因**: 墨水屏设备通常屏幕较小，且用户期望在有限空间内看到更多内容。

---

## 图标和图片处理

### 图标大小

| 位置 | 标准版 | E-Ink 版 | 变化 |
|-----|-------|---------|------|
| 顶部栏 | 24dp | 18dp | -25% |
| 底部导航 | 24dp | 20dp | -17% |
| 列表项 | 20dp | 16dp | -20% |
| 操作按钮 | 24dp | 22dp | -8% |
| FAB | 56dp | 56dp | 保持 |

### 图片加载

#### 标准版
```kotlin
AsyncImage(
    model = imageUrl,
    contentDescription = null,
    modifier = Modifier
        .size(120.dp)
        .clip(RoundedCornerShape(8.dp)),  // 圆角
    contentScale = ContentScale.Crop
)
```

#### E-Ink 版建议
```kotlin
if (BuildConfig.IS_EINK) {
    // E-Ink 版本可以考虑禁用图片或转换为黑白
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        modifier = Modifier
            .size(100.dp)  // 略微缩小
            .clip(RectangleShape),  // 使用直角（减少渲染复杂度）
        contentScale = ContentScale.Crop,
        colorFilter = ColorFilter.colorMatrix(
            ColorMatrix().apply { setToSaturation(0f) }  // 转为灰度
        )
    )
} else {
    // 标准版本正常显示彩色图片
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop
    )
}
```

---

## 特殊 UI 元素对比

### 进度指示器

#### 标准版
```kotlin
CircularProgressIndicator(
    color = MaterialTheme.colorScheme.primary,  // 彩色
    strokeWidth = 4.dp
)
```

#### E-Ink 版
```kotlin
CircularProgressIndicator(
    color = Color.Black,  // 黑色
    strokeWidth = 2.dp,   // 更细
    // 考虑使用更简单的指示方式
)

// 或使用文本指示
Text(
    text = "加载中...",
    color = Color.Black,
    style = MaterialTheme.typography.bodyMedium
)
```

### 对话框

#### 标准版
```kotlin
AlertDialog(
    onDismissRequest = { /* ... */ },
    title = { Text("标题") },
    text = { Text("内容") },
    confirmButton = {
        TextButton(onClick = { /* ... */ }) {
            Text("确认", color = MaterialTheme.colorScheme.primary)
        }
    },
    containerColor = MaterialTheme.colorScheme.surface
)
```

#### E-Ink 版
```kotlin
AlertDialog(
    onDismissRequest = { /* ... */ },
    title = { 
        Text(
            "标题",
            color = Color.Black,
            style = MaterialTheme.typography.titleMedium
        )
    },
    text = { 
        Text(
            "内容",
            color = Color.Black,
            style = MaterialTheme.typography.bodyMedium
        )
    },
    confirmButton = {
        TextButton(
            onClick = { /* ... */ },
            border = BorderStroke(1.dp, Color.Black)  // 添加边框
        ) {
            Text("确认", color = Color.Black)
        }
    },
    containerColor = Color.White,
    shape = RectangleShape  // 使用直角（减少渲染复杂度）
)
```

### Snackbar

#### 标准版
```kotlin
Snackbar(
    action = {
        TextButton(onClick = { /* ... */ }) {
            Text("操作", color = MaterialTheme.colorScheme.inversePrimary)
        }
    },
    containerColor = MaterialTheme.colorScheme.inverseSurface,
    contentColor = MaterialTheme.colorScheme.inverseOnSurface
) {
    Text("消息内容")
}
```

#### E-Ink 版
```kotlin
Snackbar(
    action = {
        TextButton(onClick = { /* ... */ }) {
            Text("操作", color = Color.Black)
        }
    },
    containerColor = Color.White,
    contentColor = Color.Black,
    shape = RectangleShape,
    border = BorderStroke(1.dp, Color.Black)  // 添加边框以突出显示
) {
    Text("消息内容")
}
```

---

## 总结和建议

### 核心设计原则

#### 标准版
1. ✅ 遵循 Material Design 3 规范
2. ✅ 支持动态颜色和深色模式
3. ✅ 提供流畅的动画和过渡
4. ✅ 使用丰富的色彩和层次

#### E-Ink 版
1. ✅ **最大化对比度**（纯黑白配色）
2. ✅ **最小化动画**（避免刷新残影）
3. ✅ **紧凑布局**（节省屏幕空间）
4. ✅ **简化视觉**（减少复杂效果）
5. ⚠️ **谨慎刷新**（减少不必要的 UI 更新）

### UI 实现检查清单

使用以下清单确保 E-Ink 版本的 UI 实现正确：

- [ ] 所有背景色使用纯白色 (#FFFFFF)
- [ ] 所有文字颜色使用纯黑色 (#000000)
- [ ] 移除或禁用所有过渡动画
- [ ] 减小间距和内边距（约减少 25-50%）
- [ ] 缩小图标大小（约减少 15-25%）
- [ ] 隐藏滚动条
- [ ] 移除阴影和海拔效果
- [ ] 使用直角而非圆角
- [ ] 图片转换为灰度
- [ ] 简化点击反馈
- [ ] 文章字体增大 10%
- [ ] 导航栏高度减少 30-40%

### 性能优化建议

1. **减少重绘次数**
   ```kotlin
   // 使用 derivedStateOf 避免不必要的重组
   val displayText by remember {
       derivedStateOf { formatText(rawText) }
   }
   ```

2. **批量更新**
   ```kotlin
   // 收集多个更新，一次性应用
   LaunchedEffect(updates) {
       snapshotFlow { updates }
           .debounce(300)  // 延迟 300ms
           .collect { applyUpdates(it) }
   }
   ```

3. **懒加载**
   ```kotlin
   // 使用 LazyColumn 而非 Column
   // 只渲染可见项
   LazyColumn {
       items(largeList) { item ->
           ItemView(item)
       }
   }
   ```

---

**文档版本**: 1.0  
**最后更新**: 2025-10-26  
**维护者**: GitHub Copilot
