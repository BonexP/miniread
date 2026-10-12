# Changelog

All notable changes to MiniRead will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.2] - 2026-10-12

### Fixed
- 补齐全仓中英文本地化覆盖。此前多个页面的用户可见文案是硬编码的（中英混杂），导致中文设备显示英文、英文设备显示中文：
  - `ui/CategoryListScreen.kt`（含「标记分类为已读」确认对话框、未读计数）
  - `ui/LoginScreen.kt`（输入框标签、登录按钮、校验提示）
  - `ui/EntryListScreen.kt`（空列表占位、「无标题」）
  - `ui/ArticleDetailScreen.kt`（加载提示、底部操作栏 6 个按钮的无障碍描述、Snackbar 提示）
  - `MainActivity.kt` 的底部导航栏标签与刷新按钮描述、`ui/adaptive/NavigationRail.kt` 的平板侧边栏标签
- 修正 `standard` / `eink` 两个变体的字符串资源结构：原先中文被放在默认 `values/` 目录（英文设备也会显示中文），现拆分为 `values/`（英文）+ `values-zh/`（中文）。

### Changed
- 新增 38 个字符串资源（`nav_*`、`login_*`、`category_list_*`、`entry_list_*`、`article_*`），英文与中文 key 名严格一致（各 74 个）。

## [1.0.1] - 2026-10-12

在 1.0.0 基础上的首个补丁版本。

### Added
- 新增面向 AI Agent 的项目文档 `AGENTS.md`：项目结构、模块职责、常见任务定位路径、开发约定与禁区、已知坑。
- 新增隐藏设置页（**连续点击顶栏标题 7 次**进入），可配置目标域名、feed 白名单与站点 Referer 规则；支持把配置导出到剪贴板 / 从剪贴板导入。

### Changed
- 定向增强（目标域名拦截、feed 白名单、站点 Referer 规则）从写死的常量改为**用户可配置**，代码默认值中性化：默认关闭且全部为空，公开版本不对任何站点做特殊处理。
- 拦截判定改为读取内存快照，移除阅读页组合期的同步读盘（`runBlocking`）。
- 拦截用的 HTTP 客户端改为复用的单例，并显式设置连接 / 读取超时（原实现每次请求新建、无超时）。
- `res/values/arrays.xml` 修正为合法的 `<item>` 结构（原写法会让 `ListPreference` 拿到空列表）。
- 根目录历史过程文档归档到 `doc/archive/`。

### Fixed
- 修复 E-Ink 版本阅读页底部操作栏过高的问题：恢复紧凑高度（48dp）与去除灰调，与列表页底栏高度保持一致。

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

[1.0.2]: https://github.com/BonexP/miniread/releases/tag/v1.0.2
[1.0.1]: https://github.com/BonexP/miniread/releases/tag/v1.0.1
[1.0.0]: https://github.com/BonexP/miniread/releases/tag/v1.0.0
[0.3.4]: https://github.com/BonexP/miniread/releases
