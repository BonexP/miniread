# AGENTS.md — MiniRead 仓库 AI 开发指令

> 本文件是本仓库对 AI Agent 的**唯一权威指令**。
> 这里**只写机制与路径**：所有"具体取值"（真实实例地址、真实 feed id、Referer 规则等）都已改为**用户可配置**，不在文档中记录；需要时请按文中指引「在哪配、怎么改」。
> 路径一律为**仓库相对路径**；代码定位一律用**符号名 / 文件名**（`fun Xxx`、`object Yyy`），**不写行号**——行号随重构漂移，2026-11-13 复核时已清除全部易漂移的「约 Lx」。
> 原 `release/1.0.0` 发布线已于 2026-10-16 清理，其提交全部已合入 `main`，勿再引用。
> 本文刻意保持精简：§8 已知坑的**完整论证**见 [doc/KNOWN_PITFALLS.md](doc/KNOWN_PITFALLS.md)，§10 版本控制的**命令 / 回退 / 实测数据**见 [doc/VC_WORKFLOW.md](doc/VC_WORKFLOW.md)。这两个文件都是 L2 参考，**冲突时以本文件为准**。

---

## 0. 外部技能与工具的优先级（最优先；冲突即按本节裁决）

优先级：**本文件 > 第三方技能／工具自带的通用规范 > 通用最佳实践**。

第三方技能描述的是"通用 Android 项目应该长什么样"，本文件的 §6 / §7 / §8 记录的是"这个项目为什么不能那样"——后者全部来自真实回归。因此：

- 外部技能给出的**模板代码、尺寸量值、Gradle 配置**，与本文件冲突时**无条件以本文件为准**；
- 不得"折中"、不得"按技能改项目"、不得因为技能声称 `Required` / `Best Practice` 就放宽本文件的硬约束；
- 技能覆盖不到的细节才可采纳技能建议，且仍须通过本项目的构建与单测（`./gradlew assembleStandardDebug` + 双 flavor 单测）。

### 0.1 已核实的冲突（`android-native-dev` 技能）

核对日期 **2026-11-13**（首次核对 2026-10-21）。市场安装版：版本 `1.0.0`、skillId `skill_2053081331777183744`、`SKILL.md` 共 **889 行**，自 2026-10-21 安装后**未升级**。三处主张**仍成立、仍与本项目冲突**：

| 技能位置（2026-11-13 复核） | 技能主张 | 本项目规则 | 照做的后果 |
| --- | --- | --- | --- |
| `SKILL.md:111` / `:124` | 在 `productFlavors` 里写 `versionNameSuffix`，作为标准示范 | §6 →「版本号唯一来源」 | `buildType` 优先级更高，会污染 release 版本名 |
| `SKILL.md:742` | Bottom Nav 高度 **80dp** | §8 已知坑 第 8 条 → E-Ink 必须 **48dp** | 回落 Material3 默认值，复现 Level2 灰底（回归点 `3707759`） |
| `SKILL.md:68` | `android.enableJetifier=true` 列为必需配置 | `gradle.properties` 仅开 `useAndroidX` | AGP 8.4 下无收益，拖慢构建 |

**复核义务**：该技能为市场安装、**不会自动更新**。每次重装或升级后必须重新核对其 `SKILL.md` 的行号与主张；本表过期时**更新本表**，不要绕过本表。

### 0.2 硬约束速查（注入截断防线）

> **本文件较长，被工具自动注入时可能被截断**（实测截断点约在前 160 行，正文后半段未必进入上下文）。
> 因此把散落在 §6 / §7 / §8 / §9 / §10 的**硬数值与红线**在此再列一遍；凡涉及**版本号、E-Ink 尺寸、阅读页、发布流程**的改动，仍须**显式读取对应章节全文**，不得仅凭本速查或第三方技能建议判断。

| 主题 | 不可改的红线 |
| --- | --- |
| 版本号 | 唯一来源 `app/build.gradle.kts` 的 `appVersionName` / `appVersionCode`（当前 **1.2.2 / 122**）；**禁止** flavor 或 release 加 `versionNameSuffix`，仅 debug 允许 `-debug` |
| 文案 | 所有用户可见文案（含 `contentDescription` / Snackbar / Toast / 对话框）走 `res/values`（英）+ `res/values-zh`（中），两文件 key 逐字一致；**禁止 Kotlin 硬编码**；`stringResource()` 只能在 `@Composable` 作用域；不要定义 `app_name` |
| E-Ink 底栏 | `BottomAppBar` 必须 `Modifier.height(48.dp)` + `tonalElevation = 0.dp` + `contentPadding`（回归点 `3707759`） |
| 阅读页视口 | `template.html` 必须保留 `<meta name="viewport" content="width=device-width, initial-scale=1">` 与 `html { font-size: 6.4px; }`；`tablet_styles.css` **无条件注入**，媒体查询带 `(min-width:600px) and (min-height:480px)` / `840px` 高度守卫 |
| 阅读页主题 | `ReaderThemeResolver.isDarkMode(isEink, systemNightMode)` 是**唯一判据**，两处调用点都要走；`custom.css` 与 `customdark.css` 各保持 **0 处 `prefers-color-scheme`** |
| 阅读页排版（#19） | 排版表 `<style id="miniread-typo-css">` **只写尺寸、不写颜色**，位于主题表 / 平板表**之后**；调节**就地替换 textContent，绝不重建 HTML**（`loadDataWithBaseURL` 只在 `LaunchedEffect(htmlContent)`）；字号统一走 `WebSettings.textZoom`（手机 rem 基数 + 平板 px 两套模型，CSS 改字号会「手机变了平板没变」）；行宽用 `max-width`+`margin:0 auto` 不写死 width；调节后必须**重锚**（取改动前快照 → `SCROLL_LAYOUT_SETTLE_DELAY_MS` → `ReadingPositionResolver.resolve` → `scrollTo` 一次 + `scrollGuardUntil`），且**不把排版配置加进恢复 Effect 的 key** |
| 导航 | **单一 NavHost**（rail / 手机共用）；`if (useNavigationRail)` 只切 `AppNavigationRail`；**不要改回 if/else 两份** |
| 阅读页加载 | `loadDataWithBaseURL` **必须留在 `LaunchedEffect(htmlContent)`**；恢复 Effect 的 **key 只能是 `pageLoadTick` 与 `sizeSignature`** |
| 阅读位置 | `measureScrollSnapshot()` 的 `contentHeight` **不乘 `scale`**（与滚到底判定 `contentHeight * scale` 并存，改一处须同步另一处）；纯布局变化先过 `needsRestoreAfterLayoutChange`，正确则**绝不 `scrollTo`**；`onPageFinished` / `onRelease` **不许用 `scrollY == 0` 覆盖**已保存位置 |
| 设置页 | 编辑态必须 `rememberSaveable`；`shouldShowTopBar == false` 时 `contentWindowInsets = WindowInsets(0)`；选择器自绘 `Dialog` + `heightIn(max = 560.dp)` + `weight(1f, fill = false)`；帮助页用 `scrollToItem`（**不用** `animateScrollToItem`）；`?section=` 的 `navArgument` 必须 `defaultValue = ""` |
| 提示条 | 不要直接 `SnackbarHost(hostState)`（M3 默认反色 `InverseSurface`）；统一走 `ui/MinireadSnackbarHost.kt`；E-Ink 自绘 `Surface(RectangleShape, White, 0.dp, 0.dp, BorderStroke(2.dp, Black))` |
| 星标 | `Icons.Outlined.Star` 是实心；用 `Icons.Outlined.StarBorder`（来自 `material-icons-extended`，**版本由 Compose BOM 托管，不要写 version**）；**不要**自造 drawable |
| 存储 / 架构 | `DataStoreManager` 是唯一门面、**公开签名不可改**；token 走 `SecureTokenStore`；无 DI、无 Repository；flavor 差异用编译期同名文件替换 + `BuildConfig.IS_EINK` |
| 测试 | 纯逻辑抽 `util/` 并配纯 JVM 单测（禁 `android.*` / Robolectric）；两 workflow 都跑 `testStandardDebugUnitTest` + `testEinkDebugUnitTest`（当前 **232 例**）；测试全在 `app/src/test/` |
| 发布 / VCS | 🚫 **推送远程 / 打 tag / 建 Release 每次需用户明确授权**；tag 归 git（附注）；提交走 jj；**凡执行 git 写操作先 `git switch main`**；必须保留仓库级 `working-copy.eol-conversion = "input-output"` |

