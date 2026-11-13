# 已知坑：完整论证（AGENTS.md §8 展开）

> **本文是 [AGENTS.md](../AGENTS.md) §8 的展开**，不是第二份权威指令。冲突时**一律以 AGENTS.md 为准**。
> AGENTS.md 的 §8 保留每条的**结论与硬数值**（注入截断也切不掉红线）；本文保留「为什么」——历史回归点、实测数据、逐条推导。
> 本文属 **L2 参考**，不保证与当前代码逐行同步；改动前先复读 AGENTS.md 对应章节，涉及版本号 / E-Ink 尺寸 / 阅读页 / 发布流程时尤其如此。
> 定位一律用**符号名**（如 `fun ArticleWebView`），不要用行号：代码重构后行号必然漂移。

---

## 1. `SettingsFragment` 无任何入口

Manifest 未注册宿主 Activity（`AndroidManifest.xml` 只有 `MainActivity`）；且应用主题是 platform 的 `android:Theme.Material.Light.NoActionBar`（`res/values/themes.xml`），**不支持 AppCompat**——直接用它做设置页会崩。新界面一律用 Compose。

## 2. `res/values/arrays.xml` 结构

原先非法（`<string-array>array</string-array>`），会让 `ListPreference` 拿到空列表。已修复为合法 `<item>` 结构，数组名不变。

## 3. `proguard-rules.pro` keep 了不存在的包

`com.i.miniread.model.**`（数据模型实际都在 `network/` 包）。⚠️ v1.2.1 已删过 3 条**全量** Compose keep（`-keep class androidx.compose.**` 等，其下 ui/foundation/runtime/material3 四组细分 keep 保留），删任何规则前先核对当前文件内容，勿误删细分规则。改动必须回归 **release 包体 + 定向拦截 + 图片加载**（见坑 10）。

## 4. `network/MinifluxApi.kt` 有两个 `deleteFeed` 重载

一个 `Int` 返回 `Void`、一个 `Long` **无返回类型**。改动时注意区别。

## 5. `viewmodel/MinifluxViewModel.kt` 末尾的 `FeedListViewModel` 疑似死代码

`async` 结果未被使用，且未被任何 UI 引用。删除前先 `grep` 全仓确认 0 引用。

## 6. `app/src/androidTest/` 为空目录

本项目测试**全部**在 `app/src/test/`（纯 JVM），`androidTest/` 仅 `java/` 空壳。

## 7. NavHost 已合并为一份（✅ 2026-10-13 修复）

此前 rail 布局与手机布局各有一份**逐字相同的副本**，且分处 `if (useNavigationRail) {...} else {...}` 两个分支内，导致手机竖屏↔横屏切换时整个内容子树被销毁重建（阅读页 WebView 被重建 → 页面重载、滚动位置丢失、卡顿）。现为**单一 NavHost**，`if` 只切换 `AppNavigationRail`。⚠️ **不要改回 if/else 两份 NavHost**；加路由只需改一处。

## 8. E-Ink 底栏 `BottomAppBar` 必须显式 48dp

`ui/ArticleDetailScreen.kt` 的 `BottomAppBar` **必须**在 E-Ink 下显式传 `Modifier.height(48.dp)` + `tonalElevation = 0.dp` + `contentPadding`，否则会落到 Material3 默认的 80dp + 系统栏 inset + Level2 灰底。
**历史回归点**：提交 `3707759`，随 v0.3.4 发布。这也是 `android-native-dev` 技能 `SKILL.md` 主张「Bottom Nav 80dp」与本项目冲突的原因（见 AGENTS.md §0.1）。

## 9. `interceptWebRequest` 自行重发请求

走 OkHttp **自行重发请求**（复用 `interceptionHttpClient` 单例），异常时返回 `null` 让 WebView 回退默认加载——改动它**必须回归图片加载**。

## 10. 启用 R8 后必须回归定向拦截与图片加载

`isMinifyEnabled` + `isShrinkResources`（release 默认开启）。出问题就把这两项置 `false`。

## 11. i18n 是历史薄弱点

