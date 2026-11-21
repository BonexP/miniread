# Changelog

All notable changes to MiniRead will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.3.0] - 2026-11-22

### Changed
- **AI 开发规范复核与瘦身**（仅文档，无生产代码改动）：`AGENTS.md` 由 425 行精简至 210 行，§0–§10 编号不变；§8 已知坑完整论证外移到新增的 `doc/KNOWN_PITFALLS.md`、§10 命令/回退/实测外移到 `doc/VC_WORKFLOW.md`（两者均声明「冲突以 AGENTS.md 为准」）。新增 §0.2「硬约束速查」把 14 条硬数值前移，抵御工具自动注入截断；全仓易漂移的「约 Lx」行号改为符号名定位；§0.1 `android-native-dev` 技能冲突表按 2026-11-13 复核更新行号（`:111/:124`、`:742`、`:68`）。
- **文档编排重排四层**（不删、不移动任何文件）：`doc/README.md` 删除无效链接 `BUILD_CONFIGURATION_SUMMARY.md`，重写为 L1 权威 / L2 参考 / L3 历史存档 / 外部资料四层导航；`AGENTS.md` 文档地图同步分层。16 份文档补状态横幅；`doc/CHECKLIST.md` 的 flavor 级 `versionNameSuffix` 打勾改为「已取消」（现行规则见 §6）；`TECHNICAL_SUMMARY.md` / `BUILD_FLAVORS_GUIDE.md` / `BUILD_GUIDE.md` / `QUICK_BUILD_REFERENCE.md` 的 `1.1.0` 与 `v1.1.0` 示例更新为 1.2.2 或 `vX.Y.Z` 占位；`README.md` 去掉硬编码实例地址/Referer、修正「TodayEntryList 继承 EntryListScreen」等事实错误；`doc/archive/README.md` 补 13 份归档清单并修复 `FLAVOR_IMPLEMENTATION.md` 归档后失效链接。`API Reference.md` 标注为上游快照。
- 校验：全部 md 相对链接零断链；文档重排时的单测基线为 208（当前 232，见 §Added）。
- **阅读排版入口上提到全局顶栏、面板不再遮挡正文**：`Aa` 按钮从阅读页内容区的浮层移到 `MainActivity` 顶栏 `actions`（与刷新同一栏，仅阅读页显示）；显隐状态提升到 `MainContent` 并 `rememberSaveable`。面板改为顶栏下方**占空间的独立横条**（正文 `Box` 用 `weight(1f)` 被往下推），彻底不遮挡正文。`ArticleWebView` 组合位置保持恒定，展开/收起不重载页面、不丢阅读位置。
- **排版面板支持宽屏两列、高度自适应**：`BoxWithConstraints` 按可用宽度在 `maxWidth >= 600.dp` 时排成两列（修长手机横屏 / 平板），窄屏保持单列；高度上限改为 `(maxHeight * 0.65f).coerceIn(160.dp, 360.dp)` 并保留滚动。修复修长手机横屏时面板溢出可视区、观感错乱的问题（此前硬编码 `heightIn(max = 360.dp)`）。六个可调项只在 `sections` 列表定义一次，两种排布共用。
- **阅读排版面板 v2：形态改为覆盖式浮层、控件按档位数统一**（issue #19）：**推翻上一轮「顶栏下方占空间横条、零遮挡」决策**（坑 20），改为挂在正文 `Box` 尾兄弟节点的覆盖浮层——竖屏顶部下拉、横屏侧边滑出，方向性 docking + 静态「来源边」把手承载语义；**遮挡正文为主动取舍**，换取覆盖不挤走正文高度：避免平板横屏 WebView 被压到 <480px 跌破 `tablet_styles.css` 高度守卫而真实重排（坑 15）、手机横屏不再被压到仅数十 dp 可见高。浮层不用 Dialog / ModalDrawer（scrim 违反坑 18）、E-Ink 走瞬切无动画无 scrim、四形态用 `screenWidthDp > screenHeightDp` 分横竖与 `smallestScreenWidthDp >= 600` 分设备（勿复用 `screenWidthDp >= 600` 的 `isTablet()`）；`ArticleWebView` 组合位置恒定、恢复 Effect key 不含 `showTypographyPanel`/`typography`（坑 12/13）等坑 20 其余红线全部保留。控件按档位数统一：数值增减（`+ / 值 / −`）、两档（`Switch`）、多档（离散 `Slider(steps=3)`）；六字段名改符号 `A`/`↔`/`↕`/`¶`/`Aa`/`≡` 并用 `contentDescription` 补可访问名。同步 `AGENTS.md` §0.2 排版行 / §8 坑 20、`doc/KNOWN_PITFALLS.md` §20（记录决策回退理由、几何量化与新红线）。
- **排版浮层 v3：真机三症状修复 + MD3 打磨**（issue #19）：v2 静态评审/构建/单测全绿但模拟器鼠标真机暴露三问题——①浮层难滚/显示不全：根因是浮层壳祖先的 `pointerInput` 用默认 `PointerEventPass.Main`（父先子后）全量 consume 手势，掐断内层 `verticalScroll` 所需的 unconsumed 变化（鼠标滚轮同理被吞）；改为 `PointerEventPass.Final` 只消费 `!isConsumed` 残留，内层已消费的滚动不抢、空白处未消费的兜底吞掉不透传 WebView，滚轮随 Compose 1.6.1 `MouseWheelScrollNode` 原生恢复（不加滚轮桥接避免双重滚动）。尺寸：竖屏去 `min=280dp` 硬下限、上限放宽至可用高 0.75/0.70 且 `coerceAtMost(maxHeight)` 防溢出；横屏放宽宽度、两列阈值 600→480dp 使横屏可达两列。②行高/段距看不到 `+/−`：为①的下游表现（步进器代码完整、随滚动恢复自愈），未重写。③字号滑块反直觉：字号改 `+ / 档位文本 / −` 索引步进器、行宽改四档按钮（窄面板 `maxWidth<268dp` 降 2×2 防英文折行），数据链路（textZoom / buildCss / 重锚 / 恢复 Effect key）零改。控件追求 MD3 观感（standard 选中态 `primaryContainer`、E-Ink 纯黑白直角 2dp 框），48dp 触控 / 8dp 网格符合 `android-native-dev` 技能，技能冲突项（Bottom Nav 80dp / versionNameSuffix / Jetifier）按 §0 判给 AGENTS 不采纳。诊断报告见 `.workbuddy/reports/issue19-v3-diagnosis.md`。**待真机复测**：鼠标滚轮手感、Final-pass 真机滚动、四形态可视性（静态审查与源码分析已过，设备验证交用户）。
- **修复标准版排版面板 +/− 按钮「白底融白底」不可见**（真机反馈）：standard 分支的三处 `OutlinedButton`（行距/段距步进器、字号索引步进器、行宽四档按钮）此前用 M3 默认 `outlinedButtonColors()`（透明底）+ `border = null`，在浅色主题下按钮与面板白底同色、看不出是按钮。改为显式 `primaryContainer` 实色底 + `onPrimaryContainer` 字 + `outline` 1dp 描边，浅/深主题下均可辨；E-Ink 分支（白底黑字 2dp 黑框）不受影响。双 flavor 单测仍 232 例全绿。
- **排版浮层 v4：真机证据推翻 v3 两处误判，修复 `+`/`−` 字形不渲染与「显示不全」**（issue #19）：在 Medium_Phone(411×914dp) / Medium_Tablet(800×500dp) 双模拟器上 `assembleStandardDebug` + `uiautomator dump` + 像素采样实测：
  - **v3 误判 A（真 bug，两 flavor 全中）**：v3 判「行距/段距 `+/−` 看不到」是滚动被堵的下游表现、修滚动即自愈——**错**。真机语义树里 `text="+"` / `text="−"` 节点**根本不存在**，截图放大后按钮为纯 `primaryContainer` 填充 + 描边 + **内部全空**。根因：`Modifier.size(48.dp)` 已把宽度锁死 48dp，而 M3 1.2.1 `ButtonDefaults.ContentPadding` 是 horizontal 24dp+24dp → 内容可用宽 = 0 → `Text` 以 0dp 测量、字形不渲染（点击仍生效，实测把行距从「跟随默认」点成 `1.4`）。修法：两个步进器的 `OutlinedButton` 显式 `contentPadding = PaddingValues(0.dp)`，48dp 命中区不变。
  - **v3 误判 B（部分对）**：`PointerEventPass.Final` 只消费残留的修法**确实修好滚动**——实测拖曳与 `input mouse scroll` 均能在面板内滚动且像素 diff 不触及正文 WebView（**确认保留，不要回退**）；但「显示不全」另有独立成因：六段是「符号标题一行 + 控件一行」的两行结构（约 600dp），竖屏正文 Box 内一屏只见 3.5 段。修法：`TypographySection` 改为**单行**（符号固定 28dp 宽在左、控件 `weight(1f)` 在右，行高 ≥48dp），六段约 336dp 可一屏看全；比例 0.75/0.70 的上限本身经实测有效（无需改高度链）。
  - 附带收紧浮层内边距（`16/12` → `12/8`）与标题行 `TextButton` 的 `contentPadding`，让横屏 ~400dp 宽浮层里「阅读排版 / 恢复默认 / 关闭」同排放下。
  - 双 flavor 构建 + `testStandardDebugUnitTest` / `testEinkDebugUnitTest` 各 232 例全绿。诊断证据见 `.workbuddy/reports/issue19-v4-device-diagnosis.md`。
