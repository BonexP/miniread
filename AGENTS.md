# AGENTS.md — MiniRead 仓库 AI 开发指令

> 本文件是本仓库对 AI Agent 的**唯一权威指令**。
> 这里**只写机制与路径**：所有"具体取值"（真实实例地址、真实 feed id、Referer 规则等）都已改为**用户可配置**，不在文档中记录；需要时请按文中指引「在哪配、怎么改」。
> 路径一律为**仓库相对路径**；行号对应 `main` 分支当前工作区（原 `release/1.0.0` 发布线已于 2026-10-16 清理，其提交全部已合入 `main`，勿再引用），标注 `约 Lx` 的为近似值。

---

## 0. 外部技能与工具的优先级（最优先；冲突即按本节裁决）

优先级：**本文件 > 第三方技能／工具自带的通用规范 > 通用最佳实践**。

第三方技能描述的是"通用 Android 项目应该长什么样"，本文件的 §6 / §7 / §8 记录的是"这个项目为什么不能那样"——后者全部来自真实回归。因此：

- 外部技能给出的**模板代码、尺寸量值、Gradle 配置**，与本文件冲突时**无条件以本文件为准**；
- 不得"折中"、不得"按技能改项目"、不得因为技能声称 `Required` / `Best Practice` 就放宽本文件的硬约束；
- 技能覆盖不到的细节才可采纳技能建议，且仍须通过本项目的构建与单测（`./gradlew assembleStandardDebug` + 双 flavor 单测）。

### 0.1 已核实的冲突（`android-native-dev` 技能，核对日期 2026-10-21）

| 技能位置 | 技能主张 | 本项目规则 | 照做的后果 |
| --- | --- | --- | --- |
| `SKILL.md:105` / `:120` | 在 `productFlavors` 里写 `versionNameSuffix`，作为标准示范 | §6 约定规范 →「版本号唯一来源」 | `buildType` 优先级更高，会污染 release 版本名 |
| `SKILL.md:736` | Bottom Nav 高度 **80dp** | §8 已知坑 第 8 条 → E-Ink 必须 **48dp** | 回落 Material3 默认值，复现 Level2 灰底（回归点 `3707759`） |
| `SKILL.md:62` | `android.enableJetifier=true` 列为必需配置 | `gradle.properties` 仅开 `useAndroidX` | AGP 8.4 下无收益，拖慢构建 |

**复核义务**：该技能为市场安装、**不会自动更新**。每次重装或升级后必须重新核对其 `SKILL.md` 的行号与主张；本表过期时**更新本表**，不要绕过本表。

> **自指提示**：本文件较长，被工具**自动注入时可能被截断**（正文后半段未必进入上下文）。凡涉及**版本号、E-Ink 尺寸、阅读页、发布流程**的改动，必须**显式读取本文件对应章节**再动手，不得仅凭注入摘要或第三方技能建议判断。

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

- flavor 维度：`version`；`eink` 用 `applicationIdSuffix = ".eink"`（`app/build.gradle.kts` 约 L54-63）。
- 每个 flavor 各自生成 `BuildConfig.FLAVOR_TYPE`（字符串）与 `BuildConfig.IS_EINK`（布尔）。

### SDK / 编译

| 项 | 取值 |
| --- | --- |
| namespace | `com.i.miniread` |
| minSdk / targetSdk / compileSdk | 25 / 34 / 34 |
| Java / JVM target | 1.8（`sourceCompatibility` / `targetCompatibility` / `kotlinOptions.jvmTarget`） |

### 版本号唯一来源

- **唯一来源**：`app/build.gradle.kts` 顶部的 `appVersionName` / `appVersionCode`（当前 **1.2.0 / 120**，约 L6-7）。
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
| `main` | 共享代码与资源；36 个 `.kt`（含 `util/InterceptionSettings.kt`、`util/InterceptionRuntime.kt`、`ui/HiddenSettingsScreen.kt`、`ui/SettingsHelpScreen.kt`、`ui/FeedWhitelistPicker.kt`）+ `res/` + `assets/` |
| `standard` | 2 个 `.kt`（`eink/EInkConfig.kt`、`eink/EInkUtils.kt` 的标准版空实现）+ 2 个 `res/values/*.xml` |
| `eink` | 2 个 `.kt`（同名文件的墨水屏实现）+ 2 个 `res/values/*.xml` |
| `test` | 纯 JVM 单测（14 个文件 / 208 个用例，见下） |
| `androidTest` | **空目录**（仅 `java/` 空壳）；本项目测试全部在 `test/` |

### `app/src/main/java/com/i/miniread/` 包职责

| 路径 | 职责 |
| --- | --- |
| `MainActivity.kt`、`SettingsFragment.kt` | Compose 宿主 + 导航；`SettingsFragment` 为历史遗留（当前无入口，见 §8） |
| `ui/` | 9 个 Screen（含隐藏设置页与设置帮助页）+ 3 个辅助文件（`FeedWhitelistPicker.kt` 弹窗组件、`SettingsHelpContent.kt` 帮助页元数据表、`MinireadSnackbarHost.kt` 统一提示条）+ `ui/adaptive/`（平板适配）+ `ui/theme/`（配色/主题/字体） |
| `ui/example/` | 见 `example/FlavorUsageExample.kt`（flavor 用法示例，非主流程） |
| `viewmodel/` | `MinifluxViewModel.kt`（唯一 ViewModel） |
| `network/` | `MinifluxApi.kt`（Retrofit 接口 + 数据类 + `RetrofitInstance`） |
| `util/` | 纯逻辑与存储门面：`DataStoreManager`、`SecureTokenStore`、`InterceptionConfig`、`InterceptionSettings`、`InterceptionRuntime`、`DomainHelper`、`FeedSorter`、`TimeWindow`、`ReadingPosition`、`ReaderTheme`（E-Ink 主题判据）、`ArticleHtml`（正文 HTML 组装：转义 / 时间 / meta 行） |
| `eink/` | flavor 专属：`EInkConfig.kt`、`EInkUtils.kt`（在 `standard/` 与 `eink/` 各有一份同名文件） |

