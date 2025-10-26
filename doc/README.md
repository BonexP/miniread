# MiniRead 文档目录

欢迎查阅 MiniRead 项目文档！

## 🚀 快速开始

- 🏗️ **[BUILD_GUIDE.md](./BUILD_GUIDE.md)** - 完整的构建指南（**推荐新手阅读**）
- ⚡ **[QUICK_BUILD_REFERENCE.md](./QUICK_BUILD_REFERENCE.md)** - 构建命令速查表
- 📝 **[BUILD_CONFIGURATION_SUMMARY.md](./BUILD_CONFIGURATION_SUMMARY.md)** - 构建配置完成总结

## 📚 核心文档

### [BUILD_GUIDE.md](./BUILD_GUIDE.md) ⭐ 新增
**完整构建指南** - 一站式构建操作手册

包含内容：
- Android Studio 操作指南（3种方法）
- 命令行构建详解
- GitHub Workflow 使用说明
- 构建变体对比
- 故障排除指南
- 快速参考命令

**适合人群**：所有需要构建项目的开发者

### [QUICK_BUILD_REFERENCE.md](./QUICK_BUILD_REFERENCE.md) ⭐ 新增
**构建命令速查表** - 快速查找构建命令

包含内容：
- 所有常用构建命令
- GitHub Actions 操作
- APK 输出位置
- Build Variants 对照表
- 快速故障排除

**适合人群**：日常开发需要快速查找命令的开发者

### [BUILD_CONFIGURATION_SUMMARY.md](./BUILD_CONFIGURATION_SUMMARY.md) ⭐ 新增
**构建配置完成总结** - 配置说明和使用指南

包含内容：
- 已完成的配置说明
- Android Studio 使用指南
- GitHub Actions 使用指南
- 配置检查清单
- 构建变体对比

**适合人群**：了解整体配置的开发者、项目维护者

### [BUILD_FLAVORS_GUIDE.md](./BUILD_FLAVORS_GUIDE.md)
**Build Flavors 开发指南** - 详细的多风味开发和维护文档

包含内容：
- 项目结构说明
- Build Variants 配置
- 开发最佳实践
- E-Ink 优化要点
- 测试策略
- 开发工作流
- 发布流程
- 常见问题解答
- E-Ink 设备适配建议

**适合人群**：新加入的开发者、需要了解架构设计的开发者

### [QUICK_REFERENCE.md](./QUICK_REFERENCE.md)
**快速参考手册** - 常用代码片段和配置

包含内容：
- 代码片段示例
- 目录结构速查
- 资源命名约定
- 调试技巧
- 版本发布检查清单

**适合人群**：日常开发需要快速查找代码示例的开发者

### [MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md)
**迁移指南** - 将现有代码迁移到 Flavor 架构

包含内容：
- 迁移步骤说明
- 代码重构示例
- 迁移检查清单
- 构建与发布说明
- 常见问题解答

**适合人群**：需要重构现有代码以适配新架构的开发者

### [CHECKLIST.md](./CHECKLIST.md)
**检查清单** - 开发、测试、发布全流程检查清单

包含内容：
- 初始设置检查
- 开发阶段检查
- 测试阶段检查
- 发布前检查
- 维护阶段检查
- 质量指标

**适合人群**：需要确保代码质量和发布流程的所有开发者

## 🎯 快速导航

### 我想...

- **了解项目的 Flavor 架构** → [BUILD_FLAVORS_GUIDE.md](./BUILD_FLAVORS_GUIDE.md#概述)
- **构建某个特定版本** → [QUICK_REFERENCE.md](./QUICK_REFERENCE.md#快速命令)
- **迁移现有代码** → [MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md)
- **添加 E-Ink 优化功能** → [BUILD_FLAVORS_GUIDE.md](./BUILD_FLAVORS_GUIDE.md#4-e-ink-版本优化要点)
- **查看代码示例** → [QUICK_REFERENCE.md](./QUICK_REFERENCE.md#代码片段)
- **准备发布新版本** → [QUICK_REFERENCE.md](./QUICK_REFERENCE.md#版本发布检查清单)
- **解决常见问题** → [BUILD_FLAVORS_GUIDE.md](./BUILD_FLAVORS_GUIDE.md#常见问题)

## 🏗️ 项目 Flavors 概览

MiniRead 目前支持两个产品风味：

| Flavor | 目标设备 | 特点 | Application ID |
|--------|---------|------|----------------|
| **Standard** | 普通智能手机/平板 | 全功能、彩色UI、流畅动画 | `com.i.miniread` |
| **E-Ink** | 电子墨水屏设备 | 黑白优化、无动画、刷新控制 | `com.i.miniread.eink` |

## 🚀 快速开始

### 1. 构建标准版
```bash
./gradlew assembleStandardDebug
```

### 2. 构建 E-Ink 版
```bash
./gradlew assembleEinkDebug
```

### 3. 切换 Build Variant（Android Studio）
View → Tool Windows → Build Variants → 选择目标变体

## 📋 项目结构

```
miniread/
├── app/
│   ├── src/
│   │   ├── main/          # 共享代码和资源
│   │   ├── standard/      # 标准版专属
│   │   ├── eink/          # E-Ink 版专属
│   │   └── test/          # 测试代码
│   └── build.gradle.kts   # 构建配置
└── doc/                   # 📍 你在这里
    ├── README.md          # 本文件
    ├── BUILD_FLAVORS_GUIDE.md
    └── QUICK_REFERENCE.md
```

## 🔧 开发建议

1. **阅读文档**：开发前请先阅读 [BUILD_FLAVORS_GUIDE.md](./BUILD_FLAVORS_GUIDE.md)
2. **共享优先**：新功能优先放在 `main/` 中，只在必要时才放在 flavor 特定目录
3. **保持兼容**：确保两个 flavor 的 API 兼容性
4. **测试充分**：每个 flavor 都应该有对应的测试
5. **文档更新**：修改架构时记得更新文档

## 📝 文档维护

- 文档应与代码同步更新
- 发现问题或有改进建议请及时提出
- 添加新功能时，请更新相应文档

## 🤝 贡献指南

如需贡献代码或文档：
1. Fork 项目
2. 创建特性分支
3. 提交变更
4. 发起 Pull Request

---

**最后更新**: 2025-10-26  
**维护者**: MiniRead 开发团队

