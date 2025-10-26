#!/bin/bash

# MiniRead 构建配置验证脚本
# 用于验证所有构建变体是否正确配置

echo "======================================"
echo "  MiniRead 构建配置验证"
echo "======================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 gradlew 是否存在
if [ ! -f "./gradlew" ]; then
    echo -e "${RED}❌ 错误: gradlew 文件不存在${NC}"
    echo "请确保在项目根目录运行此脚本"
    exit 1
fi

# 赋予执行权限
chmod +x ./gradlew

echo "1. 检查 Gradle 配置..."
if ./gradlew --version > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Gradle 配置正常${NC}"
else
    echo -e "${RED}❌ Gradle 配置错误${NC}"
    exit 1
fi

echo ""
echo "2. 列出所有可用的构建变体..."
echo -e "${YELLOW}可用的 Build Variants:${NC}"
./gradlew tasks --all | grep "assemble" | grep -E "(Standard|Eink)" | sort

echo ""
echo "3. 检查 Build Flavors..."
if ./gradlew tasks --all | grep -q "assembleStandardDebug"; then
    echo -e "${GREEN}✅ Standard flavor 配置正确${NC}"
else
    echo -e "${RED}❌ Standard flavor 配置错误${NC}"
fi

if ./gradlew tasks --all | grep -q "assembleEinkDebug"; then
    echo -e "${GREEN}✅ E-Ink flavor 配置正确${NC}"
else
    echo -e "${RED}❌ E-Ink flavor 配置错误${NC}"
fi

echo ""
echo "4. 检查文档文件..."

docs=(
    "doc/BUILD_GUIDE.md"
    "doc/QUICK_BUILD_REFERENCE.md"
    "doc/BUILD_CONFIGURATION_SUMMARY.md"
    ".github/workflows/build.yml"
    ".github/workflows/release.yml"
)

for doc in "${docs[@]}"; do
    if [ -f "$doc" ]; then
        echo -e "${GREEN}✅ $doc 存在${NC}"
    else
        echo -e "${RED}❌ $doc 不存在${NC}"
    fi
done

echo ""
echo "5. 测试构建（仅验证配置，不实际构建）..."
if ./gradlew tasks --dry-run > /dev/null 2>&1; then
    echo -e "${GREEN}✅ 构建配置验证通过${NC}"
else
    echo -e "${RED}❌ 构建配置验证失败${NC}"
fi

echo ""
echo "======================================"
echo "  验证完成"
echo "======================================"
echo ""
echo "📝 构建所有变体的命令:"
echo "   ./gradlew assemble"
echo ""
echo "📖 查看详细文档:"
echo "   - doc/BUILD_GUIDE.md"
echo "   - doc/QUICK_BUILD_REFERENCE.md"
echo ""
echo "🚀 开始构建？运行以下命令:"
echo -e "${YELLOW}   ./gradlew assemble${NC}"
echo ""