- **排版浮层 v5：按真机反馈重做交互范式**（issue #19）：五条用户反馈逐条消解，净减代码（删一个假把手、两个按设备分叉的比例分支、一个会降级 2×2 的控件与 88 行分段按钮）：
  - **① 删除直角灰色方块**（`TypographyDockHandle` 整个函数 + 调用点）。它同时错四次：直角 / 8dp 厚（M3 drag handle 是 32×4dp + 全圆角 pill）/ **不可拖** / **画在停靠边的反面**（竖屏 dock 在顶边却画底边、横屏 dock 在右边却画内侧左边，等于叫用户往反方向拉）。改成「合规 handle」也救不活——位置问题无解，且 M3 1.2.1 只有 `@ExperimentalMaterial3Api fun DragHandle(...)`（默认值在 `internal` 的 `SheetBottomTokens`，app 不可引用），该函数还自带 22dp 垂直 padding。
  - **② 字号档位文案改数值百分比**：`85% / 100% / 120% / 150%`（= `FONT_SCALES` 字面值，新增 `reader_typography_font_size_percent`，删掉 `_small/_normal/_large/_xlarge` 四个定性词 key）。**不显示 px**（默认档正文手机 17.6px / 平板 22.5px，任何「像字号」的整数必在一类设备上说谎），**不按设备动态算**（`tablet_styles.css` 判据是 WebView 视口、Kotlin 只有 `smallestScreenWidthDp`，会造第二个真相源——正是坑 16 的同源形态）。
  - **③ 行宽四档改 `+/−` 索引步进器**（删除 `TypographySegmentedButtons`，问题「手机横屏变 2×2」从代码里消失）：四等分后每档文本预算仅 42.59dp，英文 `Narrow`/`Medium` 自身就要 42.0–42.6dp，**系统字号 1.15× 时必然裁切**，阈值 268→188 只修默认字号那一档。索引步进器的文本槽 ≈90dp，**结构上不可能换行**，且与字号/行距/段距同构。已知取舍：一次只显示一档，看不到四档全貌。
  - **④ 行宽档位语义改单调**：`WIDTH_EM` 由 `[0, 24, 32, 40]` 改为 **`[0, 18, 21, 23]`**，新增展示序 **`WIDTH_STEP_ORDER = [1, 2, 3, 0]`** → UI 从左到右「窄 / 中 / 宽 / 全宽」，**「默认」这个词与 key 一起删除**（它既不是最小也不是最大，正是「选宽反而更窄」的根源）。存储语义不变（索引 0 仍 = 不限、默认态不输出 `max-width`，默认观感逐像素不变），旧持久化值 `0..3` 仍可解码（`clamp` 上限 `WIDTH_STEP_MAX` 不动，旧值只会更窄）。数值按通用单调性判据反解：`800/(18×1.88) = 23.64em`、`900/(19×1.88) = 25.20em` → 全局安全整数 23em（旧 40em 在平板横屏 = 950px > 样式表 900px 上限，是用户尚未报告的第二处反序）。
  - **⑤ 浮层定位统一 `align(Alignment.TopEnd)`**（贴正文内容区右上角、`Aa` 正下方）：删除 `isTablet` / `isLandscape` 两个判据与 0.55/0.62、0.70/0.75 四个比例分支，四形态共用一段 `widthIn(max = 560.dp)` + `heightIn(max = maxHeight)`。MD3 要求弹层 anchored to its anchor；旧横屏形态贴 `CenterEnd` 中右侧，与右上角 `Aa` 隔着正文脱节（用户「平板横屏浮层在右边不符合直觉」）。高度只封顶、不按比例（面板已内容包裹，比例上限只会制造空白或截断）；560dp 恰好让两列阈值 480dp 可达（536dp）。`PointerEventPass.Final` 吞触摸**原样保留**（v3/v4 真机验证的滚动 + 滚轮 + 不透传三者同时成立）。
  - 单测：`ReaderTypographyTest` 24 → 32 例（新增展示序单调 / 双向映射往返 / 越界回落 / 旧值兼容 / 通用单调性判据两 zone / CSS 展示序单调 / 字号百分比与 textZoom 自洽），双 flavor 各 **240 例全绿**。