登录页、分类列表页、条目列表页、文章详情底部栏、底部导航栏与平板侧边栏都曾把中英文字面量**硬编码在 Kotlin 里**（中英混杂 → 中文设备显示英文 / 英文设备显示中文）；`standard`/`eink` 的 `strings.xml` 也曾把**中文放在默认 `values/`** 目录。新增任何 UI 文案时务必走 `stringResource` 并把 key 同时加进 `values` 与 `values-zh`（见 AGENTS.md §6）。

## 12. `MainActivity` 声明了 `android:configChanges`

旋转 / 分屏 / 深色切换**不重建** Activity：这是阅读页保留 WebView 实例与滚动进度（旋转不丢进度）的关键，**不要删**。与之配套，阅读页 `ArticleWebView` 的 `loadDataWithBaseURL` **必须留在 `LaunchedEffect(htmlContent)` 里**（仅在内容变化时加载一次）——放回组合体主体会让每次重组（如收藏 toggle）都重载页面。新增依赖「配置刷新」的非 Compose 代码时，需自行在 `onConfigurationChanged` 处理。

## 13. 阅读位置持久化与恢复（旋转不丢进度的兜底机制，改动前务必读完）

滚动快照由 `ArticleWebView.recordScrollPosition()` 写入 ViewModel（`saveReadingPosition`），恢复由 `LaunchedEffect(pageLoadTick, sizeSignature)` 执行（`pageLoadTick` 在 `onPageFinished` 自增；`sizeSignature` 取 `LocalConfiguration` 的 `screenWidthDp/screenHeightDp`）。四条红线：

- 该 Effect 的 **key 只能是 `pageLoadTick` 与 `sizeSignature`**，**不得**加入 `content` / `savedPosition` / `entryId`——否则收藏 toggle 等「仅重组、不重载」场景会误触发恢复、造成位置跳动（与坑 12 同源的回归）。
- **两类触发要分开处理**：`pageLoadTick` 变化 = 真·页面加载 → 直接走比例恢复兜底；仅 `sizeSignature` 变化 = 纯布局变化（旋转/分屏）→ 必须先用 `ReadingPositionResolver.needsRestoreAfterLayoutChange`（文档**比例偏差**判据）判断原生落点是否已正确，正确则**绝不 `scrollTo`**（WebView 原生保留的位置比比例换算更准，多动一次反而挪走）。⚠️ 不要用「`scrollY > 0` 就放过」这种弱判据：旋转会触发 CSS 重排，Chromium 可能只保留像素偏移而没做滚动锚定。
- 强制补记（`onPageFinished` / `onRelease`）**不允许用 `scrollY == 0` 覆盖**已保存位置：页面刚加载完或 WebView 被重建时 `scrollY` 本就是 0，会把待恢复的位置抹掉。
- `measureScrollSnapshot()` 的 `contentHeight` **不乘 `scale`**（写入与恢复必须同口径）；而滚动到底判定里的 `contentHeight * scale` 是**既有且存疑**的口径，两者并存是已知情况，改动其一必须同步另一处。

另：`resolve()` 的贴底吸附只在 `saved.viewportHeight` 可信（`0 < viewport <= contentHeight`）时生效，规避布局未稳定时的异常测量。
另：阅读页视口自 v1.0.3+ 起改为**响应式**（见坑 15），旋转/分屏会**真实重排**（`contentHeight` 随之变化）——这正是文档比例换算恢复机制存在的意义，恢复逻辑**不受影响**；不要因为「旋转必然重排」而新增弱判据或绕过 `needsRestoreAfterLayoutChange`。

## 14. NavHost 子树重建问题（✅ 2026-10-13 修复，见坑 7）

此前两份 NavHost 分处 `if (useNavigationRail) {...} else {...}` 两分支，手机竖屏↔横屏切换会销毁重建整个内容子树 —— 阅读页 WebView 被重建、页面重载（logcat 表现为每次旋转都出现 `Page finished loading`）、滚动位置丢失、并伴随明显卡顿。已通过**合并为单一 NavHost** 根治。
**验收判据**：旋转后 logcat **不应**再出现 `Page finished loading`，而应出现 `Layout change: native position ok ... skip restore`。

## 15. 阅读页响应式渲染红线（2026-10-13 起）

