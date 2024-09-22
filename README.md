
### 项目简介

本项目是一个使用 Jetpack Compose 和 Material Design 3 (MD3) 构建的现代化 Miniflux Android 阅读客户端。应用提供简洁的 RSS 订阅管理功能，用户可以浏览、阅读和分类管理文章内容，并享受优雅且流畅的 UI 体验。

### 主要功能

1. **用户登录**：支持通过 Miniflux API Token 登录，并将 Token 安全存储以实现自动登录。
2. **查看 RSS 源**：展示用户订阅的 RSS 源，点击查看该源内的文章列表。
3. **分类管理**：用户可以按分类浏览内容，轻松切换查看不同主题的文章。
4. **文章阅读**：
    - 提供详细的文章阅读界面，支持显示文章内容、来源、发布时间。
    - 对特定 `feed_id` 进行请求头的动态调整（例如：对 `cdnfile.sspai.com` 的请求自动添加 `Referer` 头 `https://sspai.com/`）。
    - 支持动态生成 HTML 内容，并注入自定义 CSS 和 JavaScript，优化移动设备的文章显示效果。
5. **TodayEntryList**：显示当天的文章列表，用户可快速浏览最新内容。
6. **汉堡菜单导航**：MD3 风格的汉堡菜单导航，支持在不同页面间流畅切换。在文章阅读时禁用滑动手势，避免与 WebView 滑动操作冲突。

### 技术栈

- **Jetpack Compose**：基于声明式 UI 构建的现代 Android UI 框架。
- **Material Design 3 (MD3)**：提供最新的 Material Design 3 风格设计，为应用提供现代化 UI。
- **ViewModel 和 LiveData**：用于管理 UI 逻辑与数据同步，确保数据的稳定性和实时更新。
- **Retrofit**：负责所有网络请求和数据同步，简化与 Miniflux API 的交互。
- **WebView**：在文章阅读页面使用，支持动态内容渲染和自定义显示优化。

### 项目结构

- **MainActivity.kt**：应用的主活动，负责 UI 初始化和导航逻辑。
- **MinifluxViewModel.kt**：核心数据层，负责 API 调用、数据存储和 UI 数据管理。
- **FeedListScreen.kt**：展示用户订阅的 RSS 源列表，支持源选择与文章导航。
- **CategoryListScreen.kt**：管理和展示用户的分类，支持按分类筛选文章。
- **EntryListScreen.kt**：展示特定分类或订阅源下的文章列表，支持文章详情的导航。
- **TodayEntryListScreen.kt**：EntryListScreen 的特例，只展示当天发布的文章。
- **ArticleDetailScreen.kt**：文章阅读界面，使用 WebView 加载和优化文章内容。

### 文件说明

- **MainActivity.kt**：
    - 负责应用的导航结构，使用 Scaffold 实现 MD3 风格的 TopAppBar 和 BottomNavigation。
    - 在阅读文章时，自动隐藏底部导航栏，优化视觉体验。
- **MinifluxViewModel.kt**：
    - 通过 Miniflux API 获取 RSS 源、分类、文章等数据。
    - 针对特定 `feed_id` 的请求头进行了优化，提升特定站点文章加载体验。
- **EntryListScreen.kt**：
    - 显示选定分类或订阅源的文章列表，点击文章导航至详情页面。
    - 支持通过 `LaunchedEffect` 实现动态刷新文章列表。
- **TodayEntryListScreen.kt**：
    - 继承自 `EntryListScreen`，只显示当天的文章，利用 UNIX 时间戳计算当日范围的文章。
- **ArticleDetailScreen.kt**：
    - 使用 WebView 渲染文章内容，支持动态注入 `Referer` 头和优化 HTML 渲染。
    - 通过注入自定义 CSS 和 JavaScript，提升文章在移动设备上的阅读体验。

### 部署与运行

1. **克隆仓库**：
   ```bash
   git clone <repository-url>
   ```
2. **打开 Android Studio**：导入项目，并等待依赖加载完成。
3. **配置 API Token**：在应用首次启动时，输入 Miniflux API Token 并保存。
4. **运行应用**：点击 `Run` 按钮，在模拟器或物理设备上调试应用。

### 注意事项

- **Material Design 3 兼容性**：应用基于 Material Design 3 构建，确保运行设备支持最新的 MD3 样式。对于不支持的设备，应用会回退到 Material Design 2 的兼容模式，功能不受影响。
- **WebView 优化**：文章阅读页面经过优化，适配移动设备的显示效果。如遇加载性能问题，请检查网络状态或设备性能。
- **动态 API 配置**：确保 Miniflux API URL 和 Token 配置正确，以避免数据同步失败。

### 常见问题

1. **文章阅读中的滑动冲突**：文章阅读时已禁用汉堡菜单的滑动手势，确保 WebView 的交互体验顺畅。
2. **分类或文章加载失败**：在分类和文章加载异常时，请检查网络状态，或确保 `fetchCategories()`、`refreshEntriesByFeed()` 等方法正常调用。
3. **TodayEntryListScreen 加载异常**：确保系统时间准确无误，否则可能导致当天文章列表显示不正确。

### 未来改进

1. **缓存机制**：计划添加文章离线缓存功能，提升用户无网状态下的阅读体验。
2. **推送通知**：未来将集成推送功能，当订阅源有新内容时实时通知用户。
3. **深色模式优化**：进一步优化深色模式下的 UI 设计，提升用户夜间阅读体验。


### 贡献指南

欢迎对本项目提出改进建议或报告 Bug。您可以通过提交 Pull Request 或 Issue 参与项目开发。

### 许可证

本项目基于 MIT 许可证开源发布。