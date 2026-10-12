# Changelog

All notable changes to MiniRead will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-10-12

MiniRead 首个正式稳定版本。本次发布将版本语义修正为 SemVer 并升至 1.0.0，同时
对构建、签名、依赖、安全与本地化进行系统性整理。

### Added
- 新增中文本地化资源（`values-zh`）。
- 新增单元测试（域名校验、feed 排序、时间窗计算、Gson 解析），并接入 CI。
- 新增 `network_security_config`，以显式声明方式管理明文流量。

### Changed
- 版本语义修正并升级：`versionCode 100` / `versionName 1.0.0`，取代 0.3.4（33）；移除 `dev`/`alpha` 版本名后缀。
- 两个产品变体正式定名：`standard`（`com.i.miniread`，应用名 `MiniRead`）与 `eink`（`com.i.miniread.eink`，应用名 `MiniRead E-Ink`），两者可同机共存。
- `buildTypes` 去重；release 构建启用 R8 代码混淆与资源压缩。
- 保留并集中管理针对特定 feed 源/域名的定向增强（目标域名定向拦截、特定 feed 白名单、部分站点 Referer 规则），便于统一维护。
- 清理未使用依赖（volley、work、firebase-messaging、room），移除废弃的 `jcenter()` 仓库。
- E-Ink 优化：设备检测不再硬编码；同步修复墨水屏音量键翻页。
- 修复 README 徽章与排版；清理空文件与过时版本号。

### Fixed
- 修复 E-Ink 版本音量键翻页失效问题。
- 修复 README 中 CI 徽章链接与文档段落排版问题。

### Security
- 移除所有打印 API token 的日志。
- release 构建关闭 HTTP 日志输出。
- 明文流量改由 `network_security_config` 显式声明。
- API token 改用 `EncryptedSharedPreferences` 加密存储，并包含旧数据迁移。
- 备份规则排除 token，避免敏感信息进入系统备份。

### Known Limitations
- 暂无离线缓存。
- 暂无推送通知。
- 未接入墨水屏厂商 SDK（ONYX/博阅等），E-Ink 版本通过高对比配色与禁用动画进行优化。

## [0.3.4] - 开发阶段

### Development Phase
- 功能开发与测试迭代。
- E-Ink 变体实现与 flavor 体系搭建。
- 多项 Bug 修复与体验改进。

---

[1.0.0]: https://github.com/BonexP/miniread/releases/tag/v1.0.0
[0.3.4]: https://github.com/BonexP/miniread/releases