阅读页由「固定 ~980px 布局视口 + 缩放贴合」改为**真实设备宽度**的响应式渲染，三条红线：

- `assets/template.html` **必须**保留 `<meta name="viewport" content="width=device-width, initial-scale=1">` 与 `<style>html { font-size: 6.4px; }</style>` rem 基数（`6.4 = 16 × 0.4`，系数 0.4 即旧模型 WebView 宽 ÷ 980px ≈ 0.399）。⚠️ **删除 rem 基数会让全部 rem 字号整体放大 ~2.5 倍**，删除 viewport meta 会让页面退回固定布局视口、旋转不重排。
- **禁止**在 Android 侧按 `screenWidthDp >= 600` 判定后**条件注入** `tablet_styles.css`：`loadHtmlContentAsync` 必须**无条件注入**（读取后走进程级缓存 `cachedTabletCss`）。条件注入会导致旋转后不重排、手机横屏被误按平板限宽。
- `assets/tablet_styles.css` 的媒体查询**必须**带高度守卫：`(min-width:600px) and (min-height:480px)`、`(min-width:840px) and (min-height:480px)`。高度守卫用于排除 WebView 高度通常仅 200~350px 的**手机横屏**。⚠️ 深色平板块**不在此文件**——它已迁入 `customdark.css`（见坑 16），本文件只保留布局与浅色样式。**引用块例外**：`blockquote` 的颜色不写在本文件，浅色在 `custom.css` 末尾的平板断点块、深色在 `customdark.css` 的基础规则（见坑 16）。
- 是否生效完全交给 CSS 媒体查询，按**真实视口**（宽 = WebView 内容区宽度，CSS px = dp）决定；块内字号已全部改为 `px`，不依赖 rem 基数。

## 16. E-Ink 正文主题红线（2026-10-17 起，issue #12）

Compose 侧 E-Ink 恒用 `EInkLightColorScheme`，WebView 正文必须同步，否则系统深色下出现「白工具栏 + 深色正文」，墨水屏还会因深底加重残影。两条红线：

- **`util/ReaderTheme.kt` 的 `ReaderThemeResolver.isDarkMode(isEInk, systemNightMode)` 是唯一判据**。两处调用点都必须走它：`loadHtmlContentAsync`（首次注入）与 `ArticleWebView` 的 `LaunchedEffect(pageLoadTick, isDarkTheme)`（运行期就地刷新）。⚠️ 只改一处会被另一处覆盖——这正是修复前的状态。
- **两份主题表都必须保持 0 处 `prefers-color-scheme`**（`custom.css` 与 `customdark.css` 各自 `grep -c` 为 0；E-Ink 恒注入前者，standard 深色注入后者）。理由：`themeCssFor()` 已按 `ReaderThemeResolver` 整份选表，CSS 再判一次是**第二个开关**，而它的判据是 WebView 自己的 `prefers-color-scheme`，与 Kotlin 读的 `Configuration.uiMode` 不同源；`configChanges` 含 `uiMode`（Activity 不重建）时 WebView 可能沿用陈旧取值 → 媒体查询不命中，深色表里的浅色基础规则露出来（issue #18：`#d1d1d1` on `#f0f0f0` = **1.34:1**）。深色值一律直接写在 `customdark.css` 的基础规则里。⚠️ `magick.css` 也有 1 处，但该文件未被注入且禁止删除（AGENTS.md §7），不计入本条。
- **跨表覆盖只走 `body` 前缀**：主题表在 `<head>` 中**先于**平板表，同优先级会被 `tablet_styles.css` 的浅色基础样式覆盖，因此 `pre` / `code` / `th` / `td` / `a` 的深色平板取值留在 `customdark.css` 的平板块、选择器带 `body`（0,0,2）。**引用块例外**：`tablet_styles.css` 的 `blockquote` 不再声明任何颜色（只留布局），浅色平板配色在 `custom.css` 末尾的平板断点块——⚠️ 该块必须放在基础 `blockquote` **之后**，否则会被 `border-left` 简写重置回 `#ccc`。

## 17. 隐藏设置页 / 帮助页 / 订阅源选择器的红线（2026-10-21 起）

