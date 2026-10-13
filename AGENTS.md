# AGENTS.md — MiniRead 仓库 AI 开发指令

> 本文件是本仓库对 AI Agent 的**唯一权威指令**。
> 这里**只写机制与路径**：所有"具体取值"（真实实例地址、真实 feed id、Referer 规则等）都已改为**用户可配置**，不在文档中记录；需要时请按文中指引「在哪配、怎么改」。
> 路径一律为**仓库相对路径**；行号对应 `release/1.0.0` 分支当前工作区，标注 `约 Lx` 的为近似值。

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

- **唯一来源**：`app/build.gradle.kts` 顶部的 `appVersionName` / `appVersionCode`（当前 **1.0.3 / 103**，约 L6-7）。
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
| `main` | 共享代码与资源；26 个 `.kt`（含 `util/InterceptionSettings.kt`、`util/InterceptionRuntime.kt`、`ui/HiddenSettingsScreen.kt`）+ `res/` + `assets/` |
| `standard` | 2 个 `.kt`（`eink/EInkConfig.kt`、`eink/EInkUtils.kt` 的标准版空实现）+ 2 个 `res/values/*.xml` |
| `eink` | 2 个 `.kt`（同名文件的墨水屏实现）+ 2 个 `res/values/*.xml` |
| `test` | 纯 JVM 单测（8 个文件 / 110 个用例，见下） |
| `androidTest` | **空目录**（仅 `java/` 空壳）；本项目测试全部在 `test/` |

### `app/src/main/java/com/i/miniread/` 包职责

| 路径 | 职责 |
| --- | --- |
| `MainActivity.kt`、`SettingsFragment.kt` | Compose 宿主 + 导航；`SettingsFragment` 为历史遗留（当前无入口，见 §8） |
| `ui/` | 7 个 Screen + `ui/adaptive/`（平板适配）+ `ui/theme/`（配色/主题/字体） |
| `ui/example/` | 见 `example/FlavorUsageExample.kt`（flavor 用法示例，非主流程） |
| `viewmodel/` | `MinifluxViewModel.kt`（唯一 ViewModel） |
| `network/` | `MinifluxApi.kt`（Retrofit 接口 + 数据类 + `RetrofitInstance`） |
| `util/` | 纯逻辑与存储门面：`DataStoreManager`、`SecureTokenStore`、`InterceptionConfig`、`InterceptionSettings`、`InterceptionRuntime`、`DomainHelper`、`FeedSorter`、`TimeWindow`、`ReadingPosition` |
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

### `ui/`（7 个 Screen）

| 文件 | 说明 |
| --- | --- |
| `ui/LoginScreen.kt` | 登录页，输入 baseUrl + API Token（约 L21）。 |
| `ui/FeedListScreen.kt` | 订阅源列表，含排序编辑/标记已读对话框（约 L56）；条目 `FeedItem`（约 L210）。 |
| `ui/CategoryListScreen.kt` | 分类列表（约 L39）；`CategoryItem`（约 L88）。 |
| `ui/EntryListScreen.kt` | 文章列表（按 feed 或 category）（约 L53）；`EntryItem`（约 L103）；`localizePublishTime`（约 L221）。 |
| `ui/TodayEntryListScreen.kt` | 「今天」列表，复用 EntryList 思路、只取最近 24h（约 L27）。 |
| `ui/SubFeedScreen.kt` | 某分类下的订阅源列表（约 L17）。 |
| `ui/ArticleDetailScreen.kt` | 阅读页。`ArticleDetailScreen`（约 L81）→ WebView 容器；`ArticleActionsBar`（约 L131，`BottomAppBar` 约 L161）；`ArticleWebView`（约 L310）注入 `assets/` 的 HTML/CSS，含 `shouldInterceptRequest` 拦截点、**阅读位置写入与恢复**（见 §8 已知坑 12/13）；`interceptWebRequest`（约 L600）用 OkHttp 自行重发请求。 |

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
| **改定向拦截参数（域名 / 白名单 / Referer）** | 参数在应用内**隐藏设置页**（`ui/HiddenSettingsScreen.kt`）配置；持久化在 `util/DataStoreManager.kt` 的 4 个 `intercept_*` key（约 L32-35）；生效逻辑在 `util/InterceptionRuntime.kt`（+ `util/InterceptionSettings.kt` 解析、`util/InterceptionConfig.kt` 判定）；消费点在 `ui/ArticleDetailScreen.kt`（约 L262 判定、约 L342 拦截、约 L431 加 Referer） |
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
    - `assets/tablet_styles.css` 的媒体查询**必须**带高度守卫：`(min-width:600px) and (min-height:480px)`、`(min-width:840px) and (min-height:480px)`（深色块同步）。高度守卫用于排除 WebView 高度通常仅 200~350px 的**手机横屏**。
    - 是否生效完全交给 CSS 媒体查询，按**真实视口**（宽 = WebView 内容区宽度，CSS px = dp）决定；块内字号已全部改为 `px`，不依赖 rem 基数。

---

## 9. 发布流程

1. 确认 `main` 上版本号（`app/build.gradle.kts` 的 `appVersionName` / `appVersionCode`）已更新。
2. 打 tag：`v*`（如 `v1.0.0`）并推送 → 触发 `.github/workflows/release.yml`。
3. CI 产出 `MiniRead-standard-<tag>.apk` / `MiniRead-eink-<tag>.apk` 并发布 GitHub Release。
4. 本地无 keystore 时 release 回退 debug 签名，**仅用于本地回归**；正式发布走 CI（由 secrets 提供 keystore）。

> 🚫 **推送远程 / 打 tag 前必须获得用户明确授权。**

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