---

## 1. 项目概览

MiniRead 是一个基于 **Jetpack Compose + Material 3 (MD3)** 的 **Miniflux**（自建 RSS 服务）Android 阅读客户端。**单模块 `:app`**，采用 **MVVM**，**无 DI 框架、无 Repository 层**——ViewModel 直接调用网络单例。

### 技术栈

| 项 | 取值 |
| --- | --- |
| 语言 | Kotlin 1.9.10 |
| UI | Jetpack Compose，Compose BOM `2024.02.00`，`material3` 1.2.1 |
| 网络 | Retrofit 2.11.0 + Gson Converter + OkHttp（`logging-interceptor` 4.12.0） |
| 存储 | DataStore Preferences 1.0.0；`androidx.security.crypto` 1.1.0-alpha06（EncryptedSharedPreferences） |
| 已引入但未使用 | `androidx.preference`（preference-ktx 1.2.1） |
| 构建 | AGP 8.4.0，KSP `1.9.0-1.0.13`，`composeOptions` kotlinCompilerExtensionVersion `1.5.3` |

### 两个 flavor 对照表

| flavor | applicationId | app_name | 说明 |
| --- | --- | --- | --- |
| `standard` | `com.i.miniread` | MiniRead | 标准版 |
| `eink` | `com.i.miniread.eink` | MiniRead E-Ink | 墨水屏版；applicationId 不同，**可与标准版同机共存** |

- flavor 维度：`version`；`eink` 用 `applicationIdSuffix = ".eink"`（`app/build.gradle.kts`）。
- 每个 flavor 各自生成 `BuildConfig.FLAVOR_TYPE`（字符串）与 `BuildConfig.IS_EINK`（布尔）。

### SDK / 编译

| 项 | 取值 |
| --- | --- |
| namespace | `com.i.miniread` |
| minSdk / targetSdk / compileSdk | 25 / 34 / 34 |
| Java / JVM target | 1.8（`sourceCompatibility` / `targetCompatibility` / `kotlinOptions.jvmTarget`） |

### 版本号唯一来源

- **唯一来源**：`app/build.gradle.kts` 顶部的 `appVersionName` / `appVersionCode`（当前 **1.2.2 / 122**）。
- 不要在别处硬编码版本；`versionName` / `versionCode` 直接引用这两个 extra 值。

---

## 2. 常用命令

> Windows 上用 `./gradlew`（Git Bash）或 `gradlew.bat`；CI 在 Linux 上运行。

| 任务 | 命令 |
| --- | --- |
| 构建标准版 Debug | `./gradlew assembleStandardDebug` |
| 构建墨水屏 Debug | `./gradlew assembleEinkDebug` |
| 构建标准版 Release | `./gradlew assembleStandardRelease` |
| 构建墨水屏 Release | `./gradlew assembleEinkRelease` |
| 构建全部变体 | `./gradlew assemble` |
| 标准版单测 | `./gradlew testStandardDebugUnitTest` |
| 墨水屏单测 | `./gradlew testEinkDebugUnitTest` |
| 清理 | `./gradlew clean` |

产物路径：`app/build/outputs/apk/{standard,eink}/{debug,release}/`（文件名形如 `app-standard-debug.apk`、`app-eink-release.apk`）。

### CI

| 文件 | 触发 | 行为 |
| --- | --- | --- |
| `.github/workflows/build.yml` | push / PR 到 `main`、`develop`（+ 手动 dispatch） | 跑 `testStandardDebugUnitTest testEinkDebugUnitTest`，再出 **两个 debug APK** 并上传 artifact |
| `.github/workflows/release.yml` | 推 `v*` tag | 跑同样两个单测，解码 keystore，出 **两个 release APK**，重命名为 `MiniRead-<flavor>-<tag>.apk` 并发布 GitHub Release |

---

## 3. 目录地图

### source set

| source set | 内容 |
| --- | --- |
| `main` | 共享代码与资源；39 个 `.kt`（含 `util/ReaderTypography.kt`、`ui/ReaderTypographyPanel.kt`）+ `res/` + `assets/` |
| `standard` | 2 个 `.kt`（`eink/EInkConfig.kt`、`eink/EInkUtils.kt` 的标准版空实现）+ `res/values*/` |
| `eink` | 2 个 `.kt`（同名文件的墨水屏实现）+ `res/values*/` |
| `test` | 纯 JVM 单测（15 个文件 / **232 个用例**） |
| `androidTest` | **空目录**（仅 `java/` 空壳）；本项目测试全部在 `test/` |

### `app/src/main/java/com/i/miniread/` 包职责

| 路径 | 职责 |
| --- | --- |
| `MainActivity.kt`、`SettingsFragment.kt` | Compose 宿主 + 导航；`SettingsFragment` 为历史遗留（当前无入口，见 §8 坑 1） |
| `ui/` | 9 个 Screen（含 `HiddenSettingsScreen`、`SettingsHelpScreen`）+ 辅助文件（`FeedWhitelistPicker`、`SettingsHelpContent`、`MinireadSnackbarHost`）+ `ui/adaptive/`（平板适配）+ `ui/theme/`（配色 / 主题 / 字体） |
| `ui/example/` | `FlavorUsageExample.kt`（flavor 用法示例，非主流程） |
| `viewmodel/` | `MinifluxViewModel.kt`（唯一 ViewModel） |
| `network/` | `MinifluxApi.kt`（Retrofit 接口 + 数据类 + `RetrofitInstance`） |
| `util/` | 纯逻辑与存储门面：`DataStoreManager`、`SecureTokenStore`、`InterceptionConfig` / `InterceptionSettings` / `InterceptionRuntime`、`DomainHelper`、`FeedSorter`、`TimeWindow`、`ReadingPosition`、`ReaderTheme`（E-Ink 主题判据）、`ArticleHtml`（正文 HTML 组装）、`FeedSelection`、`InterceptionDiagnostics`、`InterceptionClipboardFormat`、`InterceptionExamples`、`ReaderTypography`（阅读排版纯逻辑 + Codec） |
| `eink/` | flavor 专属：`EInkConfig.kt`、`EInkUtils.kt`（`standard/` 与 `eink/` 各一份同名文件） |

