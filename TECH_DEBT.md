# MiniRead 悬而未决技术债清单

> **层级**：L2 参考（允许滞后，**冲突时一律以 `AGENTS.md` 为准**）。
> **快照日期**：2026-11-13。
> **标记**：🔒 = AGENTS.md §7 禁区（不要动）；✋ = 需用户单独确认 / 单独授权；📄 = 仅文档改动；✅ = 仓库文档已明确登记为「遗留 / 未做 / 另列技术债」。
> **定位约定**：沿用 AGENTS.md「路径为仓库相对路径、代码定位用符号名 / 文件名、不写行号」。
> **分组**：§1 低风险可立即做 → §2 需单独确认 → §3 只记录不动 → §4 会话来源候选项（**非仓库证据，不纳入正式清单**）。
> **本文件只记录，不实施任何技术债。**

## 0. 证据来源与去重

- 来源：`AGENTS.md` §7 禁区与 §8 已知坑、`doc/KNOWN_PITFALLS.md`、`app/src` 下所有 `.kt` 的 `TODO`（共 5 处、0 处 FIXME）、`app/src/main/assets/fonts/` 体积与 `@font-face` 引用、`CHANGELOG.md` 各节的「未做 / 遗留」、`README.md`「未来改进」、`RELEASE_NOTES.md`「已知限制」。
- 去重合并：字体资产（AGENTS §8 坑 20 + KNOWN_PITFALLS §20 + CHANGELOG）→ B2；reader-header（源码 TODO + CHANGELOG 1.1.0）→ B4；文章刷新（3 处 TODO）→ B3；proguard（§8 坑 3 + KNOWN_PITFALLS §3 + CHANGELOG 1.2.1）→ B1；`deleteFeed` / `FeedListViewModel` / `androidTest` / `SettingsFragment` → A3 / A2 / §3.3 / §3.2。
- 仓库内**不存在**其它技术债 / backlog / roadmap / TODO / PLAN 类文件；`doc/KNOWN_PITFALLS.md` 是论证文档而非清单。本文件不复制其论证，只做索引。

## 1. 低风险可立即做

### A1 · `CategoryItem` 的 `unreadCount` 与 `Category.unreadCount` 重复 ✅
- **位置**：`ui/CategoryListScreen.kt` 的 `fun CategoryItem` / `fun CategoryListScreen`；`network/MinifluxApi.kt` 的 `data class Category`（`total_unread`）。
- **现状**：源码 TODO 原文指出二者重复。当前从 `categoryUnreadCounts` 映射单独传 `unreadCount` 给 `CategoryItem`，而 `Category` 已带 `unreadCount`。
- **建议**：先确认二者同源同义（前者来自 `fetchCategoriesUnreadCount()`，后者来自 `fetchCategories()` 的 `total_unread`）；确认后删掉 `CategoryItem` 的 `unreadCount` 形参，UI 直接读 `category.unreadCount`。
- **风险**：⚠️ 数据源不同，**未确认语义前直接删可能改变列表显示的未读数**；改后回归「分类列表未读计数」与「标记分类为已读」对话框。

### A2 · `FeedListViewModel` / `FeedState` / `FeedWithUnreadCount` 死代码 ✅
- **位置**：`viewmodel/MinifluxViewModel.kt` 末尾三个类型。
- **现状**：只读 grep 复核，三个类型在 `app/src` 下仅本文件内出现，0 处外部 / UI 引用；`loadFeedsAndCounters()` 的 `await()` 结果未被消费；`getUnreadCountForFeed()` 依赖的 `cachedCounters` 从未赋值。
- **建议**：连同三个类型一并删除，清掉因此不再需要的 import。
- **风险**：实施前**再跑一次全仓 grep**（含 `app/src/test/`、`app/src/{standard,eink}/`）；勿动上方 `saveReadingPosition` / `getReadingPosition` 等阅读位置逻辑。

