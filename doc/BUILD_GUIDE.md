# MiniRead 多 Flavor 构建指南

本文档详细说明如何在 Android Studio 和 GitHub Workflow 中构建 MiniRead 的多个产品变体（Flavors）。

## 📋 目录

- [Android Studio 操作指南](#android-studio-操作指南)
- [命令行构建](#命令行构建)
- [GitHub Workflow](#github-workflow)
- [构建变体说明](#构建变体说明)
- [故障排除](#故障排除)

---

## Android Studio 操作指南

### 方法 1：使用 Build Variants 面板（推荐）

#### 1. 打开 Build Variants 窗口

在 Android Studio 中：
- 点击左侧边栏的 **Build Variants** 标签
- 或通过菜单: `View` → `Tool Windows` → `Build Variants`

#### 2. 选择构建变体

在 Build Variants 窗口中，你会看到类似这样的选项：

| Module | Active Build Variant |
|--------|---------------------|
| app    | standardDebug ▼     |

点击下拉菜单，你将看到所有可用的构建变体：
- ✅ **standardDebug** - 标准版 Debug
- ✅ **standardRelease** - 标准版 Release
- ✅ **einkDebug** - E-Ink 版 Debug
- ✅ **einkRelease** - E-Ink 版 Release

#### 3. 切换变体

- 选择你想要的变体（例如 `einkDebug`）
- Android Studio 会自动重新索引项目
- 现在运行或调试将使用所选的变体

#### 4. 构建所有变体

**选项 A：通过 Gradle 面板**
1. 打开右侧的 **Gradle** 面板
2. 展开 `miniread` → `app` → `Tasks` → `build`
3. 双击以下任务：
   - `assembleStandardDebug` - 构建标准版 Debug
   - `assembleStandardRelease` - 构建标准版 Release
   - `assembleEinkDebug` - 构建 E-Ink 版 Debug
   - `assembleEinkRelease` - 构建 E-Ink 版 Release
   - `assembleDebug` - 构建所有 Debug 变体
   - `assembleRelease` - 构建所有 Release 变体
   - **`assemble`** - **构建所有变体** ⭐

**选项 B：通过菜单**
1. `Build` → `Generate Signed Bundle / APK...`
2. 选择 **APK**
3. 在 Build Variants 部分，勾选你想要的变体：
   - ☑ standardDebug
   - ☑ standardRelease
   - ☑ einkDebug
   - ☑ einkRelease
4. 点击 **Finish**

### 方法 2：使用 Terminal（最快速）

在 Android Studio 底部的 **Terminal** 标签中：

```bash
# 构建所有变体（Debug + Release）
./gradlew assemble

# 或者分别构建
./gradlew assembleStandardDebug assembleEinkDebug
./gradlew assembleStandardRelease assembleEinkRelease
```

### 查看构建结果

构建完成后，APK 文件位于：

```
app/build/outputs/apk/
├── standard/
│   ├── debug/
│   │   └── app-standard-debug.apk
│   └── release/
│       └── app-standard-release.apk (或已签名版本)
└── eink/
    ├── debug/
    │   └── app-eink-debug.apk
    └── release/
        └── app-eink-release.apk (或已签名版本)
```

在 Android Studio 中右键点击 APK 文件，选择 **Show in Explorer/Finder** 可快速打开文件夹。

---

## 命令行构建

### 基本命令

```bash
# 进入项目根目录
cd /path/to/miniread

# 赋予执行权限（仅首次需要）
chmod +x gradlew

# 构建所有变体
./gradlew assemble
```

### 分别构建各个变体

```bash
# Debug 构建
./gradlew assembleStandardDebug   # 标准版 Debug
./gradlew assembleEinkDebug       # E-Ink 版 Debug
./gradlew assembleDebug           # 所有 Debug 变体

# Release 构建
./gradlew assembleStandardRelease # 标准版 Release
./gradlew assembleEinkRelease     # E-Ink 版 Release
./gradlew assembleRelease         # 所有 Release 变体
```

### 清理并重新构建

```bash
# 清理之前的构建
./gradlew clean

# 清理后构建所有变体
./gradlew clean assemble
```

### 构建 + 安装（需要连接设备）

```bash
# 安装 Standard Debug
./gradlew installStandardDebug

# 安装 E-Ink Debug
./gradlew installEinkDebug

# 卸载
./gradlew uninstallStandardDebug
./gradlew uninstallEinkDebug
```

### 同时构建并安装两个版本

```bash
# 构建并安装 Standard 和 E-Ink Debug 版本
./gradlew assembleStandardDebug assembleEinkDebug && \
adb install -r app/build/outputs/apk/standard/debug/app-standard-debug.apk && \
adb install -r app/build/outputs/apk/eink/debug/app-eink-debug.apk
```

### 并行构建（加速）

```bash
# 使用多个 worker 进程并行构建
./gradlew assemble --parallel --max-workers=4

# 启用构建缓存
./gradlew assemble --build-cache
```

---

## GitHub Workflow

### 自动构建触发器

#### 1. Debug 构建 (`.github/workflows/build.yml`)

**触发条件：**
- 推送到 `main` 或 `develop` 分支
- Pull Request 到 `main` 或 `develop`
- 手动触发

**构建内容：**
- Standard Debug APK
- E-Ink Debug APK

**获取 APK：**
1. 进入 GitHub 仓库的 **Actions** 标签
2. 选择最近的 workflow 运行
3. 在 **Artifacts** 区域下载：
   - `MiniRead-standard-debug`
   - `MiniRead-eink-debug`

#### 2. Release 构建 (`.github/workflows/release.yml`)

**触发条件：**
- 推送带 `v*` 前缀的 tag（例如 `v1.1.0`）

**构建内容：**
- Standard Release APK（已签名）
- E-Ink Release APK（已签名）

**获取 APK：**
- 在 GitHub 的 **Releases** 页面下载

### 手动触发 Debug 构建

1. 进入 GitHub 仓库的 **Actions** 标签
2. 选择 **Build Debug APKs** workflow
3. 点击 **Run workflow** 按钮
4. 选择分支，点击 **Run workflow**
5. 等待构建完成后，在 **Artifacts** 区域下载 APK

### 创建 Release

```bash
# 1. 打上版本标签（附注 tag，与已发布版本一致）
git tag -a v1.1.0 -m "MiniRead 1.1.0 — <本版要点>"

# 2. 推送标签到 GitHub
git push origin v1.1.0

# 3. GitHub Actions 会自动：
#    - 构建 Standard 和 E-Ink 的 Release APK
#    - 签名 APK
#    - 创建 GitHub Release
#    - 上传两个 APK 到 Release
```

### Workflow 配置说明

#### Release Workflow 特点

- ✅ 同时构建两个 flavor
- ✅ 自动重命名 APK（包含版本号）
- ✅ 生成详细的 Release 说明
- ✅ 使用 GitHub Secrets 进行签名

#### 所需的 GitHub Secrets

在仓库的 `Settings` → `Secrets and variables` → `Actions` 中添加：

| Secret Name | 说明 |
|-------------|------|
| `KEYSTORE_BASE64` | Keystore 文件的 Base64 编码 |
| `KEYSTORE_PASSWORD` | Keystore 密码 |
| `KEY_ALIAS` | 密钥别名 |
| `KEY_PASSWORD` | 密钥密码 |

**生成 KEYSTORE_BASE64：**
```bash
base64 -i your-keystore.jks | pbcopy  # macOS
base64 -w 0 your-keystore.jks          # Linux
```

---

## 构建变体说明

### Standard 版本
- **应用 ID**: `com.i.miniread`
- **应用名称**: MiniRead
- **特性**: 
  - 完整的 Material Design 3 主题
  - 支持动态颜色（Material You）
  - 支持深色模式
  - 流畅的动画效果
- **适用设备**: 常规 Android 手机和平板

### E-Ink 版本
- **应用 ID**: `com.i.miniread.eink`
- **应用名称**: MiniRead E-Ink
- **特性**:
  - 高对比度黑白 UI
  - 白色背景 + 黑色边框设计
  - 禁用所有动画
  - 优化的文本渲染
  - 强制浅色主题
- **适用设备**: 电子墨水屏设备（如 BOOX、文石等）

### 两个版本的关系

✅ **可以同时安装** - 不同的应用 ID，互不冲突
✅ **独立数据** - 各自维护独立的用户数据
✅ **共享代码** - 约 95% 的代码共享，只有 UI 层有差异

---

## 故障排除

### 问题 1：构建失败 - "Task 'assembleStandard' not found"

**原因**: Gradle 任务名称区分大小写

**解决**:
```bash
# ❌ 错误
./gradlew assemblestandard

# ✅ 正确（注意大写）
./gradlew assembleStandardDebug
```

### 问题 2：无法切换 Build Variant

**解决**:
1. `File` → `Invalidate Caches...`
2. 选择 **Invalidate and Restart**
3. 重启后重新选择 Build Variant

### 问题 3：Release 构建失败 - 签名错误

**检查**:
1. `local.properties` 中是否配置了正确的 Keystore 路径
2. 环境变量是否正确设置：
   ```bash
   export KEYSTORE_FILE=/path/to/keystore.jks
   export KEYSTORE_PASSWORD=your_password
   export KEY_ALIAS=your_alias
   export KEY_PASSWORD=your_key_password
   ```

**本地 Debug 签名构建**:
```bash
# 使用 Debug 签名
./gradlew assembleDebug
```

### 问题 4：APK 文件找不到

**位置**:
```
app/build/outputs/apk/<flavor>/<buildType>/app-<flavor>-<buildType>.apk
```

**快速查找**:
```bash
find app/build/outputs/apk -name "*.apk" -type f
```

### 问题 5：构建很慢

**优化方案**:

1. **启用并行构建** - 在 `gradle.properties` 中添加：
   ```properties
   org.gradle.parallel=true
   org.gradle.caching=true
   org.gradle.configureondemand=true
   ```

2. **增加 Gradle 内存**:
   ```properties
   org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m
   ```

3. **使用 Gradle Daemon**:
   ```properties
   org.gradle.daemon=true
   ```

### 问题 6：GitHub Actions 构建失败

**常见原因**:
1. Secrets 未正确配置
2. Keystore Base64 编码错误
3. Gradle wrapper 权限问题

**解决**:
- 检查 Actions 日志中的详细错误信息
- 验证所有 Secrets 都已正确添加
- 确保 `gradlew` 有执行权限

---

## 快速参考

### Android Studio
```
1. Build Variants 面板 → 选择变体 → Run
2. Gradle 面板 → Tasks → build → assemble → 双击
3. Terminal → ./gradlew assemble
```

### 命令行
```bash
./gradlew assemble                    # 构建所有
./gradlew assembleDebug               # 所有 Debug
./gradlew assembleRelease             # 所有 Release
./gradlew assembleStandardDebug       # Standard Debug
./gradlew assembleEinkDebug           # E-Ink Debug
```

### GitHub
```bash
git tag -a v1.1.0 -m "MiniRead 1.1.0 — <本版要点>"   # 创建附注标签
git push origin v1.1.0                # 触发 Release 构建
```

---

## 更多资源

- 📖 [Build Flavors 实现指南](./BUILD_FLAVORS_GUIDE.md)
- 🔄 [代码迁移指南](./MIGRATION_GUIDE.md)
- 📱 [设备兼容性](./DEVICE_COMPATIBILITY.md)
- 🎨 [UI 差异说明](./UI_DIFFERENCES.md)

---

**最后更新**: 2025-10-26