### 「去哪找什么」索引

| 想找 | 去哪 |
| --- | --- |
| 应用入口 / 导航 / 顶栏 / 底部 tab | `MainActivity.kt` |
| 阅读页与底部操作栏 | `ui/ArticleDetailScreen.kt` |
| 阅读页可调排版（配置 / CSS / 面板） | `util/ReaderTypography.kt` + `ui/ReaderTypographyPanel.kt` + `assets/template.html` 的 `miniread-typo-css` |
| 网络接口与数据类 | `network/MinifluxApi.kt` |
| 设置存储（唯一门面） | `util/DataStoreManager.kt` |
| token 加密存储 | `util/SecureTokenStore.kt` |
| 定向拦截配置与生效逻辑 | `util/InterceptionConfig.kt` / `InterceptionSettings.kt` / `InterceptionRuntime.kt` / `DomainHelper.kt` |
| 定向拦截的站内帮助文档 | `ui/SettingsHelpScreen.kt` + `ui/SettingsHelpContent.kt`（文案在 `res/values*/strings.xml` 的 `settings_help_*`） |
| 订阅源白名单选择器 | `ui/FeedWhitelistPicker.kt` + `util/FeedSelection.kt`（纯逻辑） |
| 输入诊断（哪一项会被丢弃 / 为什么没生效） | `util/InterceptionDiagnostics.kt` |
| 纯逻辑工具与单测 | `util/` + `app/src/test/` |
| 注入阅读页的 CSS/HTML | `app/src/main/assets/` |
| 平板适配 | `ui/adaptive/` |
| 混淆规则 | `app/proguard-rules.pro` |
| 已知坑的完整论证 | [doc/KNOWN_PITFALLS.md](doc/KNOWN_PITFALLS.md) |
| jj / git 命令与回退 | [doc/VC_WORKFLOW.md](doc/VC_WORKFLOW.md) |

---

## 4. 模块职责

> 逐文件的定位见 §3 的包职责表与「去哪找什么」索引；本节的**关键红线**已分别收进 §8 已知坑，完整论证见 [doc/KNOWN_PITFALLS.md](doc/KNOWN_PITFALLS.md)。

| 组 | 文件 | 要点 |
| --- | --- | --- |
| 顶层 | `MainActivity.kt` | Compose 宿主：读 DataStore 初始化 Retrofit / ViewModel；`onKeyDown` 实现 **E-Ink 音量键翻页**（依赖 `activeWebView`）；`sealed class Screen` 定义路由；`MainContent` 里 `CenterAlignedTopAppBar` + 刷新 action + `BottomNavigationBar`；**单一 NavHost**。Manifest 声明 `configChanges`（旋转 / 分屏 / 深色切换**不重建**，保阅读进度），`windowSizeClass` 因此在 `MainContent` 内调 `@Composable` 的 `calculateWindowSizeClass` 实时重算（**勿挪回 onCreate 单次计算**）。 |
| 顶层 | `SettingsFragment.kt` | `PreferenceFragmentCompat`，读 `res/xml/preferences.xml`。**当前无入口**，见 §8 坑 1。 |
| `ui/` | 9 个 Screen | `Login` / `FeedList`（排序编辑、标记已读对话框）/ `CategoryList` / `EntryList`（按 feed 或 category）/ `TodayEntryList`（最近 24h）/ `SubFeed`（某分类下的订阅源）/ `ArticleDetail`（WebView 容器 + 底部操作栏，注入 `assets/` 的 HTML/CSS、阅读位置写入与恢复、`shouldInterceptRequest` 拦截点）/ `HiddenSettings`（连点顶栏标题 7 次进入）/ `SettingsHelp`（纯说明，不改配置）。 |
| `ui/` 辅助 | `FeedWhitelistPicker` / `SettingsHelpContent` / `MinireadSnackbarHost` | 弹窗多选、帮助页元数据（**只放资源 id**）、全应用统一提示条。 |
| `ui/adaptive/`、`ui/theme/` | `AdaptiveLayout.kt`（`AdaptiveLayoutHelper`：`isTablet` / `isLandscape` / `shouldUseNavigationRail` / `getContentMaxWidth`）、`NavigationRail.kt`（`AppNavigationRail`）、`Color.kt` / `Theme.kt` / `Type.kt` | 平板适配已完工（§7 禁区，非必要不改）。 |
| `viewmodel/` | `MinifluxViewModel.kt` | `AndroidViewModel`：全部 API 调用 + LiveData 状态（feeds/entries/categories/userInfo/unread 计数/selectedEntry）、已读 / 收藏 / 翻篇 / 排序。末尾 `FeedListViewModel` **疑似死代码**（§8 坑 5）。 |
| `network/` | `MinifluxApi.kt` | `interface MinifluxApi`（所有 `v1/...` 接口）+ 数据类 `Feed` / `Entry` / `Category` / `EntriesResponse` / `UserInfo` / `Icon` 等 + `object RetrofitInstance`（`initialize(baseUrl)` / `api`）。 |
| `util/` | 见 §3 | 关键约束：`DataStoreManager` **公开签名不可改**；`InterceptionClipboardFormat` 的 4 行 `key=value` **格式不得变动**（DataStore 被备份规则排除，这是唯一跨设备备份手段）；`InterceptionExamples` **不存任何硬编码地址**；`InterceptionDiagnostics` 切分只能调 `InterceptionSettingsCodec.splitEntries`，`validCount` 与 `isActive` 已由单测锁死。 |
| flavor 专属 | `standard` / `eink` 的 `eink/EInkConfig.kt`、`EInkUtils.kt` | 标准版空实现（`DISABLE_ANIMATIONS = false`、`isEInkDevice() = false`）；墨水屏实现（黑白配色、`snap()` 动画、基于厂商 / 型号关键字的启发式判断）。**两 flavor API 必须同名同签名**（§7）。 |

---

## 5. 常见任务定位路径（核心章节）

