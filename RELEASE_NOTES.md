# MiniRead v1.0.1

MiniRead 是基于 Jetpack Compose + Material Design 3 的 Miniflux 自建 RSS 服务 Android 客户端。

- **版本**：v1.0.1（versionCode 101）
- **Tag**：`v1.0.1`
- **渠道**：GitHub Releases（仅此渠道分发）

## 下载

本次发布提供两个产品变体，可**同时安装在同一台设备**上，互不冲突：

| 变体 | APK 文件 | applicationId | 适用设备 |
|------|----------|---------------|----------|
| Standard（标准版） | `MiniRead-standard-v1.0.1.apk` | `com.i.miniread` | 常规 Android 手机 / 平板 |
| E-Ink（墨水屏版） | `MiniRead-eink-v1.0.1.apk` | `com.i.miniread.eink` | 电子墨水屏设备 |

## 本版新增（1.0.1）

- **Agent 文档**：新增 `AGENTS.md`，说明项目结构、模块职责、常见任务定位路径、开发约定与禁区。
- **定向增强改为可配置**：目标域名拦截、feed 白名单与站点 Referer 规则不再写死在代码里，改为在**隐藏设置页**自行配置（**连续点击顶栏标题 7 次**进入），支持导出 / 导入配置文本。公开版本默认不做任何定向处理。
- **E-Ink 阅读页底栏修复**：恢复紧凑高度与去除灰调，与列表页底栏高度保持一致。
- **质量**：单元测试扩充到 93 个用例（新增拦截配置解析、运行时判定等覆盖）。

## 1.0.0 首发内容

- **R8 压缩**：release 构建启用 R8 代码混淆与资源压缩，减小安装包体积。
- **安全整改**：移除所有打印 API token 的日志；release 关闭 HTTP 日志；明文流量改由 `network_security_config` 显式声明；token 改用 `EncryptedSharedPreferences` 加密存储（含旧数据迁移）；备份规则排除 token。
- **中文本地化**：新增 `values-zh`，提供完整中文界面。
- **版本语义修正**：版本号正式定为 SemVer，移除 `dev`/`alpha` 后缀。
- **依赖瘦身**：移除未使用依赖（volley、work、firebase-messaging、room）与废弃的 `jcenter()` 仓库。
- **E-Ink 优化**：设备检测不再硬编码，修复墨水屏音量键翻页。

## 安装说明

1. 按设备类型下载对应 APK（普通手机选 Standard，墨水屏设备选 E-Ink）。
2. 在系统设置中允许「安装未知来源应用」。
3. 点击 APK 完成安装。
4. 首次启动时输入你的 Miniflux API Token 并保存。

> 两个变体使用不同的 applicationId，因此可以同时安装、分别登录，互不覆盖。

## 已知限制

- 暂无离线缓存。
- 暂无推送通知。
- 未接入墨水屏厂商 SDK（ONYX / 博阅等）；E-Ink 版本通过高对比配色与禁用动画进行优化。
- 隐藏设置页的配置**不会随云备份恢复**（备份规则排除了 DataStore），换机 / 重装后请用「导出 / 导入」恢复。

---

完整变更记录见 [CHANGELOG.md](./CHANGELOG.md)。
