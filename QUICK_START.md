# 🚀 快速开始指南

**仅需 5 分钟，立即开始使用 Build Flavors！**

## 📱 你现在有什么？

MiniRead 项目现在支持 **两个产品版本**：

| 版本 | 用途 | 特点 |
|------|------|------|
| 🎨 **Standard** | 普通手机/平板 | 彩色UI、流畅动画 |
| 📖 **E-Ink** | 电子墨水屏设备 | 黑白优化、无动画 |

## ⚡ 快速操作

### 1️⃣ 切换版本（Android Studio）

```
View → Tool Windows → Build Variants
选择：standardDebug 或 einkDebug
```

### 2️⃣ 构建特定版本

```bash
# 标准版
./gradlew assembleStandardDebug

# E-Ink 版
./gradlew assembleEinkDebug
```

### 3️⃣ 在代码中判断版本

```kotlin
import com.i.miniread.BuildConfig

if (BuildConfig.IS_EINK) {
    // E-Ink 版本的代码
} else {
    // 标准版的代码
}
```

## 📝 三个核心文件

### 1. EInkConfig - 配置类
```kotlin
import com.i.miniread.eink.EInkConfig

// 获取颜色
val bgColor = EInkConfig.Colors.Background
val textColor = EInkConfig.Colors.OnSurface

// 检查设置
if (EInkConfig.DISABLE_ANIMATIONS) {
    // 禁用动画
}
```

### 2. EInkUtils - 工具类
```kotlin
import com.i.miniread.eink.EInkUtils

// 检查设备
if (EInkUtils.isEInkDevice()) {
    // 刷新屏幕
    EInkUtils.refreshScreen(fullRefresh = true)
}
```

### 3. BuildConfig - 构建配置
```kotlin
import com.i.miniread.BuildConfig

println("版本: ${BuildConfig.FLAVOR_TYPE}")
println("是否E-Ink: ${BuildConfig.IS_EINK}")
```

## 🎨 典型使用场景

### 场景 1：自适应动画

```kotlin
AnimatedVisibility(
    visible = isVisible,
    enter = if (BuildConfig.IS_EINK) {
        EnterTransition.None  // E-Ink: 无动画
    } else {
        fadeIn()  // 标准版: 淡入
    }
) {
    // 你的内容
}
```

### 场景 2：自适应颜色

```kotlin
import com.i.miniread.eink.EInkConfig

Text(
    text = "Hello",
    color = EInkConfig.Colors.OnSurface  // 自动适配
)
```

### 场景 3：E-Ink 优化包装

```kotlin
import com.i.miniread.eink.EInkOptimized

@Composable
fun MyScreen() {
    EInkOptimized {
        // 自动应用 E-Ink 优化
        Column { /* ... */ }
    }
}
```

## 📂 文件放在哪里？

```
app/src/
├── main/           ← 🎯 大部分代码放这里
├── standard/       ← 仅标准版专属代码
└── eink/          ← 仅E-Ink版专属代码
```

**规则**：能共享就共享，必须分开才分开！

## 🔍 查看完整示例

打开文件：`app/src/main/java/com/i/miniread/example/FlavorUsageExample.kt`

包含 7 个实用示例！

## 📚 需要更多帮助？

| 需求 | 查看文档 |
|------|---------|
| 📖 完整教程 | [BUILD_FLAVORS_GUIDE.md](./doc/BUILD_FLAVORS_GUIDE.md) |
| ⚡ 命令速查 | [QUICK_REFERENCE.md](./doc/QUICK_REFERENCE.md) |
| 🔄 代码迁移 | [MIGRATION_GUIDE.md](./doc/MIGRATION_GUIDE.md) |
| ✅ 质量检查 | [CHECKLIST.md](./doc/CHECKLIST.md) |

## 💡 记住这三点

1. **✨ 共享优先**：新代码先放 `main/`，必要时才分开
2. **🎨 用配置类**：使用 `EInkConfig` 和 `EInkUtils`，不要硬编码
3. **🧪 都要测试**：两个版本都要测试！

## 🚦 你准备好了！

现在你可以：
- ✅ 切换和构建不同版本
- ✅ 在代码中判断当前版本
- ✅ 使用 E-Ink 优化功能
- ✅ 知道去哪里查详细文档

**开始编码吧！** 🎉

---

**提示**：把这个文件加入书签，随时查看！