- **排版行宽 v6：单位从 `em` 改为百分比**（issue #19，推翻 v5 的数值模型）：用户第二轮真机反馈——平板把行宽选到「宽」，正文依然很窄、「显示得非常不宽」。
  - **实测证据**（800×1280dp 平板、WebView 700dp、同篇同屏）：**全宽 = 667dp（95%）**，**宽(v5 23em) = 280dp（40%）**，一档之差 **2.4 倍**。四档按左留白严格单调递减（174 → 122 → 87 → 38dp），说明顺序对了但绝对位置全塌。
  - **根因**：`max-width` 加在 `body` 上，而 `em` 相对字号，在平板被四处叠加放大：① `tablet_styles.css` 的 `body{font-size:18px/19px}` 已比手机 `2.2rem×6.4px=14.08px` 大 28%；② 再乘 `textZoom`（默认 1.25）；③ WebView **text autosizing** 二次放大计算字号（实测反解每 em ≈ 24.3px，而 `18×1.25` 只有 22.5px）；④ `body` 自带 32px/48px padding 且 `box-sizing:border-box` 让 `max-width` **包含** padding。四条叠加后 em 的实际像素**既算不准也随设备/字号漂移**。v5 为防反序把上限压到 23em（`800/(18×1.88)=23.64em`）——保住了单调性，却把四档全推进很窄的区间。
  - **修法**：`WIDTH_EM = [0,18,21,23]`（em）→ **`WIDTH_PERCENT = [0,55,70,85]`（百分比）**，CSS 输出 `max-width:85%`。百分比相对包含块宽（WebView 视口），**只参与布局计算、完全不经字体渲染链路** → 四档在任何设备/任何字号下都稳定落在 55/70/85/100% 等比位置，**单调性天然成立**。
  - **顺带废弃**：v5 那套「按 tablet cap 反解安全界」的 zone800/zone900 计算与对应单测**不再需要**，替换为「显式档严格递增且都 < 100%」；另新增防回退单测 `width css uses percent not em`（一旦改回 em/px 立刻失败）。
  - 存储语义与展示序不变（`widthStep=0` 仍= 不限，`WIDTH_STEP_ORDER=[1,2,3,0]`，`ReaderTypographyCodec` clamp 仍 0..3，旧持久化值不崩）。单测双 flavor 各 **241 例全绿**（较 v5 的 240 净增 1 例防回退用例）。

