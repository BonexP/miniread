# Build Flavors 实施检查清单

使用此清单确保 Build Flavors 正确实施和维护。

## ✅ 初始设置检查

### 构建配置
- [x] `app/build.gradle.kts` 中添加了 `flavorDimensions`
- [x] 定义了 `productFlavors` (standard, eink)
- [x] 启用了 `buildConfig = true`
- [x] 配置了 `BuildConfig` 字段（FLAVOR_TYPE, IS_EINK）
- [x] 设置了 `applicationIdSuffix`
- [x] 设置了 `versionNameSuffix`
- [x] 使用 `resValue` 定义了 app_name

### 源代码结构
- [x] 创建了 `src/standard/` 目录
- [x] 创建了 `src/eink/` 目录
- [x] Standard flavor: `EInkConfig.kt`
- [x] Standard flavor: `EInkUtils.kt`
- [x] E-Ink flavor: `EInkConfig.kt`
- [x] E-Ink flavor: `EInkUtils.kt`
- [x] 两个 flavor 的 API 保持一致

### 资源文件
- [x] Standard: `res/values/colors.xml`
- [x] Standard: `res/values/strings.xml`
- [x] E-Ink: `res/values/colors.xml`
- [x] E-Ink: `res/values/strings.xml`
- [x] 资源使用了统一的命名约定（flavor_*, eink_*）

### 文档
- [x] 创建了 `doc/` 目录
- [x] `doc/README.md` - 文档导航
- [x] `doc/BUILD_FLAVORS_GUIDE.md` - 完整开发指南
- [x] `doc/QUICK_REFERENCE.md` - 快速参考
- [x] `doc/MIGRATION_GUIDE.md` - 迁移指南
- [x] `FLAVOR_IMPLEMENTATION.md` - 实施总结

### 示例代码
- [x] 创建了 `FlavorUsageExample.kt` 示例文件

## 📋 开发阶段检查

### 添加新功能时
- [ ] 优先考虑将代码放在 `src/main/` 中
- [ ] 只在必须时才创建 flavor 特定代码
- [ ] 确保两个 flavor 的 API 兼容
- [ ] 使用 `BuildConfig.IS_EINK` 进行条件判断
- [ ] 更新相关文档

### 修改现有功能时
- [ ] 检查是否影响两个 flavor
- [ ] 在两个 flavor 中测试修改
- [ ] 更新相关示例代码
- [ ] 更新文档（如有必要）

### UI/UX 开发
- [ ] E-Ink 版禁用或简化动画
- [ ] E-Ink 版使用高对比度颜色
- [ ] 使用 `EInkConfig.Colors` 而非硬编码颜色
- [ ] 考虑 E-Ink 屏幕的刷新特性

## 🧪 测试阶段检查

### 单元测试
- [ ] 为共享代码编写测试
- [ ] 为 flavor 特定代码编写测试
- [ ] 运行 `./gradlew testStandardDebugUnitTest`
- [ ] 运行 `./gradlew testEinkDebugUnitTest`
- [ ] 所有测试通过

### 构建测试
- [ ] 构建 standardDebug 成功
- [ ] 构建 standardRelease 成功
- [ ] 构建 einkDebug 成功
- [ ] 构建 einkRelease 成功
- [ ] 检查 APK 大小合理
- [ ] 验证 Application ID 正确

### 功能测试
- [ ] 在普通设备上测试标准版
- [ ] 在 E-Ink 设备上测试 E-Ink 版（如有条件）
- [ ] 测试核心功能在两个版本中都正常
- [ ] 测试 E-Ink 特定功能（刷新、对比度等）
- [ ] 测试动画在对应版本中的表现

### UI 测试
- [ ] 标准版 UI 正常显示
- [ ] E-Ink 版 UI 正常显示
- [ ] 颜色方案在两个版本中正确应用
- [ ] 文本清晰可读
- [ ] 图标和图片适配正确

## 🚀 发布前检查

### 版本信息
- [ ] 更新 `versionCode`
- [ ] 更新 `versionName`
- [ ] 更新 CHANGELOG（如有）
- [ ] 检查版本号在两个 flavor 中一致

### 签名配置
- [ ] 配置了 release 签名
- [ ] 签名密钥文件存在
- [ ] 环境变量正确设置（KEYSTORE_FILE, KEYSTORE_PASSWORD 等）
- [ ] 测试签名构建成功

### 代码质量
- [ ] 运行代码检查/Lint
- [ ] 修复所有错误和严重警告
- [ ] 代码格式化
- [ ] 移除调试代码和注释