### A3 · `MinifluxApi` 两个 `deleteFeed` 重载 ✅
- **位置**：`network/MinifluxApi.kt` 的 `interface MinifluxApi`。
- **现状**：`Int` 形参返回 `Void`、`Long` 形参**无返回类型**，同为 `@DELETE("v1/feeds/{feedId}")`；全 `app/src` 只有一处调用（`MinifluxViewModel.deleteFeed(feedId: Int)`），`Long` 重载无任何调用点。
- **建议**：删除未被调用的 `Long` 重载。
- **风险**：改前再次 grep 全部调用点；改后回归「删除订阅源」；`Long` 版本返回 `Unit` 与 `Int` 版本的 `Void` 不等价，**不要随手互换**。

### A4 · `CHANGELOG.md` `[Unreleased]` 内单测用例数口径 📄
- **位置**：`CHANGELOG.md` `[Unreleased]`。
- **现状**：曾同时出现 208 与 232 两个用例数（208 为文档重排时的基线）。
- **建议**：已改为「基线 208（当前 232，见 §Added）」，保持即可；后续新增测试注意同步 `AGENTS.md` §0.2 / §3。
- **风险**：不改生产代码；数字须与 `AGENTS.md` 一致。

## 2. 需单独确认 ✋

### B1 · `proguard-rules.pro` 仍 keep 不存在的 `com.i.miniread.model.**` ✅ ✋
- **位置**：`app/proguard-rules.pro`（`-keep class com.i.miniread.model.**` 与 `-keepclassmembers class com.i.miniread.model.**`）；实际数据模型在 `network` 包，已被 `-keep class com.i.miniread.network.**` 覆盖。
- **现状**：`app/src/main/java/com/i/miniread/model` 目录不存在；两条 `model.**` 规则仍在。
- **建议**：确认后**只删这两条**，保留四组细分 keep 与警示注释。
- **风险**：AGENTS §8 坑 3 要求删前核对当前文件；**改动必须回归 release 包体 + 定向拦截 + 图片加载**（坑 10）；⚠️ 勿误删四组细分 keep、勿加回 `-keep class androidx.compose.**` 全量规则（v1.2.1 包体教训）。

### B2 · `assets/fonts/` 17.8MB 未被任何注入式 `@font-face` 使用 ✅ ✋
- **位置**：`app/src/main/assets/fonts/`（19 个 `.ttf`）、`assets/magick.css`（唯一引用处）、`template.html`、`util/ReaderTypography.kt`。
- **现状**：合计 **17.8MB**，其中 `NotoSerifSC-Regular.ttf` 约 14.8MB；全 `assets/*.css` 的 19 处 `@font-face` **全部在 `magick.css`**（注入用的三份表 0 处），而 `magick.css` 从未被 Kotlin 读取注入；`fonts/` 下 0 个 `.woff2`。
- **建议**：✋ 二选一，需先决策——(a) 清理未被引用的字体资产减小 APK；(b) 为「衬线」档接入 `NotoSerifSC` 并加注入式 `@font-face`。任一方向先开新 issue 记录决策。
- **风险**：① **删 assets 属公开改动需单独授权**（来源为会话指令，仓库无成文条款）；② 🔒 **§7 禁区：`magick.css` 不得删除**，删字体后其 `@font-face` 会全部悬空（但它从不注入，运行期无影响）；③ 🔒 `magick.css` 那 1 处 `prefers-color-scheme` 按坑 16 明确「不计入」判据；④ 若选 (b)，回归面覆盖坑 15 / 20 全套。