### Added
- **UI 冒烟测试（第一张安全网）**：`app/src/androidTest/java/com/i/miniread/ui/ListItemSmokeTest.kt`，5 例——三个列表项（`CategoryItem` / `FeedItem` / `EntryItem`）渲染与点击回调 + 排版面板六段可访问名存在 + **`+/−` 字形存在（防 v4 回归）**。此前全仓 241 例单测**全是纯逻辑**（`util/` + `network/` 零 UI 覆盖），任何视觉改动都是盲改；本次 `connectedStandardDebugAndroidTest` 在 Medium_Tablet 上 **5/5 通过**。
  - 测试设计要点（跑通过程中踩到并已写进 AGENTS §0.2 / 坑 6）：① 仪器测试跟随**设备语言**，断言文案必须 `context.getString(R.string.xxx)`，写死中文会失败；② 面板超一屏且 `assertIsDisplayed()` 会强制真实绘制，在模拟器上易触发 `ReferenceQueueDaemon ... HardwareRenderer` 超时，改用纯语义查询 `assertExists()`；③ `Switch` 等 `MergeDescendants` 节点在合并树里会重复，须查 `useUnmergedTree = true`。
- **`ui/MiniCard.kt`：列表卡片双形态唯一实现**（standard / E-Ink 差异集中到一处）。此前 `CategoryItem` / `FeedItem` / `EntryItem` 各自手写同样的 `if (IS_EINK) CardDefaults.cardColors(White) + border(2.dp, Black)` 模式；收敛后三个屏只管内容，**`BuildConfig.IS_EINK` 分支全仓 56 → 47 处**。附带 `miniCardTitleColor()` / `miniCardSecondaryColor()` 收掉三处重复的标题/次要文字配色分支。
  - `EntryItem` 的**滑动露出背景层保留独立实现**：它不是列表卡片（padding 16dp vs 卡片 vertical 8dp、语义是"操作提示"而非"内容容器"），复用会改变滑动几何。

