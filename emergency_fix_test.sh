#!/bin/bash

# 紧急修复测试脚本
# 用于构建、安装和收集 release 版本的日志

set -e  # 遇到错误立即退出

APP_PACKAGE="com.i.miniread.eink"
COLOR_RED='\033[0;31m'
COLOR_GREEN='\033[0;32m'
COLOR_YELLOW='\033[1;33m'
COLOR_BLUE='\033[0;34m'
COLOR_RESET='\033[0m'

echo -e "${COLOR_BLUE}=========================================="
echo "MiniRead Release 紧急修复测试"
echo -e "==========================================${COLOR_RESET}"
echo ""

# 检查设备连接
echo -e "${COLOR_YELLOW}检查设备连接...${COLOR_RESET}"
if ! adb devices | grep -q "device$"; then
    echo -e "${COLOR_RED}❌ 错误：没有检测到已连接的设备${COLOR_RESET}"
    exit 1
fi
echo -e "${COLOR_GREEN}✅ 设备已连接${COLOR_RESET}"
echo ""

# 清理旧构建
echo -e "${COLOR_YELLOW}清理旧构建...${COLOR_RESET}"
./gradlew clean
echo -e "${COLOR_GREEN}✅ 清理完成${COLOR_RESET}"
echo ""

# 同步依赖
echo -e "${COLOR_YELLOW}同步 Gradle 依赖...${COLOR_RESET}"
./gradlew --refresh-dependencies
echo -e "${COLOR_GREEN}✅ 依赖同步完成${COLOR_RESET}"
echo ""

# 构建 eink release
echo -e "${COLOR_YELLOW}开始构建 eink release 版本...${COLOR_RESET}"
./gradlew assembleEinkRelease

if [ $? -eq 0 ]; then
    echo -e "${COLOR_GREEN}✅ 构建成功${COLOR_RESET}"
else
    echo -e "${COLOR_RED}❌ 构建失败${COLOR_RESET}"
    exit 1
fi
echo ""

# 卸载旧版本
echo -e "${COLOR_YELLOW}卸载旧版本...${COLOR_RESET}"
adb uninstall $APP_PACKAGE 2>/dev/null || echo "没有旧版本需要卸载"
echo ""

# 安装新版本
echo -e "${COLOR_YELLOW}安装新版本...${COLOR_RESET}"
APK_PATH="app/build/outputs/apk/eink/release/app-eink-release.apk"
if [ ! -f "$APK_PATH" ]; then
    echo -e "${COLOR_RED}❌ 找不到 APK 文件: $APK_PATH${COLOR_RESET}"
    exit 1
fi

adb install "$APK_PATH"
if [ $? -eq 0 ]; then
    echo -e "${COLOR_GREEN}✅ 安装成功${COLOR_RESET}"
else
    echo -e "${COLOR_RED}❌ 安装失败${COLOR_RESET}"
    exit 1
fi
echo ""

# 检查应用权限
echo -e "${COLOR_YELLOW}检查应用权限...${COLOR_RESET}"
echo -e "${COLOR_BLUE}应用权限信息：${COLOR_RESET}"
adb shell dumpsys package $APP_PACKAGE | grep -A 5 "permission" | head -20
echo ""

# 清除日志
echo -e "${COLOR_YELLOW}清除旧日志...${COLOR_RESET}"
adb logcat -c
echo ""

# 启动应用
echo -e "${COLOR_YELLOW}启动应用...${COLOR_RESET}"
adb shell am start -n $APP_PACKAGE/.MainActivity
echo -e "${COLOR_GREEN}✅ 应用已启动${COLOR_RESET}"
echo ""

# 等待几秒
echo -e "${COLOR_YELLOW}等待 3 秒以收集日志...${COLOR_RESET}"
sleep 3
echo ""

# 收集日志
LOG_FILE="emergency_fix_log_$(date +%Y%m%d_%H%M%S).txt"
echo -e "${COLOR_BLUE}=========================================="
echo "收集应用日志（前 100 行）"
echo -e "==========================================${COLOR_RESET}"
adb logcat -d | grep -i -E "(miniread|fatal|exception|androidruntime)" | head -100 | tee "$LOG_FILE"
echo ""

# 检查是否有致命错误
if grep -q "FATAL" "$LOG_FILE"; then
    echo -e "${COLOR_RED}=========================================="
    echo "⚠️  发现致命错误！"
    echo -e "==========================================${COLOR_RESET}"
    echo ""
    grep -A 20 "FATAL" "$LOG_FILE"
    echo ""
    echo -e "${COLOR_RED}完整日志已保存到: $LOG_FILE${COLOR_RESET}"
else
    echo -e "${COLOR_GREEN}=========================================="
    echo "✅ 未发现致命错误"
    echo -e "==========================================${COLOR_RESET}"
    echo ""
    echo "日志已保存到: $LOG_FILE"
fi

echo ""
echo -e "${COLOR_BLUE}=========================================="
echo "测试完成！"
echo -e "==========================================${COLOR_RESET}"
echo ""
echo "如果应用仍然崩溃，请："
echo "1. 查看日志文件: $LOG_FILE"
echo "2. 运行持续日志监控: adb logcat | grep -i miniread"
echo "3. 检查设备是否支持 Android 9+ 的网络安全配置"
echo ""