### 「去哪找什么」索引

| 想找 | 去哪 |
| --- | --- |
| 应用入口 / 导航 / 顶栏 / 底部 tab | `MainActivity.kt` |
| 阅读页与底部操作栏 | `ui/ArticleDetailScreen.kt` |
| 网络接口与数据类 | `network/MinifluxApi.kt` |
| 设置存储（唯一门面） | `util/DataStoreManager.kt` |
| token 加密存储 | `util/SecureTokenStore.kt` |
| 定向拦截配置与生效逻辑 | `util/InterceptionConfig.kt` / `util/InterceptionSettings.kt` / `util/InterceptionRuntime.kt` / `util/DomainHelper.kt` |
| 定向拦截的站内帮助文档 | `ui/SettingsHelpScreen.kt` + `ui/SettingsHelpContent.kt`（文案在 `res/values*/strings.xml` 的 `settings_help_*`） |
| 订阅源白名单选择器 | `ui/FeedWhitelistPicker.kt` + `util/FeedSelection.kt`（纯逻辑） |
| 输入诊断（哪一项会被丢弃 / 为什么没生效） | `util/InterceptionDiagnostics.kt` |
| 纯逻辑工具与单测 | `util/` + `app/src/test/` |
| 注入阅读页的 CSS/HTML | `app/src/main/assets/` |
| 平板适配 | `ui/adaptive/` |
| 混淆规则 | `app/proguard-rules.pro` |

---

## 4. 模块职责

### 顶层

| 文件 | 说明 |
| --- | --- |
| `MainActivity.kt` | Compose 宿主。`class MainActivity`（约 L73）读 DataStore 初始化 Retrofit/ViewModel（约 L80-108）；`onKeyDown`（约 L142）实现 **E-Ink 音量键翻页**（依赖 `activeWebView`，约 L132/L138）；`sealed class Screen`（约 L167）定义路由；`MainContent`（约 L180）里 `CenterAlignedTopAppBar`（约 L252）+ 刷新 action + `BottomNavigationBar`（约 L555）；**单一 NavHost**（约 L419；`if (useNavigationRail)` 只切换 `AppNavigationRail`，rail/手机共用同一份 NavHost，见 §8 坑 7/14）。Activity 在 Manifest 声明了 `configChanges`（旋转/分屏/深色切换**不重建**，保阅读进度），`windowSizeClass` 因此在 `MainContent` 内改调 `@Composable` 的 `calculateWindowSizeClass` 实时重算（约 L185-188，勿再挪回 onCreate 单次计算）。 |
| `SettingsFragment.kt` | `PreferenceFragmentCompat`，读 `res/xml/preferences.xml`（约 L7-11）。**当前无入口**，见 §8。 |
| `example/FlavorUsageExample.kt` | flavor 用法演示（如何判 `BuildConfig.IS_EINK`、用 `EInkConfig`/`EInkUtils`），可作参考，非运行时主流程。 |

### `ui/`（9 个 Screen）

| 文件 | 说明 |
| --- | --- |
| `ui/LoginScreen.kt` | 登录页，输入 baseUrl + API Token（约 L21）。 |
| `ui/FeedListScreen.kt` | 订阅源列表，含排序编辑/标记已读对话框（约 L56）；条目 `FeedItem`（约 L210）。 |
| `ui/CategoryListScreen.kt` | 分类列表（约 L39）；`CategoryItem`（约 L88）。 |
| `ui/EntryListScreen.kt` | 文章列表（按 feed 或 category）（约 L53）；`EntryItem`（约 L103）；`localizePublishTime`（约 L221）。 |
| `ui/TodayEntryListScreen.kt` | 「今天」列表，复用 EntryList 思路、只取最近 24h（约 L27）。 |
| `ui/SubFeedScreen.kt` | 某分类下的订阅源列表（约 L17）。 |
| `ui/ArticleDetailScreen.kt` | 阅读页。`ArticleDetailScreen`（约 L81）→ WebView 容器；`ArticleActionsBar`（约 L131，`BottomAppBar` 约 L161）；`ArticleWebView`（约 L310）注入 `assets/` 的 HTML/CSS，含 `shouldInterceptRequest` 拦截点、**阅读位置写入与恢复**（见 §8 已知坑 12/13）；`interceptWebRequest`（约 L600）用 OkHttp 自行重发请求。 |
| `ui/HiddenSettingsScreen.kt` | 隐藏高级设置页（顶栏标题连点 7 次进入）。**编辑态必须 `rememberSaveable`**（跳帮助页会离开组合，见 §8 坑 17）；保存前用 `InterceptionDiagnostics` 给出「N 项有效 / M 项将被忽略」；签名 `(navController, viewModel)`，feeds 走 `observeAsState()` 可空重载。 |
| `ui/SettingsHelpScreen.kt` | 设置帮助页。`?section=` 直达小节，用 `rememberLazyListState(initialFirstVisibleItemIndex)` + `scrollToItem`（**不用** animate，E-Ink 拖影）。纯说明，不改配置。 |
| `ui/SettingsHelpContent.kt` | 帮助页 `HelpSection` 枚举（id / titleRes / bodyRes / exampleRes）+ `IssueCode.messageRes()` / `InactiveReason.messageRes()`。**只放资源 id，文案在 strings.xml**。 |
| `ui/FeedWhitelistPicker.kt` | 订阅源白名单弹窗：搜索 + 多选 + 计数 + 全选/清空 + 「可能已删除」分区。自绘 `Dialog`（`AlertDialog` 的 text 槽不能放 `LazyColumn`），`Surface.heightIn(max = 560.dp)` + `weight(1f, fill = false)`。 |
| `ui/MinireadSnackbarHost.kt` | 全应用统一提示条。**不要**直接用 `SnackbarHost(hostState)`：M3 默认是反色（`InverseSurface`），深色主题下弹浅色条、E-Ink 下弹深色块（见 §8 坑 18）。 |

### `ui/adaptive/`、`ui/theme/`