### Added（续）
- **阅读页可调排版**（issue #19）：顶栏「Aa」入口 + 调节面板，支持**字号（四档 85%–150%）/ 行距（1.4–2.0）/ 行宽（v5：窄 / 中 / 宽 / 全宽；初版为「默认 / 窄 / 中 / 宽」，见 §Changed 的 v5 条目）/ 段落间距（0.8–2.0em）/ 字体族（无衬线 / 衬线）/ 对齐（左 / 两端）**六项；全局生效、持久化（6 个独立 `typo_*` DataStore key）、调节即时生效且不打断当前阅读位置。
  - 新增 `util/ReaderTypography.kt`（纯逻辑：档位换算 / clamp / CSS 片段生成 / 宽容解析 Codec）与纯 JVM 单测 `ReaderTypographyTest`（初版 24 例，总用例 208 → 232；v5 增到 32 例 / 240）；新增 `ui/ReaderTypographyPanel.kt`（无动画、编辑态 `rememberSaveable`、E-Ink 黑白直角样式，文案 `reader_typography_*` 中英双份）。
  - `app/src/main/assets/template.html`：新增 `<style id="miniread-typo-css">`，位于主题表 / 平板表之后（后注入者胜出）；`ui/ArticleDetailScreen.kt`：新增 `applyTypoCss()` 就地替换样式、独立排版 Effect 做阅读位置重锚、`DataStoreManager` 新增 6 个 key 与读写函数（既有公开签名不变，`clearAll()` 自动清理）。
- `AGENTS.md` §8 新增坑 20（排版红线）、§0.2 速查表加排版行；`doc/KNOWN_PITFALLS.md` 同步 §20 完整论证。
- 新增根目录 `TECH_DEBT.md`：把悬而未决的技术债（proguard 死规则、`assets/fonts/` 17.8MB 未引用、文章刷新空壳、reader-header、字号双模型等）整理成索引，按「低风险可立即做 / 需单独确认 / 只记录不动」分组，并标注 §7 禁区与需授权项。**只记录不实施**；已登记到 `AGENTS.md` 文档地图与 `doc/README.md` 的 L2 参考层。

### Fixed
- 深色主题表 `customdark.css` 的正文字号 `2rem` 与浅色 `custom.css` 的 `2.2rem` 对齐：此前同一篇文章切到深色会小 10%（issue #19 背景点名）。
- 字号统一走 `WebSettings.textZoom`（`fontScale = 100` 对应既有 `textZoom = 125`），消除「手机 rem 基数 + 平板 px 两套模型，单改一侧出现手机变了平板没变」的隐患。

## [1.2.2] - 2026-11-03

### Fixed
- 深色主题下引用块（`blockquote`）底色仍是浅色、文字发灰，对比度最低 **1.34:1**（issue #18）。根因不是漏写一条颜色，而是 `customdark.css` 已经是深色专用表，却还在内部用 `@media (prefers-color-scheme: dark)` 做第二次明暗判断——Kotlin 侧按 `Configuration.uiMode` 整份选表，WebView 的媒体查询判据不同源（且 `configChanges` 含 `uiMode`，切深浅色不重建 Activity），两者不同步时深色表里的浅色基础规则就露出来。
  - `assets/customdark.css`：6 处夜间媒体查询全部删除，深色值直接写进基础规则（链接 / 引用 / 代码块 / 表格 / 表单 / 按钮）。引用块改为 `#2d2d2d` 底 + `#d1d1d1` 字 + `#8ab4f8` 左边框（9.02:1 / 6.53:1）；表格 `th` 边框随 `td` 一并由 `#ddd` 改为 `#555`；删除随之无引用的 4 个 `--*-light` 变量；平板块删除已无必要的 `body blockquote` 覆盖。
  - `assets/tablet_styles.css`：`blockquote` 只保留布局（`padding-left` / `margin` / `font-style`），去掉 `color: #555` 与写死的 `border-left` 颜色；浅色平板的 `#555` + `#1a73e8` 迁入 `custom.css` 末尾的平板断点块（高度守卫保留）。浅色与 E-Ink 观感逐像素不变。
  - 深色下引用块竖线在手机 / 平板两档统一为 `#8ab4f8`，不再随媒体查询命中情况漂移；浅色保留既有设计（手机 `#ccc`、平板 `#1a73e8`）。

