# MiniRead 文档目录

> 这里是**文档导航入口**（L2 参考）。**唯一权威的 AI 开发指令是仓库根目录的 [AGENTS.md](../AGENTS.md)**；命令、硬约束、发布流程以其为准，本文与其它文档冲突时一律以 AGENTS.md 为准。
> 分层规则见 AGENTS.md「文档地图」：L1 权威（与代码同步维护） / L2 参考（允许滞后，不得与 L1 冲突） / L3 历史存档（不保证与当前代码一致，勿据此判断现状） / 外部资料。
> 当前版本：**v1.2.2**（versionCode 122）。

---

## L1 权威 — 与代码同步维护

改代码必须同步这些文档。

| 文档 | 一句话定位 |
| --- | --- |
| [AGENTS.md](../AGENTS.md) | AI 开发指令（唯一权威）：优先级、硬约束、禁区、已知坑、发布与 VCS 纪律 |
| [README.md](../README.md) | 项目简介、功能、两个构建变体 |
| [CHANGELOG.md](../CHANGELOG.md) | 变更日志（Keep a Changelog / SemVer） |
| [RELEASE_NOTES.md](../RELEASE_NOTES.md) | 面向用户的发布说明 |
| [BUILD_GUIDE.md](./BUILD_GUIDE.md) | 完整构建指南（Android Studio / 命令行 / CI / 故障排除） |
| [BUILD_FLAVORS_GUIDE.md](./BUILD_FLAVORS_GUIDE.md) | Flavor 架构与开发指南 |
| [QUICK_BUILD_REFERENCE.md](./QUICK_BUILD_REFERENCE.md) | 构建命令速查（**日常命令以此为准**） |

## L2 参考 — 按需查阅

允许滞后，但不得与 L1 冲突；每份顶部都有状态横幅。

| 文档 | 一句话定位 |
| --- | --- |
| [KNOWN_PITFALLS.md](./KNOWN_PITFALLS.md) | AGENTS.md §8 已知坑的**完整论证**（回归点 / 实测数据 / 推导） |
| [VC_WORKFLOW.md](./VC_WORKFLOW.md) | AGENTS.md §10 的**命令表 / 回退 jj / eol 与 tag 实测** |
| [TECH_DEBT.md](../TECH_DEBT.md) | 悬而未决的技术债清单（只记录不实施；🔒 禁区 / ✋ 需授权已标注） |
| [TECHNICAL_SUMMARY.md](./TECHNICAL_SUMMARY.md) | 技术总结（架构与实现快照） |
| [TABLET_ADAPTATION.md](./TABLET_ADAPTATION.md) | 平板适配（响应式阅读页、媒体查询） |
| [UI_DIFFERENCES.md](./UI_DIFFERENCES.md) | Standard / E-Ink 两个变体的 UI 差异 |
| [QUICK_REFERENCE.md](./QUICK_REFERENCE.md) | 代码片段与配置速查 |
| [CHECKLIST.md](./CHECKLIST.md) | 开发 / 测试 / 发布检查清单 |
| [DEVICE_COMPATIBILITY.md](./DEVICE_COMPATIBILITY.md) | 设备兼容性记录 |
| [QUICK_START.md](../QUICK_START.md) | 入门速览（命令以 `QUICK_BUILD_REFERENCE.md` 为准） |

## L3 历史存档 — 不保证与当前代码一致

只读，**不得据此判断现状**；含 2025-10 flavor 合并期的过期配置示范（flavor 级 `versionNameSuffix`、旧版本号），现行规则见 AGENTS.md §6。

| 文档 | 一句话定位 |
| --- | --- |
| [archive/](./archive/README.md) | 13 份历史过程文档（完成总结 / 修复报告 / 迁移记录） |
| [MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md) | 迁移到 Flavor 架构的指南（历史） |
| [DETAILED_MIGRATION.md](./DETAILED_MIGRATION.md) | 详细迁移记录（历史） |
| [BRANCH_COMPARISON.md](./BRANCH_COMPARISON.md) | 原 main / eink 分支对比（历史） |
| [WORK_LOG.md](./WORK_LOG.md) | Flavor 合并工作日志（历史） |

