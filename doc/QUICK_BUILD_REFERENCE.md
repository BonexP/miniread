# MiniRead 构建命令速查表

## 🚀 快速命令

### Android Studio
```
1. 左侧 Build Variants 面板 → 选择变体 → Run
2. 右侧 Gradle 面板 → Tasks → build → assemble
3. 底部 Terminal → ./gradlew assemble
```

### 命令行 - 基础

```bash
# 构建所有变体（最常用）
./gradlew assemble

# 清理后重新构建
./gradlew clean assemble

# 查看所有可用任务
./gradlew tasks
```

### 命令行 - Debug 构建

```bash
# 所有 Debug 变体
./gradlew assembleDebug

# Standard Debug
./gradlew assembleStandardDebug

# E-Ink Debug
./gradlew assembleEinkDebug
```

### 命令行 - Release 构建

```bash
# 所有 Release 变体
./gradlew assembleRelease

# Standard Release
./gradlew assembleStandardRelease

# E-Ink Release
./gradlew assembleEinkRelease
```

### 安装到设备

```bash
# 安装 Standard Debug
./gradlew installStandardDebug

# 安装 E-Ink Debug
./gradlew installEinkDebug

# 同时安装两个版本
./gradlew installStandardDebug installEinkDebug

# 卸载
./gradlew uninstallStandardDebug uninstallEinkDebug
```

### 并行构建（更快）

```bash
# 使用多核心并行构建
./gradlew assemble --parallel --max-workers=4

# 启用构建缓存
./gradlew assemble --build-cache

# 完整优化
./gradlew clean assemble --parallel --build-cache
```

## 📦 GitHub Actions

### 自动触发

```bash
# Push 到 main/develop → 自动构建 Debug APK
git push origin main

# 创建 tag → 自动构建 Release APK
git tag v1.0.1
git push origin v1.0.1
```

### 手动触发

1. GitHub → Actions → Build Debug APKs
2. Run workflow → 选择分支 → Run

### 下载 APK

- **Debug**: Actions → 选择运行 → Artifacts
- **Release**: Releases 页面

## 📂 输出位置

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

## 🔍 查找 APK

```bash
# 列出所有生成的 APK
find app/build/outputs/apk -name "*.apk"

# 只显示 Debug APK
find app/build/outputs/apk -name "*debug.apk"

# 只显示 Release APK
find app/build/outputs/apk -name "*release.apk"
```

## 🏗️ Build Variants

| Build Variant | 说明 | 应用 ID | 用途 |
|--------------|------|---------|------|
| standardDebug | 标准版调试 | com.i.miniread | 日常开发 |
| standardRelease | 标准版发布 | com.i.miniread | 正式发布 |
| einkDebug | E-Ink 调试 | com.i.miniread.eink | E-Ink 开发 |
| einkRelease | E-Ink 发布 | com.i.miniread.eink | E-Ink 发布 |

## 🔧 故障排除

### 权限问题
```bash
chmod +x gradlew
```

### 清理构建
```bash
./gradlew clean
rm -rf .gradle build app/build
```

### 刷新依赖
```bash
./gradlew --refresh-dependencies
```

### Android Studio 问题
```
File → Invalidate Caches → Invalidate and Restart
```

## 📖 完整文档

详细说明请参考：
- [BUILD_GUIDE.md](./BUILD_GUIDE.md) - 完整构建指南
- [BUILD_FLAVORS_GUIDE.md](./BUILD_FLAVORS_GUIDE.md) - Flavor 实现指南
- [MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md) - 代码迁移指南

---

**提示**: 使用 `./gradlew tasks --all` 查看所有可用的 Gradle 任务