| 文件 | 说明 |
| --- | --- |
| `ui/adaptive/AdaptiveLayout.kt` | `object AdaptiveLayoutHelper`（约 L15）：`isTablet` / `isLandscape` / `shouldUseNavigationRail`(windowSizeClass)（约 L60）/ `getContentMaxWidth`；另有 `NavigationType`、`ContentType` 枚举。 |
| `ui/adaptive/NavigationRail.kt` | `AppNavigationRail`（约 L28）：平板横屏侧边导航栏。 |
| `ui/theme/Color.kt` / `Theme.kt` / `Type.kt` | 配色、`MinireadTheme`、排版。 |

### `viewmodel/`、`network/`

| 文件 | 说明 |
| --- | --- |
| `viewmodel/MinifluxViewModel.kt` | `class MinifluxViewModel : AndroidViewModel`（约 L28）：全部 API 调用 + LiveData 状态（feeds/entries/categories/userInfo/unread 计数/selectedEntry）、已读/收藏/翻篇/排序。末尾 `class FeedListViewModel`（约 L592）**疑似死代码**，见 §8。 |
| `network/MinifluxApi.kt` | `interface MinifluxApi`（约 L20）：所有 `v1/...` 接口；同文件含数据类 `Feed`/`Entry`/`Category`/`EntriesResponse`/`UserInfo`/`Icon` 等；`object RetrofitInstance`（约 L232）持有 `initialize(baseUrl)` 与 `api`。 |

### `util/`

| 文件 | 说明 |
| --- | --- |
| `util/DataStoreManager.kt` | **设置存储唯一门面**（约 L21）。key 定义约 L27-35（含 4 个 `intercept_*`）；`setApiToken`/`getApiToken` 委托 `SecureTokenStore`（约 L61-92）；`getInterceptionRaw`/`saveInterceptionRaw`（约 L124-141）；`clearAll`（约 L144）。**公开签名不可改**。 |
| `util/SecureTokenStore.kt` | `object`（约 L17）：`MasterKey(AES256_GCM)` + `EncryptedSharedPreferences` 加密存储 token；`get`/`set`/`clear`。 |
| `util/InterceptionConfig.kt` | 定向拦截的**默认值 + 纯判定函数**（约 L13）：`DEFAULT_*` 全部中性化（关闭/空）；`matchesBaseUrl`（约 L38，`contains` + 非空白保护）；`matchesAnyBaseUrl`；`refererFor`（约 L53，顺序敏感）。 |
| `util/InterceptionSettings.kt` | `InterceptionRawConfig`（约 L6，对应 4 个 DataStore key）、`InterceptionSettings`（约 L16，已解析，`isActive`）、`InterceptionSettingsCodec`（约 L42，文本 ⇄ 配置的纯解析/序列化）。 |
| `util/InterceptionRuntime.kt` | `object`（约 L20）：拦截配置的**内存快照**（`StateFlow`），`load`（约 L35）/ `refreshBaseUrl` / `save` / `reset`；`isInterceptionActiveFor`（约 L59，纯函数）。 |
| `util/DomainHelper.kt` | `object`（约 L9）：`isTargetDomain(settings, baseUrl)`（纯判定）/ 无参重载走内存快照。 |
| `util/FeedSorter.kt` | `object`（约 L21）：`sort`（约 L32）纯排序函数（列表排序逻辑抽取，便于单测）。 |
| `util/TimeWindow.kt` | `object`（约 L15）：`publishedAfterForLast24h`（约 L29）「最近 24h」时间戳纯函数。 |
| `util/ReadingPosition.kt` | `ScrollSnapshot`（阅读位置快照，`scrollY`/`contentHeight`/`viewportHeight`，**均为 view px、不乘 scale**）+ `object ReadingPositionResolver.resolve(saved, current)`（约 L42）位置换算纯函数：顶部早退 → 未布局保护 → 布局一致原样返回 → 贴底吸附 → 文档相对比例换算 + clamp。配套 `ReadingPositionResolverTest`（17 个用例）。 |
| `util/FeedSelection.kt` | 白名单文本的纯逻辑：`parseCsv`（保序、丢非数字）、`toggle`（幂等、新项追加末尾）、`selectAll`、`clearAll`、`buildView`（`available` / `missing` 分流）。**失效 id 不受搜索影响**。 |
| `util/InterceptionDiagnostics.kt` | 三字段诊断：`FieldReport`（`rawCount` / `validCount` / `errors` / `warnings`）、`IssueCode`、`InactiveReason`。红线：切分只能调 `InterceptionSettingsCodec.splitEntries`；`validCount` 必须等于对应 `parseXxx` 结果数；`isActive` 必须等于 `InterceptionSettings.isActive`（单测已锁）。 |
| `util/InterceptionClipboardFormat.kt` | 导出/导入的 4 行 `key=value` 文本格式（从 `HiddenSettingsScreen` 上移，行为逐字不变）。**格式不得变动**——DataStore 被备份规则排除，这是唯一的跨设备备份手段。 |
| `util/InterceptionExamples.kt` | 「用当前服务器填入示例」的派生逻辑：`parseServer(baseUrl)` 取 scheme/host（宽容处理无 scheme、带端口/路径/userinfo、IPv6），`build(baseUrl, currentFeedIds, availableFeedIds)` 生成域名 / 编号 / Referer 占位。**不存任何硬编码地址**——避免私有实例地址随 APK 分发。 |

### flavor 专属

| 文件 | 说明 |
| --- | --- |
| `standard/.../eink/EInkConfig.kt`、`EInkUtils.kt` | 标准版空实现（`DISABLE_ANIMATIONS = false`、`isEInkDevice() = false`）。 |
| `eink/.../eink/EInkConfig.kt`、`EInkUtils.kt` | 墨水屏实现（黑白配色、`snap()` 动画、`isEInkDevice()` 基于厂商/型号关键字启发式判断约 L13-26）。 |

---

## 5. 常见任务定位路径（核心章节）