- **编辑态必须 `rememberSaveable`**：点字段旁「?」跳帮助页会让 `HiddenSettingsScreen` 离开组合，普通 `remember` 在返回时会把四个字段全部丢掉（必现，不是概率问题）。
- **主顶栏让位时必须同时清 inset**：`shouldShowTopBar == false` 时 `Scaffold` 的 `contentWindowInsets` 要置 `WindowInsets(0)`，否则内层 `Scaffold` 二次消费状态栏高度，标题上方多出一条空白。⚠️ **不要**为了隐藏顶栏在 content 里写 `if/else` 包 `NavHost`（违反坑 7/14）。
- **选择器不能换成 `AlertDialog`**：M3 1.2.1 的 `text` 槽没有内建滚动，`LazyColumn` 直接放进去会撞 `infinite maximum height constraints`。必须自绘 `Dialog` + `Surface.heightIn(max = 560.dp)` + `Box(weight(1f, fill = false))`，且 `DialogProperties(usePlatformDefaultWidth = false)` 才能让 `fillMaxWidth(0.92f)` 生效。
- **`feeds` 必须用 `observeAsState()` 的可空重载**：`fetchFeeds()` 失败会 `postValue(emptyList())`，给 `emptyList()` 默认值就无法区分「尚未加载」与「已加载为空」。两种状态都**不许裁剪已选 id**——失效 id 要在「可能已删除」分区里显式展示并允许主动取消。
- 帮助页定位用 `scrollToItem`（瞬移），**不要** `animateScrollToItem`（E-Ink 拖影）；`?section=` 的 `navArgument` 必须给 `defaultValue = ""`，否则不带参数进入会崩在参数校验。
- 诊断与解析必须同口径：`InterceptionDiagnosticsAnalyzer` 只能调 `InterceptionSettingsCodec.splitEntries`，**不要**另写 split；`FieldReport.validCount` 与 `InterceptionSettings.isActive` 已由单测锁死，改任一侧都要同步。
- **E-Ink 只换样式、不重排**（2026-10-24 起，issue #16）：所有差异走 `val eink = BuildConfig.IS_EINK` 的条件参数，**不要**加分组卡片、**不要**改间距节奏 / 文案 / `rememberSaveable` / `contentWindowInsets`。固定取值：顶栏 `Modifier.height(40.dp)` + `titleMedium`（与 `MainActivity` 的 E-Ink 顶栏一致）+ 返回/帮助图标均 20dp；次要按钮统一走私有包装器 `SettingsOutlinedButton`（0dp 直角 + `BorderStroke(2.dp, Black)` + `contentColor = Black`），主 `Button` 黑底白字 + 全零 elevation；`OutlinedTextField` 只改边框/光标/容器色，**error 语义色保留 M3 默认**（灰度屏靠深浅区分即可）；`Switch` 选中白钮黑轨、未选中黑钮白轨 + 黑边。
- ⚠️ `AlertDialog` 没有 `border` 参数，黑框只能写 `Modifier.border(2.dp, Black, 与 shape 完全相同的 RoundedCornerShape(8.dp))`：该 modifier 落在 `BasicAlertDialog` 的外层 `Box` 上（与内层 `Surface` 同尺寸），且 `Modifier.border` 先画内容后画描边，所以不会被白底盖住；**圆角不一致会在四角露出方框**。
- ⚠️ `OutlinedTextFieldDefaults.colors(...)` 未传的项保持 token 默认（实现是 `.copy(...)`，不会回落成 primary 紫灰），因此 `supportingText` 仍是 `onSurfaceVariant` 灰，与本页 `StatusBlock` 同阶，不要「顺手」改成纯黑。

## 18. Snackbar 默认是「反色」，不是主题色（2026-10-21 核实）

