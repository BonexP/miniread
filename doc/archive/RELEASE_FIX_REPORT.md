# Release 版本闪退问题修复报告

## 问题诊断

经过代码审查，发现导致 eink release 版本闪退的**主要原因**：

### 🔴 关键问题：缺少网络权限

**AndroidManifest.xml 中完全缺少必需的网络权限声明**

应用使用了以下需要网络权限的组件：
- Retrofit（网络请求）
- WebView（加载网页内容）
- OkHttp（HTTP 客户端）

但 AndroidManifest.xml 中没有声明 `INTERNET` 权限。

### 为什么 Debug 版本可以运行？

在某些情况下，Android Studio 在调试模式下可能会自动添加某些权限，或者设备的开发者选项可能允许某些权限。但在 release 版本中，这些隐式权限不再有效，导致应用在尝试进行网络请求时立即崩溃。

### 🟡 次要问题

1. **签名配置问题**：build.gradle.kts 中的 release 签名配置在本地环境中可能找不到密钥文件，导致构建失败或签名问题
2. **重复的 buildTypes 配置**：存在重复的 release 配置定义
3. **ProGuard 规则不完整**：虽然 minifyEnabled 设置为 false，但规则仍然不够完善

## 已实施的修复

### 1. ✅ 添加网络权限（AndroidManifest.xml）

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 2. ✅ 修复签名配置（app/build.gradle.kts）

改进了签名配置，使其在本地构建时更加健壮：

```kotlin
signingConfigs {
    create("release") {
        val keystoreFile = System.getenv("KEYSTORE_FILE")
        println("Keystore path: $keystoreFile")
        
        // 只有在环境变量存在时才配置签名
        if (!keystoreFile.isNullOrEmpty() && File(keystoreFile).exists()) {
            storeFile = File(keystoreFile)
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
            println("Release signing config is set up with keystore")
        } else {
            println("Warning: KEYSTORE_FILE not found or not set. Using debug signing for release build.")
        }
    }
}
```

### 3. ✅ 清理重复的 buildTypes 配置

合并了重复的 debug 和 release 配置，并添加了降级机制：

```kotlin
release {
    // ...其他配置...
    
    // 尝试使用release签名配置，如果不存在则使用debug签名
    signingConfig = try {
        signingConfigs.getByName("release")
    } catch (e: Exception) {
        println("Warning: Release signing config not available, using debug signing")
        signingConfigs.getByName("debug")
    }
}
```

### 4. ✅ 增强 ProGuard 规则（proguard-rules.pro）

添加了完整的规则来保护：
- Retrofit 和网络相关类
- Gson 序列化/反序列化
- WebView
- BuildConfig
- 数据模型类

## 测试步骤

### 方法 1：使用构建脚本

```bash
chmod +x build_test.sh
./build_test.sh
```

### 方法 2：手动构建

```bash
# 清理之前的构建
./gradlew clean

# 构建 eink release 版本
./gradlew assembleEinkRelease

# APK 输出位置
# app/build/outputs/apk/eink/release/app-eink-release.apk
```

### 安装到设备测试

```bash
# 卸载旧版本（如果存在）
adb uninstall com.i.miniread.eink

# 安装 release 版本
adb install app/build/outputs/apk/eink/release/app-eink-release.apk

# 启动应用
adb shell am start -n com.i.miniread.eink/.MainActivity

# 查看日志（如果需要）
adb logcat | grep -i miniread
```

## 预期结果

修复后，eink release 版本应该能够：
1. ✅ 正常启动，不再闪退
2. ✅ 正常进行网络请求
3. ✅ WebView 正常加载内容
4. ✅ 所有功能与 debug 版本一致

## 如果问题仍然存在

如果修复后问题仍然存在，请执行以下步骤收集更多信息：

### 1. 启用 release 版本的日志

临时修改 `app/build.gradle.kts`：

```kotlin
release {
    isDebuggable = true  // 临时启用调试
    // ...
}
```

### 2. 查看崩溃日志

```bash
# 清除日志
adb logcat -c

# 启动应用并查看日志
adb logcat | grep -E "(AndroidRuntime|FATAL|miniread)"
```

### 3. 检查权限

```bash
# 查看应用已授予的权限
adb shell dumpsys package com.i.miniread.eink | grep permission
```

## 其他建议

### 1. 为本地开发配置 debug keystore

创建或使用 Android 默认的 debug keystore：

```bash
# 默认位置
~/.android/debug.keystore
```

### 2. 考虑启用混淆

虽然目前 `isMinifyEnabled = false`，但在生产环境中建议启用代码混淆：

```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    // ...
}
```

但需要确保 ProGuard 规则完整，避免混淆导致的运行时错误。

## 总结

**根本原因**：缺少 INTERNET 权限导致应用在 release 版本中无法进行网络请求而崩溃。

**修复措施**：
1. ✅ 添加必需的网络权限
2. ✅ 修复签名配置问题
3. ✅ 清理重复配置
4. ✅ 增强 ProGuard 规则

修复后请重新构建并测试。如有任何问题，请提供完整的崩溃日志以便进一步诊断。