### B3 · 文章内容刷新是空壳，刷新 action 三项增强未做 ✅ ✋
- **位置**：`viewmodel/MinifluxViewModel.kt` 的 `fun reloadArticleContent`；`MainActivity.kt` 顶栏刷新 action 的 `Screen.ArticleDetail.route` 分支。
- **现状**：`reloadArticleContent(entryId)` 只打日志 + 调 `loadEntryById`；KDoc 列的三种实现方案均未落地；三条体验增强（清缓存 / 加载动画 / 滚到顶）仍是注释。
- **建议**：✋ 先确认产品意图——补齐，或删掉 ArticleDetail 分支的刷新调用与两处 TODO，避免「点了像没反应」。
- **风险**：⚠️ 阅读页红线——`loadDataWithBaseURL` 必须留在 `LaunchedEffect(htmlContent)`、恢复 Effect 的 key 只能是 `pageLoadTick` 与 `sizeSignature`；「刷新后滚到顶」会与阅读位置恢复冲突，需明确优先级；E-Ink 的「加载动画」与无动画红线冲突，需单独设计。

### B4 · 阅读页 Compose 顶栏信息栏未做（reader-header）✅ ✋
- **位置**：`ui/ArticleDetailScreen.kt` 的 `TODO(reader-header)`；`util/ArticleHtml.kt` 的 `formatPublishedAt` / `buildContent`；`CHANGELOG.md` `[1.1.0]`。
- **现状**：订阅源 · 发布时间目前只注入到 WebView 正文的 `.miniread-meta`；Compose 侧顶栏信息栏明确「本轮不做」。
- **建议**：✋ 作为独立功能立项（按 TODO 自带要点：Scaffold 加 `topBar`、复用 `ArticleHtml.formatPublishedAt`、E-Ink 顶栏与 48dp 底栏同风格），或明确「永久不做」并改掉两处表述。
- **风险**：`shouldShowTopBar == false` 时 `contentWindowInsets = WindowInsets(0)`、⚠️ 不要为隐藏顶栏在 content 里写 `if/else` 包 NavHost（违反坑 7/14）；时间格式化必须复用 `ArticleHtml.formatPublishedAt`（minSdk 25、未开 desugaring，禁 `java.time`）。

### B5 · 阅读页字号「双模型」未统一（方案 B 被明确搁置）✅ ✋
- **位置**：`doc/KNOWN_PITFALLS.md` §20 末段；`template.html` 的 `html { font-size: 6.4px }`；`tablet_styles.css` 的 px 字号；`ui/ArticleDetailScreen.kt` 的 `applyTypoCss` / `WebSettings.textZoom`。
- **现状**：当前靠 `WebSettings.textZoom` 同时命中「手机 rem 基数 + 平板 px」两套模型；`fontScale = 100` 映射既有 `textZoom = 125`。「把平板表 px 全改 rem/em」的方案 B 已明确记录为「更干净但回归面大，暂不采用」。
- **建议**：✋ 只记录、暂不实施；确需重构平板字号体系时单独立项。
- **风险**：会触发坑 15 / 20 全套回归（rem 基数、媒体查询高度守卫、排版表后注入者胜出、`textZoom` 四档映射、调节后重锚）；`customdark.css` 的 `2rem` 已与 `custom.css` 的 `2.2rem` 对齐，勿回退。

## 3. 只记录不动

### 3.1 🔒 AGENTS.md §7 禁区逐项
| 禁区 | 原因 | 处理 |
| --- | --- | --- |
| `util/InterceptionConfig.kt` 判定语义（`contains` + 非空白保护） | 空串让 `contains` 恒真 →「未配置」变「拦截一切」 | 不立债务 |
| 🔒 `app/src/main/assets/magick.css` | 保留资产（当前无引用），不要删除 | 与 B2 绑定：可清理 `fonts/`，此文件必留 |
| `ui/adaptive/` | 平板适配已完工，非必要不改 | 不立债务 |
| flavor 的 `EInkConfig.kt`（两份） | 两 flavor API 必须同名同签名 | 不立债务 |
| 🔒 `SettingsFragment.kt` + `res/xml/preferences.xml` | 保留现状（无入口），不要据此新建设置入口 | 见 §3.2 |
| `.workbuddy/` | 本机私有目录，已 gitignore | 见 §4 |
| `.jj/` | jj colocated 工作目录，不要删除也不要写进 `.gitignore` | 回退 jj 属不可逆，需授权 |