| 我要做 X | 改哪几个文件 |
| --- | --- |
| **加一个新页面** | `MainActivity.kt` 的 `sealed class Screen` + **单一 NavHost**（rail / 手机共用一份）+ 新增 `ui/XxxScreen.kt` |
| **加 / 改一条导航路由** | 同上。NavHost 只有**一份**；`if (useNavigationRail)` 只切换 `AppNavigationRail`，不切换 NavHost |
| **改定向拦截参数（域名 / 白名单 / Referer）** | 参数在应用内**隐藏设置页**（`ui/HiddenSettingsScreen.kt`）配置，页内「?」直达帮助页（`ui/SettingsHelpScreen.kt`，10 个小节）；白名单用弹窗勾选（`ui/FeedWhitelistPicker.kt`），底层仍写回逗号分隔文本。持久化在 `util/DataStoreManager.kt` 的 4 个 `intercept_*` key；生效逻辑在 `util/InterceptionRuntime.kt`（+ `InterceptionSettings.kt` 解析、`InterceptionConfig.kt` 判定）；消费点在 `ui/ArticleDetailScreen.kt`（判定 / 拦截 / 加 Referer） |
| **改 E-Ink 行为** | 编译期：改 `app/src/eink/...`（同名文件按 flavor 替换）；运行期：判 `BuildConfig.IS_EINK` |
| **加 / 改界面文案** | `res/values/strings.xml`（英文）+ `res/values-zh/strings.xml`（中文），**不要在 Kotlin 里硬编码**。**不要**定义 `app_name`——它由 flavor 的 `resValue` 生成。flavor 专属文案同理：`app/src/{standard,eink}/res/values/`（英文）+ `values-zh/`（中文） |
| **加依赖** | `gradle/libs.versions.toml` + `app/build.gradle.kts` |
| **加纯逻辑 + 单测** | 抽到 `util/` 纯函数 → 测试放 `app/src/test/java/com/i/miniread/`（纯 JVM，禁用 `android.*` 与 Robolectric，用 JUnit4） |
| **改阅读页排版（字号 / 行距 / 行宽 / 段落间距 / 字体 / 对齐）** | 配置与纯逻辑 `util/ReaderTypography.kt` + `util/DataStoreManager.kt` 的 6 个 `typo_*` key；CSS 注入 `assets/template.html` 的 `miniread-typo-css` + `ui/ArticleDetailScreen.kt` 的 `applyTypoCss` / 排版 Effect；调节面板 `ui/ReaderTypographyPanel.kt`；文案 `res/values*/strings.xml` 的 `reader_typography_*`。**红线见 §0.2 / §8 坑 20** |
| **改主题 / 配色** | `ui/theme/Color.kt`、`ui/theme/Theme.kt`、`res/values/{colors,themes}.xml`、flavor 的 `res/values/colors.xml` |
| **改 token / baseUrl 存储** | `util/DataStoreManager.kt`（门面，**公开签名不可改**）+ `util/SecureTokenStore.kt` |
| **改 API 接口** | `network/MinifluxApi.kt`（接口与数据类同文件） |
| **发新版本** | 改 `app/build.gradle.kts` 的 `appVersionName` / `appVersionCode` → 打 `v*` tag（§9，推送需授权） |

---

## 6. 约定规范（硬约束）

- **版本号唯一来源**：`app/build.gradle.kts` 的 `appVersionName` / `appVersionCode`。**不要**给 flavor 或 release buildType 加 `versionNameSuffix`（仅 debug 允许，值 `-debug`）。
- **签名**：本地无 keystore 时 release **自动回退 debug 签名**（由 `hasReleaseKeystore` 判定）；正式签名由 CI 处理（`release.yml` 解码 `KEYSTORE_BASE64`）。本地回退签名仅用于回归。
- **字符串外置（强制）**：**所有**用户可见文案一律走资源文件 —— 包括可见文本、`contentDescription`（无障碍）、`Snackbar`/`Toast` 提示、对话框标题与按钮。**禁止在 Kotlin 里硬编码中/英文字面量**。
  - 目录约定：`res/values` = **英文**（默认 / fallback），`res/values-zh` = 中文；flavor 专属文案放 `app/src/{standard,eink}/res/values` 与 `values-zh`。新增 key 必须**两个文件同时加**，key 名逐字一致。
  - `app_name` **不要**写进 `strings.xml` —— 它由 flavor 的 `resValue` 生成，因此**不随语言切换**（品牌名，属预期）。
  - ⚠️ `stringResource()` **只能在 `@Composable` 作用域调用**。在 `onClick`、`coroutineScope.launch {}`、`AlertDialog` 的 `confirmButton` 等 lambda 里必须先提取成 `val` 再用。
  - 导航标签不要硬编码：`MainActivity.kt` 的 `sealed class Screen` 用 `@StringRes labelRes`；`ui/adaptive/NavigationRail.kt` 的 `NavigationItem` 同。
- **纯逻辑抽 `util/` 并配纯 JVM 单测**；两个 workflow 都跑 `testStandardDebugUnitTest` + `testEinkDebugUnitTest`。测试禁止 `android.*` / Robolectric。
- **token 走 `SecureTokenStore`**（EncryptedSharedPreferences）；`DataStoreManager` 是唯一门面，**公开函数签名保持不变**。
- **flavor 差异**用**编译期 source set 同名文件替换** + 运行期 `BuildConfig.IS_EINK`；**不要**用运行时反射或字符串判断 flavor。
- **无 DI、无 Repository**：ViewModel 直接调用 `RetrofitInstance.api`；**不要**引入 Hilt/Koin/Dagger，除非用户明确要求。
- 明文流量由 `res/xml/network_security_config.xml` 显式允许（自建 Miniflux 常为局域网 http）；云备份排除见 `res/xml/backup_rules.xml` / `res/xml/data_extraction_rules.xml`。
- **版本控制**：本仓库已启用 jj（colocated 模式，见 §10）。凡涉及提交 / 改写历史 / tag / 分支 / 回退，**先读 §10 的纪律**，不要凭 git 习惯直接操作。

---

## 7. 禁区（不要动）

| 禁区 | 原因 |
| --- | --- |
| `util/InterceptionConfig.kt` 的判定语义（`contains` + 非空白保护） | 空串会让 `contains` 恒真 → "未配置" 变 "拦截一切"；非空白保护必须保留 |
| `app/src/main/assets/magick.css` | 保留资产（当前无引用），不要删除 |
| `ui/adaptive/` | 平板适配已完工，非必要不改 |
| flavor 的 `EInkConfig.kt`（`standard/` 与 `eink/` 两份） | 两 flavor API 必须同名同签名 |
| `SettingsFragment.kt` + `res/xml/preferences.xml` | 保留现状（当前无入口），不要据此新建设置入口 |
| `.workbuddy/` | 本机私有目录，已被 `.gitignore` 忽略 |
| `.jj/` | jj 的工作目录（colocated）。**不要删除、也不要写进 `.gitignore`**（jj 自动生成 `.jj/.gitignore`，git 已能忽略）。⚠️ 仓库级配置**不在**这里，删 `.jj` 也不会删配置（见 §10.1 第 1 条）；确需回退见 §10.3 |

---

## 8. 已知坑（逐条，全部已核实）

> 每条的**结论与硬数值**在此保留（注入截断也切不掉）；「为什么」、历史回归点、实测数据见 [doc/KNOWN_PITFALLS.md](doc/KNOWN_PITFALLS.md)。改动前**务必读完对应条目**。