## [1.2.1] - 2026-10-24

### Added
- 新增 `material-icons-extended` 依赖（版本由 Compose BOM 托管，不写 version），供阅读页使用 `Icons.Outlined.StarBorder`。

### Changed
- `app/proguard-rules.pro` 移除 `-keep class androidx.compose.** { *; }` / `-keep interface androidx.compose.** { *; }` / `-keepclassmembers class androidx.compose.** { *; }` 三条**全量保留**规则（其下 ui / foundation / runtime / material3 的四组细分 keep 原样保留）。原因：全量 keep 会把 `material-icons-extended` 整包留在 release dex 里——实测 9905 个图标类、实际只用 2 个，APK 从 16.6MB 涨到 19.9MB；收窄后回落。

### Fixed
- E-Ink 阅读页星标按钮在「已收藏 / 未收藏」两种状态下都显示实心星（issue #17）：`Icons.Outlined.Star` 来自 `material-icons-core`，该库的 outlined 与 filled 的 path **逐行相同**（单个闭合轮廓、无内孔），「空心」根本画不出来，包里也没有 `StarBorder`。现引入 `material-icons-extended`，未收藏改用 `Icons.Outlined.StarBorder`，已收藏仍用 `Icons.Filled.Star`。
- E-Ink 提示条边框几何错误（issue #15）：`MinireadSnackbarHost` 把 `Modifier.border(2.dp, Color.Black)` 传给了 M3 的 `Snackbar(snackbarData, modifier)`，该重载内部先 `modifier.padding(12.dp)` 再交给 `Surface`，边框于是画在整条容器**之外**，成一圈悬浮黑框；且 `Snackbar` 的 `shadowElevation` 写死为 6dp，纯白页面上必带灰晕。E-Ink 分支改为**自绘容器**：白底 + 黑字 + 直角 + `BorderStroke(2.dp, Black)` + tonal/shadow 均 `0.dp`，外层左右 12dp / 上下 8dp 留白，内层 16dp / 12dp 排版（可选 `actionLabel` 走 `TextButton`）。standard 分支逐字未改。
- 高级设置页在 E-Ink 上仍是标准圆角 / 灰调 / 带阴影组件（issue #16）：顶栏改 40dp + `titleMedium`、返回与帮助图标统一 20dp；5 个次要按钮改「直角 + 2dp 黑框」、主按钮黑底白字零投影；3 个 `OutlinedTextField` 的边框、光标、容器色统一黑白（error 语义色保留 M3 默认）；`Switch` 改「选中白钮黑轨 / 未选中黑钮白轨 + 黑边」；确认弹窗改 8dp 圆角、纯白容器、`tonalElevation = 0` 并补 2dp 黑框。页面结构、间距、文案、`rememberSaveable` 与诊断逻辑均未改动。

## [1.2.0] - 2026-10-21

### Added
- 隐藏设置页（顶栏标题连点 7 次进入）新增**站内帮助文档**，不必离开 App 查资料：
  - `ui/SettingsHelpScreen.kt` + `ui/SettingsHelpContent.kt`，10 个小节（机制说明 / 何时开启 / 四个条件 / 三个字段各自的匹配规则 / 生效时机 / 导出备份 / 常见错误 / 隐私范围），页头明确「本页只做说明，不改配置」。
  - 每个字段标题右侧新增「?」，通过 `settingsHelp?section=<id>` 直达对应小节；定位用 `scrollToItem` 瞬移（E-Ink 不用动画滚动）。
  - 文案全部外置到 `values` / `values-zh` 的 `settings_help_*`，两文件 key 逐字一致。