| 我要做 X | 改哪几个文件 |
| --- | --- |
| **加一个新页面** | `MainActivity.kt` 的 `sealed class Screen`（约 L166）+ **单一 NavHost**（`MainActivity.kt` 内，rail/手机共用一份）+ 新增 `ui/XxxScreen.kt` |
| **加 / 改一条导航路由** | 同上。NavHost 只有**一份**（rail 与手机共用；`if (useNavigationRail)` 只切换 `AppNavigationRail`，不切换 NavHost） |
| **改定向拦截参数（域名 / 白名单 / Referer）** | 参数在应用内**隐藏设置页**（`ui/HiddenSettingsScreen.kt`）配置，页内「?」直达帮助页（`ui/SettingsHelpScreen.kt`，10 个小节）；白名单用弹窗勾选（`ui/FeedWhitelistPicker.kt`），底层仍写回逗号分隔文本。持久化在 `util/DataStoreManager.kt` 的 4 个 `intercept_*` key（约 L32-35）；生效逻辑在 `util/InterceptionRuntime.kt`（+ `util/InterceptionSettings.kt` 解析、`util/InterceptionConfig.kt` 判定）；消费点在 `ui/ArticleDetailScreen.kt`（约 L262 判定、约 L342 拦截、约 L431 加 Referer） |
| **改 E-Ink 行为** | 编译期：改 `app/src/eink/...`（同名文件按 flavor 替换）；运行期：判 `BuildConfig.IS_EINK` |
| **加 / 改界面文案** | `res/values/strings.xml`（英文）+ `res/values-zh/strings.xml`（中文），**不要在 Kotlin 里硬编码**。**不要**定义 `app_name`——它由 flavor 的 `resValue` 生成。flavor 专属文案同理：`app/src/{standard,eink}/res/values/`（英文）+ `values-zh/`（中文） |
| **加依赖** | `gradle/libs.versions.toml` + `app/build.gradle.kts` |
| **加纯逻辑 + 单测** | 抽到 `util/` 纯函数 → 测试放 `app/src/test/java/com/i/miniread/`（纯 JVM，禁用 `android.*` 与 Robolectric，用 JUnit4） |
| **改主题 / 配色** | `ui/theme/Color.kt`、`ui/theme/Theme.kt`、`res/values/{colors,themes}.xml`、flavor 的 `res/values/colors.xml` |
| **改 token / baseUrl 存储** | `util/DataStoreManager.kt`（门面，**公开签名不可改**）+ `util/SecureTokenStore.kt` |
| **改 API 接口** | `network/MinifluxApi.kt`（接口与数据类同文件） |
| **发新版本** | 改 `app/build.gradle.kts` 的 `appVersionName` / `appVersionCode`（约 L6-7）→ 打 `v*` tag |

---

## 6. 约定规范（硬约束）

- **版本号唯一来源**：`app/build.gradle.kts` 的 `appVersionName` / `appVersionCode`。**不要**给 flavor 或 release buildType 加 `versionNameSuffix`（仅 debug 允许，值 `-debug`）。
- **签名**：本地无 keystore 时 release **自动回退 debug 签名**（由 `hasReleaseKeystore` 判定，约 L10-11/L86）；正式签名由 CI 处理（`release.yml` 解码 `KEYSTORE_BASE64`）。本地回退签名仅用于回归。
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