## 外部资料

| 文档 | 一句话定位 |
| --- | --- |
| [API Reference.md](../API%20Reference.md) | Miniflux 上游 API 文档快照，**非本项目 API 实现**；本项目接口见 `network/MinifluxApi.kt` |

---

## 「我想…」快速导航

| 我想 | 去哪 |
| --- | --- |
| 知道 AI 开发该遵守什么 | [AGENTS.md](../AGENTS.md)（尤其 §0 优先级、§6 硬约束、§7 禁区、§8 已知坑） |
| 构建某个变体 / 查命令 | [QUICK_BUILD_REFERENCE.md](./QUICK_BUILD_REFERENCE.md) → [BUILD_GUIDE.md](./BUILD_GUIDE.md) |
| 了解 Flavor 架构 | [BUILD_FLAVORS_GUIDE.md](./BUILD_FLAVORS_GUIDE.md) |
| 改阅读页 / 撞到 WebView 或旋转丢进度 | AGENTS.md §8 坑 12–16 → [KNOWN_PITFALLS.md](./KNOWN_PITFALLS.md) |
| 改 E-Ink 样式 / 提示条 / 星标 | AGENTS.md §8 坑 8 / 17–19 → [KNOWN_PITFALLS.md](./KNOWN_PITFALLS.md) |
| 发新版 / 打 tag / 推远程 | AGENTS.md §9 + §10.1（🚫 每次需用户明确授权）→ [VC_WORKFLOW.md](./VC_WORKFLOW.md) |
| 查 jj / git 命令与回退 | [VC_WORKFLOW.md](./VC_WORKFLOW.md) |
| 查两个变体的界面差异 | [UI_DIFFERENCES.md](./UI_DIFFERENCES.md) |
| 查平板适配 | [TABLET_ADAPTATION.md](./TABLET_ADAPTATION.md) |
| 查历史（为何这样设计） | L3 存档，**只看背景，不照抄配置** |

---

## 两个产品变体（速览）

| Flavor | applicationId | 目标设备 | 特点 |
| --- | --- | --- | --- |
| `standard` | `com.i.miniread` | 常规 Android 手机 / 平板 | 完整 MD3 主题、动态颜色、深色模式 |
| `eink` | `com.i.miniread.eink` | 电子墨水屏设备 | 高对比黑白、无动画、48dp 底栏 |

两个变体 applicationId 不同，**可同机共存**。E-Ink 的硬尺寸 / 样式红线见 AGENTS.md §0.2 与 §8。

---

## 常用命令（详见 L1）

```bash
./gradlew assembleStandardDebug        # 标准版 Debug
./gradlew assembleEinkDebug            # 墨水屏 Debug
./gradlew testStandardDebugUnitTest    # 标准版单测
./gradlew testEinkDebugUnitTest        # 墨水屏单测
```

产物：`app/build/outputs/apk/{standard,eink}/{debug,release}/`。完整命令表见 [QUICK_BUILD_REFERENCE.md](./QUICK_BUILD_REFERENCE.md)。

---

## 文档维护约定

- 分层以 AGENTS.md「文档地图」为准；新增文档必须先在地图里登记层级，再写内容。
- L1 文档改代码时同步；L2 顶部标注状态 / 快照日期；L3 一律加状态横幅，不逐字改写历史正文。
- 所有构建命令、版本号、硬约束只从 AGENTS.md 或 L1 引用，不要在 L2 / L3 里另立一套。
- 版本号唯一来源：`app/build.gradle.kts` 的 `appVersionName` / `appVersionCode`。

---

**最后更新**：2026-11-13（文档编排重排为四层；断链 `BUILD_CONFIGURATION_SUMMARY.md` 已移除）
**维护**：见 [AGENTS.md](../AGENTS.md) 文档地图