### 3.2 🔒 `SettingsFragment` 无入口 + `androidx.preference` 依赖 ✅
- **位置**：`SettingsFragment.kt`、`res/xml/preferences.xml`、`res/values/arrays.xml`、`app/build.gradle.kts`。
- **现状**：Manifest 未注册宿主 Activity；应用主题不支持 AppCompat，直接用会崩；`arrays.xml` 结构问题已在 1.0.1 修复（坑 2 已了结）。依赖虽标「未使用」，但 Fragment 仍 import `PreferenceFragmentCompat`，**删依赖会连带要求删 §7 禁区文件**。
- **建议**：🔒 全部保持现状；新界面一律 Compose。

### 3.3 `app/src/androidTest/` 空目录 ✅
- **现状**：Git 不跟踪空目录，本机工作副本中未检出该目录，**不存在可删除的对象**。
- **建议**：保持「测试全部在 `test/`」约定；⚠️ 不要为补齐目录而新建。

### 3.4 已知限制 / 未来改进（产品路线，不排期）✅
- **位置**：`CHANGELOG.md` `[1.0.0]` Known Limitations、`README.md`「未来改进」、`RELEASE_NOTES.md`「已知限制」、`doc/BUILD_FLAVORS_GUIDE.md` Q5、`app/src/eink/.../EInkUtils.kt` 注释。
- **现状**：跨多份文档一致登记：暂无离线缓存、暂无推送通知、未接入墨水屏厂商 SDK（ONYX 等，`refreshScreen()` 为占位实现）、深色模式优化。
- **建议**：只登记，立项走新 issue。L3 存档里的 TODO 一律按文档地图「不保证与当前代码一致」处理。
- **注**：`RELEASE_NOTES.md` 的「配置不随云备份恢复」「应用名不随语言切换」**属设计预期**（DataStore 被备份规则排除、`app_name` 由 `resValue` 生成），**不是债务**。

### 3.5 授权类动作（不是债务）
- 推送远程 / 打 tag / 建 Release / 回退 jj 每次需用户明确授权（AGENTS §9 / §10.1 / §10.3）。

## 4. 会话来源候选项（非仓库证据，不纳入正式清单）

> ⚠️ 以下来源为会话记录或本机私有目录，**不是仓库文档**，**不纳入正式清单**，仅作线索登记。

- **C1 · 「删 assets 属公开改动需单独授权」**：来源为会话用户指令；仓库内检索不到对应条款（AGENTS §9 / §10.1 只覆盖推送 / tag / Release / 回退 jj）。B2 已按此写作前置条件。若要成为仓库规则，需在 AGENTS.md 补一条并同步 `doc/VC_WORKFLOW.md`。
- **C2 · `.workbuddy/memory/MEMORY.md` 的「遗留 / 未修」清单**：来源为本机私有目录（已被 gitignore、非仓库文档）。所列项（NavHost 未传 transition、部分 `LazyColumn` 缺 key、`MinifluxViewModel` 无 `isLoading`、`EInkConfig.getAnimationSpec()` 未被使用、`localizePublishTime` 的 API 下限、组合体内每帧调 `setCurrentEntryList`、KDoc / 测试夹具残留具体域名等）**部分在源码中有痕迹**，但「700ms 默认淡变」「缺 key」「可能崩」等结论仅见于该私有文件，**仓库文档未登记为技术债**。如要立项，需先取得仓库内可引用的证据（设备实测或依赖源码）。
- **C3 · 已核对、不属于本节者**：`AGENTS.md` §8 坑 1/3/4/5/6/20、`reloadArticleContent` 空壳、`CategoryListScreen` 的 `unreadCount` 重复、reader-header TODO、刷新视图注释——**全部已在仓库文件中有记录**，已分别归入 A1 / A2 / A3 / B1 / B2 / B3 / B4 / §3.2 / §3.3。