- 订阅源白名单改为**弹窗多选**（`ui/FeedWhitelistPicker.kt` + `util/FeedSelection.kt`）：列表显示标题与 `#编号`、支持按标题或编号搜索、已选计数、全选当前结果、清空全部；底层仍写回逗号分隔文本，导出/导入格式与既有单测不受影响。
  - 配置里存在、但当前订阅列表中没有的编号会进入「已选但不在订阅列表中（可能已删除）」分区，**不会被静默丢弃**，可主动取消。
  - `feeds` 用 `observeAsState()` 可空重载区分「尚未加载」与「已加载为空」，两种状态都不裁剪已选项，并提供「重新加载」。
- 行内诊断（`util/InterceptionDiagnostics.kt`）：实时给出每个字段「N 项有效 / M 项将被忽略」、被丢弃项原文与原因（中文标点、域名带 `https://`、编号非数字、规则缺 `=>`、规则被前面的关键字遮蔽等），并在配置不生效时逐条列出原因。
- 「用当前服务器填入示例」按钮：从当前 `baseUrl` 派生目标域名与 Referer 占位，订阅源编号保留用户已填值、否则取前 3 个已加载源（`util/InterceptionExamples.kt`）；已有输入时先弹确认框，填入后仍需手动保存。**不含任何硬编码地址**，避免私有实例地址随 APK 分发。
- 新增 4 个纯 JVM 单测文件（`FeedSelectionTest` 14、`InterceptionDiagnosticsTest` 26、`InterceptionClipboardFormatTest` 7、`InterceptionExamplesTest` 15），总用例数 146 → 208。

### Changed
- 导出 / 导入的 4 行 `key=value` 文本格式从 `HiddenSettingsScreen` 上移到 `util/InterceptionClipboardFormat.kt`，**行为逐字不变**（含「命中任一 key 即成功、未出现字段取空」的旧语义，已由单测锁定），便于回归与复用。
- `InterceptionSettingsCodec` 仅新增 `splitEntries(raw)`，供诊断复用同一套分隔逻辑；现有解析/序列化函数签名与语义零改动。
- 三个字段的 `supportingText` 由固定 hint 改为「错误优先 / 警告其次 / 否则 hint」三态；三个输入框新增 `placeholder`，`isError` 跟随诊断结果。
- 保存与「恢复默认」补 `try/catch`：写盘失败时 Toast 提示且不退出页面，不再让异常直接崩在协程里；保存时若有条目被丢弃，Toast 改为「已保存，N 项无效内容被忽略」。

### Fixed
- 阅读页「已标记为已读 / 已标记为未读 / 已收藏」等提示条配色不跟随界面主题：M3 默认用的是**反色**（`SnackbarDefaults.color` → `SnackbarTokens.ContainerColor` → `InverseSurface`），深色主题下反而弹出一条浅色提示，E-Ink 上则变成深色块。新增 `ui/MinireadSnackbarHost.kt` 统一着色：standard 走 `surfaceVariant` / `onSurfaceVariant`，E-Ink 走白底黑字 + 2dp 黑框、直角。
- 隐藏设置页与新增的帮助页会与主页顶栏叠成**两条 AppBar**：`MainActivity` 新增 `shouldShowTopBar`，隐藏时同步把 `contentWindowInsets` 置为 `WindowInsets(0)`，避免内层 `Scaffold` 二次消费状态栏高度而多出一条空白。
- 隐藏设置页四个编辑状态由 `remember` 改为 `rememberSaveable`：跳转帮助页会让该 Composable 离开组合，旧写法返回后会把刚改的内容全部丢掉。

## [1.1.0] - 2026-10-18

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

[1.2.2]: https://github.com/BonexP/miniread/releases/tag/v1.2.2
[1.2.1]: https://github.com/BonexP/miniread/releases/tag/v1.2.1
[1.2.0]: https://github.com/BonexP/miniread/releases/tag/v1.2.0
[1.1.0]: https://github.com/BonexP/miniread/releases/tag/v1.1.0
[1.0.3]: https://github.com/BonexP/miniread/releases/tag/v1.0.3
[1.0.2]: https://github.com/BonexP/miniread/releases/tag/v1.0.2
[1.0.1]: https://github.com/BonexP/miniread/releases/tag/v1.0.1
[1.0.0]: https://github.com/BonexP/miniread/releases/tag/v1.0.0
[0.3.4]: https://github.com/BonexP/miniread/releases