1. **`SettingsFragment` 无任何入口**：Manifest 未注册宿主 Activity（`AndroidManifest.xml` 只有 `MainActivity`）；且应用主题是 platform 的 `android:Theme.Material.Light.NoActionBar`（`res/values/themes.xml`），**不支持 AppCompat**——直接用它做设置页会崩。新界面一律用 Compose。
2. `res/values/arrays.xml` 结构原先非法（`<string-array>array</string-array>`），会让 `ListPreference` 拿到空列表（已修复为合法 `<item>` 结构，数组名不变）。
3. `app/proguard-rules.pro` 中 keep 了**不存在的**包 `com.i.miniread.model.**`（约 L81、L106）（数据模型实际都在 `network/` 包）。
4. `network/MinifluxApi.kt` 有**两个 `deleteFeed` 重载**（约 L40 `Int` 返回 `Void`、约 L141 `Long` **无返回类型**）。改动时注意区别。
5. `viewmodel/MinifluxViewModel.kt` 末尾的 `FeedListViewModel`（约 L592）**疑似死代码**：`async` 结果未被使用，且未被任何 UI 引用。
6. `app/src/androidTest/` 为**空目录**——本项目测试**全部**在 `app/src/test/`（纯 JVM）。
7. **NavHost 已合并为一份**（✅ 2026-10-13 修复）：此前 rail 布局与手机布局各有一份**逐字相同的副本**，且分处 `if (useNavigationRail) {...} else {...}` 两个分支内，导致手机竖屏↔横屏切换时整个内容子树被销毁重建（阅读页 WebView 被重建 → 页面重载、滚动位置丢失、卡顿）。现为**单一 NavHost**，`if` 只切换 `AppNavigationRail`。⚠️ **不要改回 if/else 两份 NavHost**；加路由只需改一处。
8. `ui/ArticleDetailScreen.kt` 的 `BottomAppBar` **必须**在 E-Ink 下显式传 `Modifier.height(48.dp)` + `tonalElevation = 0.dp` + `contentPadding`，否则会落到 Material3 默认的 80dp + 系统栏 inset + Level2 灰底（历史回归点：提交 `3707759`，随 v0.3.4 发布）。
9. `interceptWebRequest`（约 L470）走 OkHttp **自行重发请求**（复用 `interceptionHttpClient` 单例），异常时返回 `null` 让 WebView 回退默认加载——改动它**必须回归图片加载**。
10. 启用 R8（`isMinifyEnabled` + `isShrinkResources`，release 默认开启）后**必须回归定向拦截与图片加载**；出问题就把这两项置 `false`。
11. **i18n 是历史薄弱点**：登录页、分类列表页、条目列表页、文章详情底部栏、底部导航栏与平板侧边栏都曾把中英文字面量**硬编码在 Kotlin 里**（中英混杂 → 中文设备显示英文 / 英文设备显示中文）；`standard`/`eink` 的 `strings.xml` 也曾把**中文放在默认 `values/`** 目录。新增任何 UI 文案时务必走 `stringResource` 并把 key 同时加进 `values` 与 `values-zh`。
12. **MainActivity 声明了 `android:configChanges`**（旋转/分屏/深色切换**不重建** Activity）：这是阅读页保留 WebView 实例与滚动进度（旋转不丢进度）的关键，**不要删**。与之配套，阅读页 `ArticleWebView` 的 `loadDataWithBaseURL` **必须留在 `LaunchedEffect(htmlContent)` 里**（仅在内容变化时加载一次）——放回组合体主体会让每次重组（如收藏 toggle）都重载页面。新增依赖「配置刷新」的非 Compose 代码时，需自行在 `onConfigurationChanged` 处理。
13. **阅读位置持久化与恢复**（旋转不丢进度的兜底机制，改动前务必读完）：滚动快照由 `ArticleWebView.recordScrollPosition()` 写入 ViewModel（`saveReadingPosition`），恢复由 `LaunchedEffect(pageLoadTick, sizeSignature)` 执行（`pageLoadTick` 在 `onPageFinished` 自增；`sizeSignature` 取 `LocalConfiguration` 的 `screenWidthDp/screenHeightDp`）。四条红线：
    - 该 Effect 的 **key 只能是 `pageLoadTick` 与 `sizeSignature`**，**不得**加入 `content` / `savedPosition` / `entryId`——否则收藏 toggle 等「仅重组、不重载」场景会误触发恢复、造成位置跳动（与坑 12 同源的回归）。
    - **两类触发要分开处理**：`pageLoadTick` 变化 = 真·页面加载 → 直接走比例恢复兜底；仅 `sizeSignature` 变化 = 纯布局变化（旋转/分屏）→ 必须先用 `ReadingPositionResolver.needsRestoreAfterLayoutChange`（文档**比例偏差**判据）判断原生落点是否已正确，正确则**绝不 `scrollTo`**（WebView 原生保留的位置比比例换算更准，多动一次反而挪走）。⚠️ 不要用「`scrollY > 0` 就放过」这种弱判据：旋转会触发 CSS 重排，Chromium 可能只保留像素偏移而没做滚动锚定。
    - 强制补记（`onPageFinished` / `onRelease`）**不允许用 `scrollY == 0` 覆盖**已保存位置：页面刚加载完或 WebView 被重建时 `scrollY` 本就是 0，会把待恢复的位置抹掉。
    - `measureScrollSnapshot()` 的 `contentHeight` **不乘 `scale`**（写入与恢复必须同口径）；而滚动到底判定里的 `contentHeight * scale` 是**既有且存疑**的口径，两者并存是已知情况，改动其一必须同步另一处。
    另：`resolve()` 的贴底吸附只在 `saved.viewportHeight` 可信（`0 < viewport <= contentHeight`）时生效，规避布局未稳定时的异常测量。
    另：阅读页视口自 v1.0.3+ 起改为**响应式**（见坑 15），旋转/分屏会**真实重排**（`contentHeight` 随之变化）——这正是文档比例换算恢复机制存在的意义，恢复逻辑**不受影响**；不要因为「旋转必然重排」而新增弱判据或绕过 `needsRestoreAfterLayoutChange`。
14. **NavHost 子树重建问题**（✅ 2026-10-13 修复，见坑 7）：此前两份 NavHost 分处 `if (useNavigationRail) {...} else {...}` 两分支，手机竖屏↔横屏切换会销毁重建整个内容子树 —— 阅读页 WebView 被重建、页面重载（logcat 表现为每次旋转都出现 `Page finished loading`）、滚动位置丢失、并伴随明显卡顿。已通过**合并为单一 NavHost** 根治。验收判据：旋转后 logcat **不应**再出现 `Page finished loading`，而应出现 `Layout change: native position ok ... skip restore`。
15. **阅读页响应式渲染红线**（2026-10-13 起）：阅读页由「固定 ~980px 布局视口 + 缩放贴合」改为**真实设备宽度**的响应式渲染，三条红线：
    - `assets/template.html` **必须**保留 `<meta name="viewport" content="width=device-width, initial-scale=1">` 与 `<style>html { font-size: 6.4px; }</style>` rem 基数（`6.4 = 16 × 0.4`，系数 0.4 即旧模型 WebView 宽 ÷ 980px ≈ 0.399）。⚠️ **删除 rem 基数会让全部 rem 字号整体放大 ~2.5 倍**，删除 viewport meta 会让页面退回固定布局视口、旋转不重排。
    - **禁止**在 Android 侧按 `screenWidthDp >= 600` 判定后**条件注入** `tablet_styles.css`：`loadHtmlContentAsync` 必须**无条件注入**（读取后走进程级缓存 `cachedTabletCss`）。条件注入会导致旋转后不重排、手机横屏被误按平板限宽。
    - `assets/tablet_styles.css` 的媒体查询**必须**带高度守卫：`(min-width:600px) and (min-height:480px)`、`(min-width:840px) and (min-height:480px)`。高度守卫用于排除 WebView 高度通常仅 200~350px 的**手机横屏**。⚠️ 深色平板块**不在此文件**——它已迁入 `customdark.css`（见坑 16），本文件只保留布局与浅色样式。
    - 是否生效完全交给 CSS 媒体查询，按**真实视口**（宽 = WebView 内容区宽度，CSS px = dp）决定；块内字号已全部改为 `px`，不依赖 rem 基数。
