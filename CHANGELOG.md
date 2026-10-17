# Changelog

All notable changes to MiniRead will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- 阅读页正文顶部注入元信息行（issue #13）：在 `<h1>` 与正文之间插入 `.miniread-meta`（`订阅源 · 发布时间`）。
  - 新增 `util/ArticleHtml.kt`：`buildContent()` 负责组装（标题 → meta → 正文），`formatPublishedAt()` 把 Miniflux 的 RFC3339 转本地时区 `yyyy-MM-dd HH:mm`（用 `java.text` 而非 `java.time`，`minSdk = 25` 且未开 desugaring），`escapeHtml()` 逐字符转义。
  - `ui/ArticleDetailScreen.kt`：正文拼接改走 `buildContent`；`remember` 的 key 扩到 `title / content / feedTitle / published_at`——漏了后两个会让 meta 永不重建，而加入 `starred` 或 `entry` 对象会让收藏 toggle 重载页面、丢阅读位置。
  - `assets/custom.css` / `assets/customdark.css`：新增 `.miniread-meta`，字号用 `0.8em` 自动跟随平板字号，无需改 `tablet_styles.css`。
  - 空值处理：标题为 null/空白时不输出 `<h1>`（原先会留一个空标题及其间距）；订阅源与时间任一缺失只显示存在项，两者都缺则整行不输出。
  - Compose 侧顶栏信息栏本轮未做，TODO 留在 HTML 注入点。
- E-Ink 阅读页恢复 7 键图标底栏（issue #11）：按「上一篇 / 下一篇 / 已读 / 未读 / 收藏 / 分享 / 外部打开」均分 48dp 底栏，收藏由 `Switch` 改为 `IconToggleButton`（实心/空心星，墨水屏只能靠形状区分状态），统一纯黑 tint，并补 `contentDescription`（新增 `article_action_add_bookmark` / `article_action_remove_bookmark`，中英同步）。standard flavor 的 6 键 + 弹性占位 + 开关布局逐行保持不变。
- 新增纯 JVM 单测 `util/ReaderThemeResolverTest`（4 例，覆盖 flavor × 系统深浅色真值表）与 `util/ArticleHtmlTest`（21 例，覆盖转义、时间换算与元数据省略规则），总用例数 121 → 146。

### Changed
- 阅读页主题 CSS 收敛为「Android 侧整份替换」单一机制（issue #12），消除与 WebView 自带 `prefers-color-scheme` 的双重判据：
  - `assets/custom.css`：删除 7 处 `@media (prefers-color-scheme: dark)` 与死分支 `body.dark-mode`，并清掉随之无人引用的 4 个 `--*-dark` 变量（−80 行）。该文件此后只服务浅色。
  - `assets/tablet_styles.css`：删除深色平板块（规则迁入 `customdark.css`），文件只保留布局与浅色样式。
  - `assets/customdark.css`：新增深色平板块，选择器带 `body` 前缀（主题表在 `<head>` 中先于平板表，同优先级会被平板基础样式覆盖），高度守卫保留；自身 6 处夜间媒体查询有意保留。
- `ui/theme/Theme.kt`：删除零引用的 `EInkDarkColorScheme`，让「E-Ink 恒浅色」只剩一条路径。

### Fixed
- E-Ink 版正文主题与界面不一致（issue #12）：界面恒浅色、正文却跟随系统 `uiMode`，系统深色下变成「白工具栏 + 深色正文」，墨水屏上深底还会加重残影。
  - 新增 `util/ReaderTheme.kt` 的 `ReaderThemeResolver.isDarkMode(isEink, systemNightMode)` 作为唯一判据，替换 `loadHtmlContentAsync`（首次注入）与 `ArticleWebView` 运行期就地刷新这两处各自为政的判定——此前只改一处会被另一处覆盖。
  - 运行期刷新的 `LaunchedEffect` key 由 `uiModeNight`（Int）改为解析后的 `isDarkTheme`（Boolean）：E-Ink 切换系统深浅色时不再触发一次无意义的整页 CSS 重设（墨水屏上是可见的闪烁）。
  - ⚠️ 仅改 Kotlin 判定不足以修复：`custom.css` 自带的夜间媒体查询会让 E-Ink 在系统深色下仍被染深，故同批清理（见 Changed）。
- 阅读页标题未做 HTML 转义：标题一直以 `"<h1>" + title + "</h1>"` 直接拼进 WebView HTML，含 `<` `>` `&` 的标题会被当作标签或破坏 DOM 结构。现统一走 `ArticleHtml.escapeHtml`；正文是 Miniflux 返回的 HTML，仍原样透传。
- E-Ink 底栏触控目标低于无障碍下限：`ActionButton` 在 E-Ink 下把尺寸压到 40dp（小于 Material3 的 48dp 最小交互尺寸）。改为 `Modifier.weight(1f).fillMaxHeight()` 均分 48dp 底栏（360dp 宽 ÷ 7 ≈ 51dp），并删除 `compact` 参数以免再次被压小。

## [1.0.3] - 2026-10-13