1. **`SettingsFragment` 无任何入口**：Manifest 未注册宿主 Activity；应用主题是 platform 的 `android:Theme.Material.Light.NoActionBar`，**不支持 AppCompat**——直接用它做设置页会崩。新界面一律用 Compose。
2. `res/values/arrays.xml` 结构原先非法，会让 `ListPreference` 拿到空列表（已修复为合法 `<item>` 结构，数组名不变）。
3. `app/proguard-rules.pro` 中 keep 了**不存在的**包 `com.i.miniread.model.**`（数据模型实际都在 `network/` 包）。⚠️ 删任何规则前先核对当前文件：v1.2.1 已删过 3 条**全量** Compose keep，其下四组细分 keep 保留。改动必须回归 **release 包体 + 定向拦截 + 图片加载**（坑 10）。
4. `network/MinifluxApi.kt` 有**两个 `deleteFeed` 重载**（`Int` 返回 `Void`、`Long` **无返回类型**）。改动时注意区别。
5. `viewmodel/MinifluxViewModel.kt` 末尾的 `FeedListViewModel` **疑似死代码**：`async` 结果未被使用，且未被任何 UI 引用；删除前先 `grep` 全仓确认 0 引用。
6. `app/src/androidTest/` 为**空目录**——本项目测试**全部**在 `app/src/test/`（纯 JVM）。
7. **NavHost 已合并为一份**（✅ 2026-10-13 修复）：此前 rail / 手机各一份逐字相同的副本，分处 `if/else` 两分支 → 竖横屏切换销毁重建整个内容子树（WebView 重建、页面重载、滚动位置丢失、卡顿）。现为**单一 NavHost**，`if` 只切 `AppNavigationRail`。⚠️ **不要改回 if/else 两份**；加路由只需改一处。
8. `ui/ArticleDetailScreen.kt` 的 `BottomAppBar` **必须**在 E-Ink 下显式传 `Modifier.height(48.dp)` + `tonalElevation = 0.dp` + `contentPadding`，否则回落 Material3 默认 80dp + 系统栏 inset + Level2 灰底（历史回归点：提交 `3707759`，随 v0.3.4 发布）。
9. `interceptWebRequest` 走 OkHttp **自行重发请求**（复用 `interceptionHttpClient` 单例），异常时返回 `null` 让 WebView 回退默认加载——改动它**必须回归图片加载**。
10. 启用 R8（`isMinifyEnabled` + `isShrinkResources`，release 默认开启）后**必须回归定向拦截与图片加载**；出问题就把这两项置 `false`。
11. **i18n 是历史薄弱点**：登录页 / 分类列表 / 条目列表 / 文章详情底栏 / 底部导航 / 平板侧栏都曾把中英文字面量**硬编码在 Kotlin 里**；`standard`/`eink` 的 `strings.xml` 也曾把**中文放在默认 `values/`**。新增任何 UI 文案务必走 `stringResource` 并把 key 同时加进 `values` 与 `values-zh`。
12. **MainActivity 声明了 `android:configChanges`**（旋转 / 分屏 / 深色切换**不重建** Activity）：阅读页保留 WebView 实例与滚动进度的关键，**不要删**。配套：阅读页 `ArticleWebView` 的 `loadDataWithBaseURL` **必须留在 `LaunchedEffect(htmlContent)` 里**（仅内容变化时加载一次）——放回组合体主体会让每次重组（如收藏 toggle）都重载页面。新增依赖「配置刷新」的非 Compose 代码时，需自行在 `onConfigurationChanged` 处理。
13. **阅读位置持久化与恢复**（旋转不丢进度的兜底，改动前务必读完）：快照由 `recordScrollPosition()` 写入 ViewModel（`saveReadingPosition`），恢复由 `LaunchedEffect(pageLoadTick, sizeSignature)` 执行（`pageLoadTick` 在 `onPageFinished` 自增；`sizeSignature` 取 `LocalConfiguration` 的 `screenWidthDp/screenHeightDp`）。四条红线：
    - 该 Effect 的 **key 只能是 `pageLoadTick` 与 `sizeSignature`**，**不得**加入 `content` / `savedPosition` / `entryId`——否则收藏 toggle 等「仅重组、不重载」场景会误触发恢复、造成位置跳动（与坑 12 同源）。
    - **两类触发分开处理**：`pageLoadTick` 变化 = 真·页面加载 → 走比例恢复兜底；仅 `sizeSignature` 变化 = 纯布局变化（旋转/分屏）→ 必须先用 `ReadingPositionResolver.needsRestoreAfterLayoutChange`（文档**比例偏差**判据）判断原生落点是否已正确，正确则**绝不 `scrollTo`**。⚠️ 不要用「`scrollY > 0` 就放过」这种弱判据：旋转触发 CSS 重排，Chromium 可能只保留像素偏移而没做滚动锚定。
    - 强制补记（`onPageFinished` / `onRelease`）**不允许用 `scrollY == 0` 覆盖**已保存位置：页面刚加载完或 WebView 被重建时 `scrollY` 本就是 0，会抹掉待恢复的位置。
    - `measureScrollSnapshot()` 的 `contentHeight` **不乘 `scale`**（写入与恢复同口径）；滚到底判定里的 `contentHeight * scale` 是**既有且存疑**的口径，两者并存，改动其一必须同步另一处。
    另：`resolve()` 的贴底吸附只在 `saved.viewportHeight` 可信（`0 < viewport <= contentHeight`）时生效；阅读页视口自 v1.0.3+ 为**响应式**（坑 15），旋转会**真实重排**——这正是文档比例换算恢复机制存在的意义，不要新增弱判据或绕过 `needsRestoreAfterLayoutChange`。
14. **NavHost 子树重建问题**（✅ 2026-10-13 修复，见坑 7）：已通过合并为**单一 NavHost** 根治。**验收判据**：旋转后 logcat **不应**再出现 `Page finished loading`，而应出现 `Layout change: native position ok ... skip restore`。
15. **阅读页响应式渲染红线**（2026-10-13 起）：三条红线——
    - `assets/template.html` **必须**保留 `<meta name="viewport" content="width=device-width, initial-scale=1">` 与 `<style>html { font-size: 6.4px; }</style>` rem 基数（`6.4 = 16 × 0.4`）。⚠️ **删除 rem 基数会让全部 rem 字号放大 ~2.5 倍**，删除 viewport meta 会让页面退回固定布局视口、旋转不重排。
    - **禁止**在 Android 侧按 `screenWidthDp >= 600` 判定后**条件注入** `tablet_styles.css`：`loadHtmlContentAsync` 必须**无条件注入**（读取后走进程级缓存 `cachedTabletCss`）。
    - `assets/tablet_styles.css` 的媒体查询**必须**带高度守卫：`(min-width:600px) and (min-height:480px)`、`(min-width:840px) and (min-height:480px)`（排除手机横屏）。⚠️ 深色平板块**不在此文件**（已迁入 `customdark.css`，见坑 16）；**引用块例外**：`blockquote` 颜色不写在本文件，浅色在 `custom.css` 末尾平板断点块、深色在 `customdark.css` 基础规则。块内字号已全改 `px`，不依赖 rem 基数。