16. **E-Ink 正文主题红线**（2026-10-17 起，issue #12）：Compose 侧 E-Ink 恒用 `EInkLightColorScheme`，WebView 正文必须同步，否则系统深色下出现「白工具栏 + 深色正文」，墨水屏还会因深底加重残影。两条红线：
    - **`util/ReaderTheme.kt` 的 `ReaderThemeResolver.isDarkMode(isEink, systemNightMode)` 是唯一判据**。两处调用点都必须走它：`loadHtmlContentAsync`（首次注入）与 `ArticleWebView` 的 `LaunchedEffect(pageLoadTick, isDarkTheme)`（运行期就地刷新）。⚠️ 只改一处会被另一处覆盖——这正是修复前的状态。
    - **`assets/custom.css` 必须保持 0 处 `prefers-color-scheme: dark`**。E-Ink flavor 恒注入这份文件，WebView 的 `prefers-color-scheme` 只跟系统走，媒体查询会把正文染成白底 `#d1d1d1` 字——只改 Kotlin 判定等于没修。`customdark.css` 内的夜间块**保留**（它只服务 standard + 系统深色）。深色平板块在 `customdark.css`，选择器必须带 `body` 前缀：主题表在 `<head>` 中**先于**平板表，同优先级会被 `tablet_styles.css` 的浅色基础样式覆盖。
17. **隐藏设置页 / 帮助页 / 订阅源选择器的红线**（2026-10-21 起）：
    - **编辑态必须 `rememberSaveable`**：点字段旁「?」跳帮助页会让 `HiddenSettingsScreen` 离开组合，普通 `remember` 在返回时会把四个字段全部丢掉（必现，不是概率问题）。
    - **主顶栏让位时必须同时清 inset**：`shouldShowTopBar == false` 时 `Scaffold` 的 `contentWindowInsets` 要置 `WindowInsets(0)`，否则内层 `Scaffold` 二次消费状态栏高度，标题上方多出一条空白。⚠️ **不要**为了隐藏顶栏在 content 里写 `if/else` 包 `NavHost`（违反坑 7/14）。
    - **选择器不能换成 `AlertDialog`**：M3 1.2.1 的 `text` 槽没有内建滚动，`LazyColumn` 直接放进去会撞 `infinite maximum height constraints`。必须自绘 `Dialog` + `Surface.heightIn(max = 560.dp)` + `Box(weight(1f, fill = false))`，且 `DialogProperties(usePlatformDefaultWidth = false)` 才能让 `fillMaxWidth(0.92f)` 生效。
    - **`feeds` 必须用 `observeAsState()` 的可空重载**：`fetchFeeds()` 失败会 `postValue(emptyList())`，给 `emptyList()` 默认值就无法区分「尚未加载」与「已加载为空」。两种状态都**不许裁剪已选 id**——失效 id 要在「可能已删除」分区里显式展示并允许主动取消。
    - 帮助页定位用 `scrollToItem`（瞬移），**不要** `animateScrollToItem`（E-Ink 拖影）；`?section=` 的 `navArgument` 必须给 `defaultValue = ""`，否则不带参数进入会崩在参数校验。
    - 诊断与解析必须同口径：`InterceptionDiagnosticsAnalyzer` 只能调 `InterceptionSettingsCodec.splitEntries`，**不要**另写 split；`FieldReport.validCount` 与 `InterceptionSettings.isActive` 已由单测锁死，改任一侧都要同步。
    - **E-Ink 只换样式、不重排**（2026-10-24 起，issue #16）：所有差异走 `val eink = BuildConfig.IS_EINK` 的条件参数，**不要**加分组卡片、**不要**改间距节奏 / 文案 / `rememberSaveable` / `contentWindowInsets`。固定取值：顶栏 `Modifier.height(40.dp)` + `titleMedium`（与 `MainActivity` 的 E-Ink 顶栏一致）+ 返回/帮助图标均 20dp；次要按钮统一走私有包装器 `SettingsOutlinedButton`（0dp 直角 + `BorderStroke(2.dp, Black)` + `contentColor = Black`），主 `Button` 黑底白字 + 全零 elevation；`OutlinedTextField` 只改边框/光标/容器色，**error 语义色保留 M3 默认**（灰度屏靠深浅区分即可）；`Switch` 选中白钮黑轨、未选中黑钮白轨 + 黑边。
    - ⚠️ `AlertDialog` 没有 `border` 参数，黑框只能写 `Modifier.border(2.dp, Black, 与 shape 完全相同的 RoundedCornerShape(8.dp))`：该 modifier 落在 `BasicAlertDialog` 的外层 `Box` 上（与内层 `Surface` 同尺寸），且 `Modifier.border` 先画内容后画描边，所以不会被白底盖住；**圆角不一致会在四角露出方框**。
    - ⚠️ `OutlinedTextFieldDefaults.colors(...)` 未传的项保持 token 默认（实现是 `.copy(...)`，不会回落成 primary 紫灰），因此 `supportingText` 仍是 `onSurfaceVariant` 灰，与本页 `StatusBlock` 同阶，不要「顺手」改成纯黑。
18. **Snackbar 默认是「反色」，不是主题色**（2026-10-21 核实）：M3 1.2.1 源码 `SnackbarTokens.ContainerColor = ColorSchemeKeyTokens.InverseSurface`，`SupportingTextColor = InverseOnSurface`。后果：深色主题下提示条变**浅色**（像没跟随主题），E-Ink 恒浅色方案下反而成**深色块**（违反恒浅色、残影重）。统一走 `ui/MinireadSnackbarHost.kt`，**不要**再直接写 `SnackbarHost(hostState)`；标准版用 `surfaceVariant` / `onSurfaceVariant`，E-Ink 走**自绘容器**（2026-10-24 定稿，issue #15）：`Surface(shape = RectangleShape, color = White, tonalElevation = 0.dp, shadowElevation = 0.dp, border = BorderStroke(2.dp, Black))` + 内层 `Row`（`start = 16.dp`，有 action 时 `end = 8.dp` 否则 16dp，文字 `bodyMedium` 上下 12dp，可选 `actionLabel` 走 `TextButton`），外层 `fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)`。
    - **为什么必须自绘**：① `Modifier.border(...)` 套在 M3 `Snackbar(snackbarData, modifier)` 上，会被该重载内部的 `modifier.padding(12.dp)` 推到容器**之外**，观感是一圈悬浮黑框（不是描边，也不是「12dp 错位」）；② `Snackbar` 的 `shadowElevation` 写死为 `SnackbarTokens.ContainerElevation`（6dp），**没有公开参数可覆盖**，纯白页面上必留灰晕。两条都无法用参数绕过。
    - **视觉规则**：只用纯黑 / 纯白 + 直角 + 2dp 线，与阅读页底栏（顶部 2dp 黑线）、列表卡片（2dp 黑框）同一套线条语言；**不要**加圆角、阴影、灰底或半透明 —— 墨水屏上这些都会变成可见的灰块或毛边。
    - 自绘版**不支持** `withDismissAction`（全仓 `showSnackbar(...)` 均为单参调用，要用需先补实现）；`SnackbarHost` 的淡入淡出仍在，属宿主内部动画，未改。
