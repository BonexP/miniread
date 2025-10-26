#!/bin/bash

# 构建测试脚本 - 用于测试 eink debug 和 release 版本

echo "=========================================="
echo "清理之前的构建..."
echo "=========================================="
./gradlew clean

echo ""
echo "=========================================="
echo "构建 eink debug 版本..."
echo "=========================================="
./gradlew assembleEinkDebug

if [ $? -eq 0 ]; then
    echo "✅ eink debug 构建成功"
    ls -lh app/build/outputs/apk/eink/debug/
else
    echo "❌ eink debug 构建失败"
    exit 1
fi

echo ""
echo "=========================================="
echo "构建 eink release 版本..."
echo "=========================================="
./gradlew assembleEinkRelease

if [ $? -eq 0 ]; then
    echo "✅ eink release 构建成功"
    ls -lh app/build/outputs/apk/eink/release/
else
    echo "❌ eink release 构建失败"
    exit 1
fi

echo ""
echo "=========================================="
echo "构建完成！"
echo "=========================================="
echo "Debug APK: app/build/outputs/apk/eink/debug/"
echo "Release APK: app/build/outputs/apk/eink/release/"
echo ""
echo "提示：使用 adb install 命令安装 APK 到设备"
echo "例如："
echo "  adb install -r app/build/outputs/apk/eink/debug/app-eink-debug.apk"
echo "  adb install -r app/build/outputs/apk/eink/release/app-eink-release.apk"