16. **E-Ink 正文主题红线**（2026-10-17 起，issue #12）：Compose 侧 E-Ink 恒用 `EInkLightColorScheme`，WebView 正文必须同步。两条红线——
    - **`util/ReaderTheme.kt` 的 `ReaderThemeResolver.isDarkMode(isEink, systemNightMode)` 是唯一判据**，两处调用点都必须走：`loadHtmlContentAsync`（首次注入）与 `ArticleWebView` 的 `LaunchedEffect(pageLoadTick, isDarkTheme)`（运行期就地刷新）。⚠️ 只改一处会被另一处覆盖。
    - **两份主题表都必须保持 0 处 `prefers-color-scheme`**（`custom.css` 与 `customdark.css` 各 `grep -c` 为 0）。理由：`themeCssFor()` 已按 `ReaderThemeResolver` 整份选表，CSS 再判一次是**第二个开关**，判据（`prefers-color-scheme`）与 Kotlin 的 `Configuration.uiMode` 不同源，`configChanges` 含 `uiMode` 时 WebView 可能沿用陈旧取值 → 深色表里的浅色基础规则露出来（issue #18：`#d1d1d1` on `#f0f0f0` = **1.34:1**）。深色值一律直接写在 `customdark.css` 基础规则里。⚠️ `magick.css` 也有 1 处，但未被注入且禁止删除（§7），不计入本条。
    - **跨表覆盖只走 `body` 前缀**（主题表在 `<head>` 中先于平板表，同优先级会被浅色基础样式覆盖）：`pre`/`code`/`th`/`td`/`a` 的深色平板取值留在 `customdark.css` 平板块、选择器带 `body`（0,0,2）。**引用块例外**：`tablet_styles.css` 的 `blockquote` 不声明任何颜色（只留布局），浅色平板配色在 `custom.css` 末尾平板断点块——⚠️ 该块必须放在基础 `blockquote` **之后**，否则被 `border-left` 简写重置回 `#ccc`。
17. **隐藏设置页 / 帮助页 / 订阅源选择器的红线**（2026-10-21 起）：**编辑态必须 `rememberSaveable`**（跳帮助页会离开组合，普通 `remember` 必丢四个字段）；`shouldShowTopBar == false` 时 `Scaffold` 的 `contentWindowInsets` 要置 `WindowInsets(0)`（否则二次消费状态栏高度多一条空白），⚠️ **不要**为了隐藏顶栏在 content 里写 `if/else` 包 `NavHost`（违反坑 7/14）；**选择器不能换成 `AlertDialog`**（M3 1.2.1 的 `text` 槽无内建滚动，`LazyColumn` 会撞 `infinite maximum height constraints`）→ 自绘 `Dialog` + `Surface.heightIn(max = 560.dp)` + `Box(weight(1f, fill = false))` + `DialogProperties(usePlatformDefaultWidth = false)`；**`feeds` 必须用 `observeAsState()` 的可空重载**（`fetchFeeds()` 失败会 `postValue(emptyList())`，给默认值就无法区分「尚未加载」与「已加载为空」），两种状态都**不许裁剪已选 id**（失效 id 在「可能已删除」分区显式展示并允许取消）；帮助页定位用 `scrollToItem`（瞬移），**不要** `animateScrollToItem`（E-Ink 拖影），`?section=` 的 `navArgument` 必须给 `defaultValue = ""`；诊断只能调 `InterceptionSettingsCodec.splitEntries`（`validCount` 与 `isActive` 已由单测锁死）；**E-Ink 只换样式、不重排**（2026-10-24 起，issue #16）：差异全走 `val eink = BuildConfig.IS_EINK` 条件参数，**不要**加分组卡片 / 改间距节奏 / 文案 / `rememberSaveable` / `contentWindowInsets`，固定取值：顶栏 `Modifier.height(40.dp)` + `titleMedium` + 返回/帮助图标 20dp、次要按钮走私有 `SettingsOutlinedButton`（0dp 直角 + `BorderStroke(2.dp, Black)` + `contentColor = Black`）、主 `Button` 黑底白字 + 全零 elevation、`OutlinedTextField` 只改边框/光标/容器色（**error 语义色保留 M3 默认**）、`Switch` 选中白钮黑轨 / 未选中黑钮白轨 + 黑边；⚠️ `AlertDialog` 没有 `border` 参数，黑框只能写 `Modifier.border(2.dp, Black, 与 shape 完全相同的 RoundedCornerShape(8.dp))`（落在 `BasicAlertDialog` 外层 `Box`，`Modifier.border` 先画内容后画描边；**圆角不一致会在四角露出方框**）；⚠️ `OutlinedTextFieldDefaults.colors(...)` 未传项保持 token 默认（`supportingText` 仍是 `onSurfaceVariant` 灰，不要「顺手」改成纯黑）。
18. **Snackbar 默认是「反色」，不是主题色**（2026-10-21 核实）：M3 1.2.1 `SnackbarTokens.ContainerColor = InverseSurface`、`SupportingTextColor = InverseOnSurface` → 深色主题下弹**浅色**条、E-Ink 恒浅色方案下反成**深色块**（违反恒浅色、残影重）。统一走 `ui/MinireadSnackbarHost.kt`，**不要**再直接写 `SnackbarHost(hostState)`；标准版用 `surfaceVariant` / `onSurfaceVariant`，E-Ink 走**自绘容器**（2026-10-24 定稿，issue #15）：`Surface(shape = RectangleShape, color = White, tonalElevation = 0.dp, shadowElevation = 0.dp, border = BorderStroke(2.dp, Black))` + 内层 `Row`（`start = 16.dp`，有 action 时 `end = 8.dp` 否则 16dp，文字 `bodyMedium` 上下 12dp，可选 `actionLabel` 走 `TextButton`），外层 `fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)`。
    - **为什么必须自绘**：① `Modifier.border(...)` 套在 M3 `Snackbar(snackbarData, modifier)` 上，会被该重载内部的 `modifier.padding(12.dp)` 推到容器**之外**（一圈悬浮黑框）；② `Snackbar` 的 `shadowElevation` 写死 `SnackbarTokens.ContainerElevation`（6dp），**无公开参数可覆盖**。两条都无法用参数绕过。
    - **视觉规则**：只用纯黑 / 纯白 + 直角 + 2dp 线（与阅读页底栏顶部分界线、列表卡片黑框同一套）；**不要**圆角 / 阴影 / 灰底 / 半透明——墨水屏上都会变成可见灰块或毛边。自绘版**不支持** `withDismissAction`（全仓 `showSnackbar(...)` 均为单参，要用需先补实现）；`SnackbarHost` 的淡入淡出仍属宿主内部动画，未改。
19. **E-Ink 星标的「空心」要用 `StarBorder`，不能用 `Icons.Outlined.Star`**（2026-10-24 起，issue #17）：`material-icons-core-android:1.6.1` 里 `outlined/StarKt` 与 `filled/StarKt` 的 path **逐行相同**（单闭合轮廓、无内孔），`Icons.Outlined.Star` 渲染就是实心星；core 里也**没有** `StarBorder`。
    - 正确写法：`imageVector = if (entry.starred) Icons.Filled.Star else Icons.Outlined.StarBorder`；`StarBorder` 来自 **`material-icons-extended`** —— `gradle/libs.versions.toml` 里 `androidx-material-icons-extended`（**版本由 Compose BOM 托管，不要写 version**），`app/build.gradle.kts` 里 `implementation(libs.androidx.material.icons.extended)`。
    - 代价已知：该库 classes 约 40MB，debug 包与增量构建变慢，release 由 R8 裁到只剩用到的图标。**不要**再为此自造 drawable / 自绘 Canvas。⚠️ 该库其它 outlined 图标是否有内孔**未逐个核实**；阅读页 7 键里只有星标依赖「镂空」表达状态，改别的图标前先在设备上看一眼。