19. **E-Ink 星标的「空心」要用 `StarBorder`，不能用 `Icons.Outlined.Star`**（2026-10-24 起，issue #17）：`material-icons-core-android:1.6.1` 里 `outlined/StarKt` 与 `filled/StarKt` 的 path **逐行相同**（源码逐行比对过）——单个闭合轮廓、没有内孔，`Icons.Outlined.Star` 渲染出来就是实心星，墨水屏上「已收藏 / 未收藏」区分不出来；core 里也**没有** `StarBorder`。
    - 正确写法：`imageVector = if (entry.starred) Icons.Filled.Star else Icons.Outlined.StarBorder`；`StarBorder` 来自 **`material-icons-extended`** —— `gradle/libs.versions.toml` 里 `androidx-material-icons-extended`（**版本由 Compose BOM 托管，不要写 version**），`app/build.gradle.kts` 里 `implementation(libs.androidx.material.icons.extended)`。
    - 代价已知：该库 classes 约 40MB，debug 包与增量构建变慢，release 由 R8 裁到只剩用到的图标。为一个空心星引入它是权衡后的选择，**不要**再为此自造 drawable / 自绘 Canvas。
    - ⚠️ 该库其它 outlined 图标是否有内孔**未逐个核实**；阅读页 7 键里只有星标依赖「镂空」表达状态，改别的图标前先在设备上看一眼。

---

## 9. 发布流程

1. 确认 `main` 上版本号（`app/build.gradle.kts` 的 `appVersionName` / `appVersionCode`）已更新。
2. 打 tag 并推送（`v*`，如 `v1.1.0`）→ 触发 `.github/workflows/release.yml`。该 workflow 只用 `github.ref_name`，**不依赖附注 tag**。
   ⚠️ **tag 由 git 打与推**（附注 tag，与 `v1.0.0`~`v1.0.3` 一致），命令见 §10.2。用 jj 推 tag 有 `stale info` 前置要求且会把附注降级，见 §10.1 第 4 条。
3. CI 产出 `MiniRead-standard-<tag>.apk` / `MiniRead-eink-<tag>.apk` 并发布 GitHub Release。
4. 本地无 keystore 时 release 回退 debug 签名，**仅用于本地回归**；正式发布走 CI（由 secrets 提供 keystore）。

> 🚫 **推送远程 / 打 tag 前必须获得用户明确授权。**

---

## 10. 版本控制工作流（Git + jj）与纪律

本仓库自 **2026-10-16** 起以 **colocated 模式**同时使用 git 与 **jj（Jujutsu 0.46.0）**：`.jj/` 与 `.git/` 同级共存，两类命令都可用。引入前已按「换行符 / 性能 / 发布链路 / 回退」四道闸门实测，结论为**适合本项目**（数据见 `.workbuddy/reports/jj-pilot-report-2026-10-16.html`，本机私有，不随仓库分发）。

**为什么本仓库能用**：jj 官方**不支持**的 submodule / Git LFS / git hooks / `.gitattributes` / partial clone / `git worktree`，本仓库**一个都没用到**。⚠️ 将来若引入其中任意一项，需重新评估 jj 的适用性。

### 10.1 硬性纪律

1. ⚠️ **必须保留仓库级配置 `working-copy.eol-conversion = "input-output"`**（**不在 `.jj/` 内**：jj 0.38 起仓库级配置因安全原因移出仓库，实际存于 `%APPDATA%\jj\repos\<config-id>\config.toml`，其中 `<config-id>` 就是 `.jj/repo/config-id` 的内容，本仓库当前为 `56a476adacc04af8cabf`）。
   自查两条：`jj config path --repo` 应打印上述路径；`jj config list --repo` 应含 `working-copy.eol-conversion = "input-output"`。改配置用 `jj config edit --repo`，**不要**去 `.jj/repo/` 下找 `config.toml`（那里没有）。
   本机 `core.autocrlf=true`（磁盘 CRLF / 仓库 LF），而该项**默认 `none`**：缺失时 `jj st` 会把 **115 个文件**全部判为已修改（实测 21628 增 / 21628 删，纯行尾差异），且 `gradlew` 有被写成 CRLF 的风险（会破坏 Linux CI）。**重建 `.jj` 会生成新的 config-id，旧配置目录随即成为孤儿、新仓库读不到它 —— 配置必然丢失**；换机器同理。因此必须重设并强制重读一次：

   ```bash
   jj config set --repo working-copy.eol-conversion input-output
   jj restore        # 仅需一次，强制 jj 重读工作副本
   ```

   修复后磁盘字节**零改动**（115 个 CRLF 文件与 `gradlew` 哈希均保持不变）。**不要**试图用 `git add --renormalize` 或新增 `.gitattributes` 绕过这个问题（jj 不读 `.gitattributes`）。
2. ⚠️ **发布 tag 归 git，jj 只读**。实测（2026-10-18）`jj tag list -T 'name ++ " " ++ tracked'` 显示 `v1.0.0`~`v1.0.3` **全部 `tracked=false`** —— jj 从未跟踪过这些远端的附注 tag，这是既有常态。
   因此：建 / 移 / 删 / 推 tag 一律用 git；jj 侧只需在推送后 `jj git fetch` 让 tag 可见（`tracked` 仍可能为 `false`，无需处理）。
   若坚持用 jj 推 tag，未先 fetch 会失败：`Warning: ... refs/tags/vX (reason: stale info)` → `Error: Failed to push some bookmarks`；`--force` **不是**合法参数。
   不对称提示：**bookmark 重写后会自动 force-push，tag 不会**。