M3 1.2.1 源码 `SnackbarTokens.ContainerColor = ColorSchemeKeyTokens.InverseSurface`，`SupportingTextColor = InverseOnSurface`。后果：深色主题下提示条变**浅色**（像没跟随主题），E-Ink 恒浅色方案下反而成**深色块**（违反恒浅色、残影重）。统一走 `ui/MinireadSnackbarHost.kt`，**不要**再直接写 `SnackbarHost(hostState)`；标准版用 `surfaceVariant` / `onSurfaceVariant`，E-Ink 走**自绘容器**（2026-10-24 定稿，issue #15）：`Surface(shape = RectangleShape, color = White, tonalElevation = 0.dp, shadowElevation = 0.dp, border = BorderStroke(2.dp, Black))` + 内层 `Row`（`start = 16.dp`，有 action 时 `end = 8.dp` 否则 16dp，文字 `bodyMedium` 上下 12dp，可选 `actionLabel` 走 `TextButton`），外层 `fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)`。

- **为什么必须自绘**：① `Modifier.border(...)` 套在 M3 `Snackbar(snackbarData, modifier)` 上，会被该重载内部的 `modifier.padding(12.dp)` 推到容器**之外**，观感是一圈悬浮黑框（不是描边，也不是「12dp 错位」）；② `Snackbar` 的 `shadowElevation` 写死为 `SnackbarTokens.ContainerElevation`（6dp），**没有公开参数可覆盖**，纯白页面上必留灰晕。两条都无法用参数绕过。
- **视觉规则**：只用纯黑 / 纯白 + 直角 + 2dp 线，与阅读页底栏（顶部 2dp 黑线）、列表卡片（2dp 黑框）同一套线条语言；**不要**加圆角、阴影、灰底或半透明 —— 墨水屏上这些都会变成可见的灰块或毛边。
- 自绘版**不支持** `withDismissAction`（全仓 `showSnackbar(...)` 均为单参调用，要用需先补实现）；`SnackbarHost` 的淡入淡出仍在，属宿主内部动画，未改。

## 19. E-Ink 星标的「空心」要用 `StarBorder`（2026-10-24 起，issue #17）

`material-icons-core-android:1.6.1` 里 `outlined/StarKt` 与 `filled/StarKt` 的 path **逐行相同**（源码逐行比对过）——单个闭合轮廓、没有内孔，`Icons.Outlined.Star` 渲染出来就是实心星，墨水屏上「已收藏 / 未收藏」区分不出来；core 里也**没有** `StarBorder`。

- 正确写法：`imageVector = if (entry.starred) Icons.Filled.Star else Icons.Outlined.StarBorder`；`StarBorder` 来自 **`material-icons-extended`** —— `gradle/libs.versions.toml` 里 `androidx-material-icons-extended`（**版本由 Compose BOM 托管，不要写 version**），`app/build.gradle.kts` 里 `implementation(libs.androidx.material.icons.extended)`。
- 代价已知：该库 classes 约 40MB，debug 包与增量构建变慢，release 由 R8 裁到只剩用到的图标。为一个空心星引入它是权衡后的选择，**不要**再为此自造 drawable / 自绘 Canvas。
- ⚠️ 该库其它 outlined 图标是否有内孔**未逐个核实**；阅读页 7 键里只有星标依赖「镂空」表达状态，改别的图标前先在设备上看一眼。

## 20. 阅读页排版（issue #19，2026-11-13 起）

六项配置（字号 / 行距 / 行宽 / 段落间距 / 字体族 / 对齐）全局生效、持久化、调节即时生效且不打断阅读位置。AGENTS.md §8 条目 20 与 §0.2 保留了结论；这里记录**为什么这么设计**与取舍。

### 为什么不重建 HTML

`ArticleWebView` 的 `loadDataWithBaseURL` 必须留在 `LaunchedEffect(htmlContent)` 里（坑 12）：一旦把排版拼进 `loadHtmlContentAsync` 的输出，`htmlContent` 变化就会重载页面、阅读位置归零。因此排版走 `template.html` 里独立的 `<style id="miniread-typo-css">`（位于主题表 / 平板表**之后**，同优先级后注入者胜出），由 `applyTypoCss()` 用 JS 就地替换 `textContent`，与 `applyThemeCss()` 同一机制。首次加载时注入空槽，实际配置由页面加载完成后的排版 Effect 写入。

### 为什么字号走 textZoom 而不是 CSS

阅读页有**两套字号模型**：手机靠 `template.html` 的 `html { font-size: 6.4px }` rem 基数（坑 15），平板的 `body` / 标题在 `tablet_styles.css` 里全部写成 px。若只改 `html` 的 rem 基数或只改 body 的 `font-size`，必然出现「手机生效、平板不动」（或反之）。`WebSettings.textZoom` 是唯一同时命中两套模型的开关，且改动最小。