20. **阅读页排版（issue #19，2026-11-13 起）**：六项配置（字号 / 行距 / 行宽 / 段落间距 / 字体族 / 对齐）全局生效、持久化、调节即时生效且不打断阅读位置。红线见 §0.2，逐条落地：
    - **不得重建 HTML**：`loadDataWithBaseURL` 仍只在 `LaunchedEffect(htmlContent)`；排版走 `<style id="miniread-typo-css">` 的 `textContent` 就地替换（`applyTypoCss`），首次注入为空槽。把排版拼进 `loadHtmlContentAsync` 会让 `content` 变化触发重载、阅读位置归零（坑 12 同源）。
    - **字号只走 `WebSettings.textZoom`**：手机 rem 基数（`html{font-size:6.4px}`）+ 平板 px 字号是两套模型；CSS 里改字号必然「手机生效、平板不动」。`fontScale = 100` 对应既有硬编码 `textZoom = 125`，默认值观感逐像素不变。`customdark.css` 的 `2rem` 已与 `custom.css` 的 `2.2rem` 对齐（此前深色比浅色小 10%）。
    - **排版表只写尺寸、不写颜色**，且必须位于主题表 / 平板表**之后**（后注入者胜出，才能覆盖平板 `max-width:800px/900px` 与 `line-height:1.8`）；行宽只用 `max-width` + `margin:0 auto`，不写死 `width`；`em` 相对元素字号、不依赖 rem 基数。颜色永远归 `custom.css` / `customdark.css`（避免 #18 双开关）。
    - **调节后必须重锚**：独立 `LaunchedEffect(pageLoadTick, typography)`，区分「新页面首次应用」与「用户调节」；后者取改动前 `measureScrollSnapshot()` → 应用样式 → `delay(SCROLL_LAYOUT_SETTLE_DELAY_MS)` → `ReadingPositionResolver.resolve` → `scrollTo` **一次** + `scrollGuardUntil`（防被误判滚到底而标记已读）。⚠️ **绝不把 `typography` 加进恢复 Effect 的 key**（坑 13）。
    - **入口在全局顶栏、面板不遮挡正文**（2026-11-13 改）：`Aa` 按钮位于 `MainActivity` 的 `CenterAlignedTopAppBar` 的 `actions` 里，**仅阅读页显示**（`isArticleDetail` 判定）；显隐状态 `showTypographyPanel` 提升到 `MainContent` 并用 `rememberSaveable`，经参数传给 `ArticleDetailScreen`。面板是顶栏下方**占空间的独立横条**（`Column` 的第一个孩子，正文 `Box` 用 `weight(1f)` 被往下推），**不再是 Box 叠层**。⚠️ 只增删面板这个兄弟节点，`ArticleWebView` 的组合位置必须恒定——把正文包进 `if/else` 会重建 WebView、重载页面（§8 坑 12/13）。底栏仍 standard 6 键 / E-Ink 7 键、E-Ink 仍 48dp；面板**无动画**、编辑态 `rememberSaveable`、E-Ink 只换黑白直角样式不重排；文案走 `reader_typography_*`（中英两文件 key 逐字一致）。
    - **面板宽屏两列 + 高度约束**（2026-11-13 改）：面板内用 `BoxWithConstraints`，`maxWidth >= 600.dp`（修长手机横屏 / 平板）时六个可调项**两列网格**排布，窄屏单列；六个 section 只在 `sections` 列表里定义一次、两种排布共用（避免两套控件逻辑漂移）。高度上限取 `(maxHeight * 0.65f).coerceIn(160.dp, 360.dp)` 并保留 `verticalScroll`，任何屏幕比例下都不溢出、内容可滚到底（此前硬编码 `heightIn(max = 360.dp)` 在横屏可用高度仅约 200dp 时会被裁剪）。
    - 六个独立 `typo_*` DataStore key + `ReaderTypographyCodec`（宽容 + clamp，脏值回落 DEFAULT）；`clearAll()` 用 `preferences.clear()` 自动清掉；`DataStoreManager` **只新增、不改既有公开签名**。
    - `assets/fonts/` 17.8MB 未被任何注入式 `@font-face` 引用（仅 `magick.css` 引用 Averia*，§7 禁删）；「衬线」选项用 CSS 系统字体族（`Georgia` 等）实现，**未启用 NotoSerifSC**，字体资产清理另列技术债。

---

## 9. 发布流程

1. 确认 `main` 上版本号（`app/build.gradle.kts` 的 `appVersionName` / `appVersionCode`）已更新。
2. 打 tag 并推送（`v*`，如 `v1.2.2`）→ 触发 `.github/workflows/release.yml`。该 workflow 只用 `github.ref_name`，**不依赖附注 tag**。
   ⚠️ **tag 由 git 打与推**（附注 tag，与 `v1.0.0`~`v1.2.2` 一致），命令见 §10.2。用 jj 推 tag 有 `stale info` 前置要求且会把附注降级，见 §10.1 第 4 条。
3. CI 产出 `MiniRead-standard-<tag>.apk` / `MiniRead-eink-<tag>.apk` 并发布 GitHub Release。
4. 本地无 keystore 时 release 回退 debug 签名，**仅用于本地回归**；正式发布走 CI（由 secrets 提供 keystore）。

> 🚫 **推送远程 / 打 tag 前必须获得用户明确授权。**

---

## 10. 版本控制工作流（Git + jj）与纪律

本仓库自 **2026-10-16** 起以 **colocated 模式**同时使用 git 与 **jj（Jujutsu 0.46.0）**：`.jj/` 与 `.git/` 同级共存。引入前已按「换行符 / 性能 / 发布链路 / 回退」四道闸门实测，结论为**适合本项目**（数据见 `.workbuddy/reports/jj-pilot-report-2026-10-16.html`，本机私有）。

**为什么本仓库能用**：jj 官方**不支持**的 submodule / Git LFS / git hooks / `.gitattributes` / partial clone / `git worktree`，本仓库**一个都没用到**。⚠️ 将来若引入其中任意一项，需重新评估 jj 的适用性。

> §10.2 命令表、§10.3 回退 jj、§10.4 其它坑的**完整内容与实测数据**已移到 [doc/VC_WORKFLOW.md](doc/VC_WORKFLOW.md)（L2 参考）。下面 §10.1 五条**硬性纪律必须留在本文**。

### 10.1 硬性纪律