### Fixed
- 修复阅读页在旋转屏幕或 toggle 收藏后页面重载、阅读进度（滚动位置）重置的问题：
  - `ui/ArticleDetailScreen.kt`：`loadDataWithBaseURL` 原先在组合体主体中执行，任何重组（如收藏后 `selectedEntry` 更新触发）都会重载页面；现移入 `LaunchedEffect(htmlContent)`，仅在 HTML 内容变化时加载一次。
  - `AndroidManifest.xml`：MainActivity 声明 `configChanges`（`orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden|density|uiMode`），旋转与深色模式切换不再重建 Activity，WebView 实例与滚动位置得以保留。
  - `MainActivity.kt`：`windowSizeClass` 从 onCreate 单次计算改为在 `MainContent` 内随 `LocalConfiguration` 重算，避免 Activity 不重建后手机横屏失去底部栏 ↔ NavigationRail 自适应切换。
- 修复旋转屏幕后阅读位置仍归零的问题：`configChanges` 只保证了 Activity 不重建，但 WebView 在视图尺寸变化时缩放重排仍会丢失内部滚动位置。现新增阅读位置持久化与恢复机制——滚动快照按 `entryId` 存入 ViewModel（LRU 上限 50 篇），在「页面加载完成」与「窗口尺寸变化」两个时机按文档相对比例换算并恢复，兜住 WebView 内部丢失 / 重建 / Activity 重建等全部路径：
  - 新增 `util/ReadingPosition.kt`：`ScrollSnapshot` + `ReadingPositionResolver.resolve()` 纯函数（顶部早退 → 未布局保护 → 布局一致原样返回 → 贴底吸附 → 文档相对比例换算 + clamp）。
  - `viewmodel/MinifluxViewModel.kt`：新增 `saveReadingPosition` / `getReadingPosition` / `clearReadingPosition`，使用普通 `LinkedHashMap`（非可观察容器，避免滚动时每帧重组）。
  - `ui/ArticleDetailScreen.kt`：滚动中 / 页面加载完成 / 离开页面三处写入（节流），恢复前用 300ms 时间窗抑制「滚到底」误判，避免重复标记已读与弹出 Snackbar。
  - 新增单测 `ReadingPositionResolverTest`（17 个用例，纯 JVM），总用例数 93 → 110。
- **根治旋转重建**：`MainActivity.kt` 中两份完全相同的 NavHost 原先分处 `if (useNavigationRail) {...} else {...}` 两侧，手机竖屏（Compact）↔ 横屏（Expanded）切换时 `useNavigationRail` 翻转，导致整个内容子树被销毁重建——阅读页 WebView 每次旋转都被重建、页面重新加载（此前只能靠按比例恢复勉强兜住，落点必然偏移）。现合并为**单一 NavHost**，`if` 仅切换侧边栏，旋转时不再重建内容子树、不再重载，滚动位置由 WebView 原生保留（严格同一段文字）。
- 阅读页新增**布局变化守卫**：纯布局变化（旋转/分屏，页面未重载）时不再无条件按比例恢复——先用 `ReadingPositionResolver.needsRestoreAfterLayoutChange`（文档比例偏差判据）判断原生落点是否已正确，正确则不做任何 `scrollTo`；仅当原生位置丢失或偏差过大时才走比例兜底。
- `ReadingPositionResolver.resolve` 的贴底吸附增加 `viewportHeight` 可信性守卫（`0 < viewport <= contentHeight`），规避布局未稳定时异常测量导致的误贴底。
- 阅读页改为**响应式渲染**，修复旋转屏幕后正文贴左、右侧大片留白，以及手机横屏（宽 ≥600dp）被误按平板限宽的问题：
  - `assets/template.html`：补 `<!DOCTYPE html>`、`<meta charset>` 与 `<meta name="viewport" content="width=device-width, initial-scale=1">`，布局视口由固定约 980px 改为真实设备宽度（旋转后正确重排）；并设 `html{font-size:6.4px}` 作为 rem 基数，保持字号观感与旧版一致。
  - `ui/ArticleDetailScreen.kt`：移除 `loadHtmlContentAsync` 中按 `screenWidthDp>=600` 一次性判定平板的逻辑；`tablet_styles.css` 改为始终注入并纳入进程级缓存。
  - `assets/tablet_styles.css`：媒体查询改为 `(min-width:600px) and (min-height:480px)`（高度守卫排除手机横屏），块内 `rem` 统一改为 `px`。
  - `assets/custom.css` / `customdark.css`：修正标题层级（h2/h3 原先小于正文字号，现调整为大于正文）。

### Performance
- `ui/ArticleDetailScreen.kt`：移除每次重组都打印整篇文章的调试日志；正文 HTML 拼接改为 `remember(title, content)` 缓存；`AndroidView` 增加 `onRelease`（`stopLoading` / `loadUrl("about:blank")` / `destroy`），修复 WebView 从未被销毁导致的资源泄漏；删除无读取的死状态 `contentHeightState`。
- `network/MinifluxApi.kt`：`RetrofitInstance.initialize(baseUrl)` 幂等化（相同 baseUrl 直接返回），避免组合体每帧重建 Retrofit / GsonConverterFactory；`api` getter 改为缓存动态代理对象。

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

[1.0.3]: https://github.com/BonexP/miniread/releases/tag/v1.0.3
[1.0.2]: https://github.com/BonexP/miniread/releases/tag/v1.0.2
[1.0.1]: https://github.com/BonexP/miniread/releases/tag/v1.0.1
[1.0.0]: https://github.com/BonexP/miniread/releases/tag/v1.0.0
[0.3.4]: https://github.com/BonexP/miniread/releases