`fontScale = 100` 映射到既有的硬编码 `textZoom = 125`，因此**默认值下观感逐像素不变**；四档 85 / 100 / 120 / 150 分别对应 `textZoom` 106 / 125 / 150 / 188。顺带把 `customdark.css` 的 `font-size: 2rem` 与 `custom.css` 的 `2.2rem` 对齐——此前深浅两套表字号不一致，同一篇文章切到深色会小 10%（issue #19 背景点名）。

方案 B（把平板表 px 字号全改 rem/em、由 rem 基数统一驱动）更干净，但要重算整张平板表字号、回归面大，故不采用。

### 为什么排版表只写尺寸

颜色归 `custom.css` / `customdark.css`，Android 侧按 `ReaderThemeResolver` 整份选表（坑 16）。若排版表也写颜色，就回到 issue #18 的「同一属性两个开关、谁生效看媒体查询」的冲突。行宽只用 `max-width` + `margin: 0 auto`（不写死 width），只在超限时收窄；`em` 相对元素自身字号，不依赖 html 的 6.4px rem 基数；平板固定上限 800px / 900px 由后注入的排版表覆盖。

### 调节后为什么必须重锚

改字号 / 行距 / 行宽会真实改变 `contentHeight`（响应式渲染，坑 15），滚动锚点会漂移。流程：取改动前 `measureScrollSnapshot()` → 应用样式 → `delay(SCROLL_LAYOUT_SETTLE_DELAY_MS)` 等一次重排 → `ReadingPositionResolver.resolve(before, latest)` 按文档相对比例换算 → `scrollTo` **一次** → 设 `scrollGuardUntil` 防止被「滚到底」监听误判而把文章标记为已读（同坑 13）。

⚠️ **绝不把 `typography` 加进恢复 Effect 的 key**：恢复 Effect 的 key 只能是 `pageLoadTick` 与 `sizeSignature`（坑 13），排版有自己独立的 Effect；混在一起会让「仅重组、不重载」的场景误触发恢复。

### 入口与 E-Ink

底栏 standard 6 键 / E-Ink 7 键、E-Ink 48dp 是硬约束（坑 8 / issue #11），**不能加第 8 个图标按钮**。因此用内容区右上的 `Aa` 浮动按钮 + 内联面板（叠层，不占底栏）。面板无动画（直接 `if` 组合，E-Ink 上任何渐变都是可见闪烁）、编辑态 `rememberSaveable`（同坑 17）、E-Ink 只换黑白直角样式不重排、文案走 `reader_typography_*`（中英两文件 key 逐字一致）。

### 存储与字体资产

六个独立 `typo_*` DataStore key + `ReaderTypographyCodec`（宽容 + clamp：缺失 / 空串 / 非数字 / NaN / 越界一律回落 DEFAULT，绝不把脏值传给 WebView 或 Slider）。`DataStoreManager` **只新增、不改既有公开签名**；`clearAll()` 用 `preferences.clear()` 自动清掉这些 key。

`assets/fonts/` 17.8MB（含 14.8MB `NotoSerifSC-Regular.ttf`）当前**没有**任何注入式 `@font-face` 引用（只有未被注入且禁删的 `magick.css` 引用 Averia*，§7）。「衬线」选项因此用 CSS 系统字体族（`Georgia` / `Times New Roman` / `serif`）实现，**未启用 NotoSerifSC**；是否启用自定义字体、以及这 17.8MB 资产的清理，单列技术债 / 新 issue。

### 验收判据（issue 原文）

三档字号 × 手机/平板 × 明/暗共 12 组合都生效（不出现「手机变了平板没变」）；调节后不重载、位置偏差 ≤1 屏、不误标已读；行距 1.4 / 2.0 极端值下引用块 / 代码块 / 表格不重叠；退出重进 / 杀进程保留、`clearAll()` 回默认；E-Ink 无动画、底栏仍 48dp、无残影；中英文案齐全无硬编码；双 flavor 单测 + assemble 通过。