1. ⚠️ **必须保留仓库级配置 `working-copy.eol-conversion = "input-output"`**（**不在 `.jj/` 内**：jj 0.38 起移出仓库，实际存于 `%APPDATA%\jj\repos\<config-id>\config.toml`，`<config-id>` 即 `.jj/repo/config-id` 的内容，本仓库当前为 `56a476adacc04af8cabf`）。自查：`jj config path --repo` 打印该路径、`jj config list --repo` 含该项；改配置用 `jj config edit --repo`，**不要**去 `.jj/repo/` 下找 `config.toml`。本机 `core.autocrlf=true`（磁盘 CRLF / 仓库 LF），该项**默认 `none`**：缺失时 `jj st` 会把 **115 个文件**全判为已修改（纯行尾差异），且 `gradlew` 有被写成 CRLF 的风险（破坏 Linux CI）。**重建 `.jj` 会生成新 config-id → 配置必然丢失**；换机器同理。重设并强制重读一次：`jj config set --repo working-copy.eol-conversion input-output` + `jj restore`（仅需一次）。修复后磁盘字节**零改动**。**不要**用 `git add --renormalize` 或新增 `.gitattributes` 绕过（jj 不读它）。
2. ⚠️ **发布 tag 归 git，jj 只读**：实测 `v1.0.0`~`v1.0.3` 在 jj 侧全部 `tracked=false`，是既有常态。建 / 移 / 删 / 推 tag 一律用 git；jj 侧只需推送后 `jj git fetch` 让 tag 可见。若坚持用 jj 推 tag，未先 fetch 会失败（`stale info`）；`--force` **不是**合法参数。不对称提示：**bookmark 重写后会自动 force-push，tag 不会**。
3. ⚠️ **jj 改写 `@`（`jj commit` / `jj edit` 等）会把 git 的 HEAD 置于 detached 状态** → 此时 `git fetch && git merge --ff-only origin/main` 会**静默失效**（只移动游离 HEAD，`refs/heads/main` 不前进）。**凡要执行写操作的 git 命令，先 `git switch main`**；`git log/diff/show/status` 等只读命令不受影响。实测 `git switch main` 之后不会被再次 detach——正确说法是「**每次 jj 改写 `@` 之后重新挂回一次**」。
4. **tag 归 git 独占**：`git tag -a` / `-d` / `-f` / `push` 是唯一路径（本仓库 `v1.0.0`~`v1.2.2` 均为附注 tag）。**禁止**用 `jj tag set` 动发布 tag（只能产轻量 tag，会把附注降级）；也**避免** `jj git push --all`（会把 `tracked=false` 的历史 tag 纳入候选）。发布链路不依赖附注 tag（`release.yml` 只用 `github.ref_name`），附注仅用于把发布说明写进注记。
5. **推送远程、打 / 移动 / 删除 tag、创建 Release 属公开不可逆动作**，每次均需用户明确授权（同 §9）。清理上游已消失的本地分支同理，且**必须先确认已完全合并**（见 §10.3）。

### 10.2 常用命令

> 命令表见 [doc/VC_WORKFLOW.md](doc/VC_WORKFLOW.md) §2。要点：提交走 `jj commit -m "..."`（无暂存区，改完即自动快照）；开新工作 `jj new main -m "意图"`；撤销 `jj undo` / `jj op restore <op-id>`；同步远端 `jj git fetch` → `jj rebase -d main`；发版打 tag 用 `git tag -a vX -m "..."` → `git push origin vX`；**tag 操作不要用 `jj tag set`**。

### 10.3 回退 jj（完全退回纯 git）

> 三行命令（`rm -rf .jj` + 清 `refs/jj/*` + `git switch main`）、82 个残留引用的清理、`%APPDATA%` 孤儿目录的处理，全部见 [doc/VC_WORKFLOW.md](doc/VC_WORKFLOW.md) §3。⚠️ 该操作会删除 `.jj/` 与本机 jj 配置目录，属不可逆，执行前须按 §7 确认并取得用户明确授权。

### 10.4 其它已核实的坑

> 见 [doc/VC_WORKFLOW.md](doc/VC_WORKFLOW.md) §4：① 本地 tag 与远端不一致时 `git fetch --tags` 报 `would clobber existing tag`；② colocated 下不要用 git 做提交 / 改写历史 / 切工作副本（会 divergent change）；③ `jj st` 实测约 991ms vs `git status` 约 748ms（工作区含 625MB / 7240 个已忽略构建文件），属预期行为。

---

## 文档地图

> 分层规则：**L1 权威**与代码同步维护，改代码必须同步；**L2 参考**允许滞后但不得与 L1 冲突（顶部标注快照/状态）；**L3 历史存档**只读、不保证与当前代码一致、**不得据此判断现状**；**外部资料**是上游快照，非本项目实现。
> 总入口：[doc/README.md](doc/README.md)。**唯一权威指令始终是 [AGENTS.md](AGENTS.md)**。

### L1 权威（与当前代码同步维护）

| 文档 | 说明 |
| --- | --- |
| `AGENTS.md` | 本文件，AI 开发指令（唯一权威） |
| `README.md` | 项目简介、功能、构建变体 |
| `CHANGELOG.md` | 变更日志 |
| `RELEASE_NOTES.md` | 发布说明 |
| `doc/BUILD_GUIDE.md` | 完整构建指南 |
| `doc/BUILD_FLAVORS_GUIDE.md` | Flavor 架构与开发指南 |
| `doc/QUICK_BUILD_REFERENCE.md` | 构建命令速查 |

### L2 参考（按需查阅，顶部标注状态）

| 文档 | 说明 |
| --- | --- |
| `doc/KNOWN_PITFALLS.md` | AGENTS.md §8 已知坑的完整论证（回归点 / 实测数据 / 推导） |
| `doc/VC_WORKFLOW.md` | AGENTS.md §10 的命令表 / 回退 jj / eol 与 tag 实测 |
| `TECH_DEBT.md` | 悬而未决的技术债清单（只记录不实施；🔒 禁区 / ✋ 需授权已标注） |
| `doc/TECHNICAL_SUMMARY.md` | 技术总结 |
| `doc/TABLET_ADAPTATION.md` | 平板适配 |
| `doc/UI_DIFFERENCES.md` | UI 差异 |
| `doc/QUICK_REFERENCE.md` | 代码片段与配置速查 |
| `doc/CHECKLIST.md` | 开发 / 测试 / 发布检查清单 |
| `doc/DEVICE_COMPATIBILITY.md` | 设备兼容性记录 |
| `QUICK_START.md` | 入门速览（命令以 L1 的 `QUICK_BUILD_REFERENCE.md` 为准） |

### L3 历史存档（不保证与当前代码一致）

- `doc/archive/` — 13 份历史过程文档（完成总结 / 修复报告 / 迁移记录），入口 [doc/archive/README.md](doc/archive/README.md)。
- 写于 2025-10 flavor 合并期、已加状态横幅：`doc/MIGRATION_GUIDE.md`、`doc/DETAILED_MIGRATION.md`、`doc/BRANCH_COMPARISON.md`、`doc/WORK_LOG.md`。⚠️ 含过期配置示范（flavor 级 `versionNameSuffix`、旧版本号），**勿照抄**，现行规则见 §6。

### 外部资料

- `API Reference.md`（仓库根目录）— Miniflux 上游 API 文档快照，**非本项目 API 实现**；本项目接口见 `network/MinifluxApi.kt`。