3. ⚠️ **jj 改写 `@`（`jj commit` / `jj edit` 等）时会把 git 的 HEAD 置于 detached 状态** → 此时既有的 `git fetch && git merge --ff-only origin/main` 会**静默失效**（只移动游离的 HEAD，`refs/heads/main` 不前进）。
   **凡要执行写操作的 git 命令，先 `git switch main`**；`git log/diff/show/status` 等只读命令不受影响。
   实测（2026-10-17）：`git switch main` 之后**不会**被再次 detach —— 连跑 `jj st` 与 `jj log`，HEAD 仍稳定在 `refs/heads/main`（`@` 的父提交与 main 指向一致时尤其如此）。所以「用了 jj 就永远不能跑 git 写命令」是误解，正确的说法是「**每次 jj 改写 `@` 之后重新挂回一次**」。
4. **tag 归 git 独占**：`git tag -a` / `-d` / `-f` / `push` 是唯一路径（本仓库 `v1.0.0`~`v1.1.0` 均为附注 tag）。**禁止**用 `jj tag set` 动发布 tag —— 它只能产出轻量 tag，会把附注降级为普通 commit 引用；也**避免** `jj git push --all`（会把 `tracked=false` 的历史 tag 一并纳入候选）。发布链路本身不依赖附注 tag（`release.yml` 只用 `github.ref_name`，见 §9），附注仅用于把发布说明写进注记。
5. **推送远程、打 / 移动 / 删除 tag、创建 Release 属公开不可逆动作**，每次均需用户明确授权（同 §9）。清理上游已消失的本地分支同理，且**必须先确认已完全合并**（见 10.3）。

### 10.2 常用命令

| 目的 | 命令 |
| --- | --- |
| 状态 / 日志 / diff | `jj st` / `jj log` / `jj diff` |
| 提交（无暂存区，改完即自动快照） | `jj commit -m "..."`（等价 describe + new） |
| 开新工作（≈ 建分支） | `jj new main -m "意图"` |
| 撤销上一步 / 跳到任意历史状态 | `jj undo` / `jj op log` / `jj op restore <op-id>` |
| 发版打 tag（附注，git） | `git tag -a vX -m "MiniRead X.Y.Z — 要点1、要点2"` → `git push origin vX` |
| 移动 tag 让 CI 重跑（git） | `git tag -f -a vX -m "..."` → `git push origin --delete vX` → `git push origin vX` |
| 让 jj 看见新 tag | `jj git fetch`（`tracked` 仍可能为 `false`，正常） |
| 同步远端（≈ git pull） | `jj git fetch` → `jj rebase -d main` |

> tag 操作**不要**用 `jj tag set`（只能轻量 tag，会降级附注）；`jj tag set` 若真要用于非发布场景，默认**拒绝移动**已有 tag，必须显式加 `--allow-move`。

### 10.3 回退 jj（完全退回纯 git）

```bash
rm -rf .jj
git for-each-ref --format='%(refname)' refs/jj | while read -r r; do git update-ref -d "$r"; done
git switch main
```

三行缺一不可：第二行清除实测 **82 个** `refs/jj/*` 残留引用（49 个 `keep` + 33 个 `remote-tags`），第三行修掉 detached HEAD。执行后 **git 侧**的 refs / index / `git status` / git 本地配置 / 文件哈希与引入前基线**完全一致**（已实测）。

⚠️ **但「完全一致」仅限 git 侧**：`%APPDATA%\jj\repos\<config-id>\`（本仓库为 `56a476adacc04af8cabf`，即 `.jj/repo/config-id` 的内容）不会随 `.jj` 删除而消失，其中的 `config.toml` 仍留在磁盘上。它不再被任何仓库读取，但会永久占位（本机现有 5 个，其中 4 个是试点实验遗留）。确认不再使用 jj 后可定向清理：

```bash
ls "$APPDATA/jj/repos/"                          # 先确认目录（Git Bash 下 $APPDATA 可用）
rm -rf "$APPDATA/jj/repos/56a476adacc04af8cabf"  # 只删本仓库这一份；其余按需
```

清理陈旧分支的安全做法（**先校验已合并，再删**）：

```bash
git fetch --all --prune
git rev-list --count origin/main..<branch>   # 必须为 0 才可删
git branch -d <branch>                       # 用 -d 而非 -D
```

### 10.4 其它已核实的坑

1. **本地 tag 与远端不一致**时，`git fetch --tags` 会报 `would clobber existing tag` 并跳过该 tag。确认以远端为准后 `git tag -f <tag> <远端 commit>` 对齐即可（本地旧对象不会丢失）。
2. **colocated 下不要用 git 做提交 / 改写历史 / 切工作副本**（会造成 divergent change）。提交一律走 jj；git 只用于只读查看与 tag / 分支清理。
3. **性能预期**：jj 每条命令都会走一次工作区快照，`jj st` 实测约 **991ms** vs `git status` 约 748ms（工作区含 625MB / 7240 个已忽略的构建文件）。属预期行为，不是故障。

---

## 文档地图

### 权威（与当前代码同步维护）

| 文档 | 说明 |
| --- | --- |
| `README.md` | 项目简介、功能、构建变体 |
| `CHANGELOG.md` | 变更日志 |
| `RELEASE_NOTES.md` | 发布说明 |
| `AGENTS.md` | 本文件，AI 开发指令（唯一权威） |
| `doc/BUILD_GUIDE.md` | 完整构建指南 |
| `doc/BUILD_FLAVORS_GUIDE.md` | Flavor 架构与开发指南 |
| `doc/QUICK_BUILD_REFERENCE.md` | 构建命令速查 |
| `doc/TECHNICAL_SUMMARY.md` | 技术总结 |
| `doc/TABLET_ADAPTATION.md` | 平板适配 |
| `doc/UI_DIFFERENCES.md` | UI 差异 |

### 历史存档

- `doc/archive/` — 历史过程文档（完成总结 / 修复报告 / 迁移记录），**不保证与当前代码一致**。

### 开发笔记（不保证最新）

- `API Reference.md`（仓库根目录）。