### 文档
- [ ] 更新项目 README（如有必要）
- [ ] 更新 API 文档（如有必要）
- [ ] 更新版本说明
- [ ] 文档与代码同步

### 最终测试
- [ ] 在真机上测试标准版 Release
- [ ] 在真机上测试 E-Ink 版 Release（如有条件）
- [ ] 测试安装和卸载流程
- [ ] 测试升级流程（从旧版本升级）
- [ ] 检查权限请求正常
- [ ] 检查崩溃报告和日志

### 构建产物
- [ ] 生成所有 Release APK
- [ ] 验证 APK 可以正常安装
- [ ] 记录 APK 文件大小
- [ ] 备份 APK 文件
- [ ] 准备发布说明

## 🔧 维护阶段检查

### 定期维护（每次更新）
- [ ] 检查两个 flavor 的功能一致性
- [ ] 更新依赖库版本
- [ ] 测试新的 Android SDK 兼容性
- [ ] 清理未使用的代码和资源
- [ ] 更新文档

### 性能监控
- [ ] 监控 APK 大小变化
- [ ] 监控启动时间
- [ ] 检查内存使用
- [ ] E-Ink 版刷新性能
- [ ] 用户反馈收集

### 问题追踪
- [ ] 记录 flavor 特定的 bug
- [ ] 标注问题影响的 flavor
- [ ] 在两个 flavor 中验证修复
- [ ] 更新已知问题列表

## 📝 新增 Flavor 检查清单

如果需要添加新的 flavor（如 lite, pro 等）：

- [ ] 在 `build.gradle.kts` 中添加新的 productFlavor
- [ ] 创建对应的源码目录 `src/[flavor_name]/`
- [ ] 创建必要的资源文件
- [ ] 实现 flavor 特定的代码
- [ ] 添加到构建和测试流程
- [ ] 更新文档
- [ ] 更新示例代码
- [ ] 更新此检查清单

## 🎯 代码审查清单

在代码审查时检查：

- [ ] 没有硬编码的 flavor 判断
- [ ] 使用 `BuildConfig.IS_EINK` 而非其他方式判断
- [ ] 正确使用了 `EInkConfig` 和 `EInkUtils`
- [ ] 资源命名遵循约定
- [ ] 没有在 flavor 特定目录中放置共享代码
- [ ] API 在所有 flavor 中保持一致
- [ ] 有适当的注释和文档

## ⚠️ 常见问题检查

遇到问题时检查：

### 构建失败
- [ ] 同步项目（Sync Project with Gradle Files）
- [ ] 清理构建（Clean Project）
- [ ] 重新构建（Rebuild Project）
- [ ] 检查 Gradle 版本兼容性
- [ ] 检查依赖版本冲突

### Flavor 切换问题
- [ ] 在 Build Variants 面板中选择正确的变体
- [ ] 重新同步项目
- [ ] 重启 Android Studio
- [ ] Invalidate Caches / Restart

### 资源找不到
- [ ] 检查资源是否在正确的 flavor 目录中
- [ ] 确认资源 ID 一致
- [ ] 检查是否缺少必要的资源文件
- [ ] 清理并重新构建

### 代码提示失效
- [ ] 确认 Build Variant 已选择
- [ ] 同步项目
- [ ] Invalidate Caches
- [ ] 检查 IDE 索引状态

## 📊 质量指标

维护以下指标以确保质量：

### 构建指标
- [ ] 构建成功率 > 95%
- [ ] 构建时间 < 5分钟
- [ ] APK 大小增长 < 10% （每次更新）

### 测试指标
- [ ] 单元测试覆盖率 > 70%
- [ ] 所有测试通过率 100%
- [ ] UI 测试覆盖核心功能

### 代码质量指标
- [ ] Lint 错误数 = 0
- [ ] Lint 严重警告数 < 5
- [ ] 代码重复率 < 5%

## 🔄 持续改进

定期回顾和改进：

- [ ] 每月回顾此检查清单
- [ ] 根据实际情况更新清单
- [ ] 收集团队反馈
- [ ] 优化开发流程
- [ ] 更新最佳实践

---

**检查清单版本**: 1.0  
**最后更新**: 2025-10-26  
**维护者**: MiniRead 开发团队

## 使用说明

1. **开发阶段**：每次添加/修改功能时，参考"开发阶段检查"
2. **测试阶段**：在提交代码前，完成"测试阶段检查"
3. **发布前**：准备发布时，完整完成"发布前检查"
4. **日常维护**：定期进行"维护阶段检查"

建议将此文件打印或保存为书签，方便随时查阅。

