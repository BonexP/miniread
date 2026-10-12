# Release 闪退问题快速修复指南

## 🎯 问题根源

**AndroidManifest.xml 缺少 INTERNET 权限** - 这是导致 release 版本闪退的主要原因！

## ✅ 已修复的文件

1. **app/src/main/AndroidManifest.xml** - 添加网络权限
2. **app/build.gradle.kts** - 修复签名配置和重复定义
3. **app/proguard-rules.pro** - 增强混淆规则

## 🚀 快速测试

```bash
# 1. 清理构建
./gradlew clean

# 2. 构建 eink release
./gradlew assembleEinkRelease

# 3. 安装到设备
adb install -r app/build/outputs/apk/eink/release/app-eink-release.apk

# 4. 启动应用
adb shell am start -n com.i.miniread.eink/.MainActivity
```

## 📋 修复内容摘要

### AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### build.gradle.kts
- ✅ 签名配置添加了文件存在性检查
- ✅ 清理了重复的 buildTypes 配置
- ✅ 添加了降级到 debug 签名的机制

### proguard-rules.pro
- ✅ 添加 Retrofit 保护规则
- ✅ 添加 Gson 保护规则
- ✅ 添加 WebView 保护规则
- ✅ 添加数据模型保护规则

## 📝 详细说明

查看 `RELEASE_FIX_REPORT.md` 了解完整的问题分析和修复详情。

## ⚠️ 如果问题仍然存在

1. 临时启用 release 调试：
   ```kotlin
   release {
       isDebuggable = true
   }
   ```

2. 查看崩溃日志：
   ```bash
   adb logcat | grep -E "(FATAL|AndroidRuntime)"
   ```

3. 提供日志以便进一步诊断

