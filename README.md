### 项目简介

这个项目是一个使用 Jetpack Compose 和 Material Design 3 (MD3) 构建的 Miniflux Android 阅读客户端。用户可以通过应用访问和管理他们的 RSS 源、查看分类、阅读文章等。应用设计以现代的 Material Design 3 风格为基础，支持动态切换界面和数据同步。

### 主要功能

1. **用户登录**：通过 API Token 登录并保存到本地。
2. **查看 RSS 源**：展示用户订阅的 RSS 源，并允许点击查看文章详情。
3. **分类管理**：查看并切换不同的分类。
4. **文章阅读**：
   - 支持文章的详细阅读，包括文章内容和发布时间显示。
   - 对特定 `feed_id`（如 `feed_id = 26`）的文章，通过 WebView 对 `cdnfile.sspai.com` 的请求自动添加 `Referer` 头 `https://sspai.com/`。
   - 通过动态生成 HTML 内容，并注入自定义 CSS 和 JavaScript，优化了文章显示效果，特别是移动设备上的字体大小和布局。
5. **汉堡菜单导航**：使用 MD3 风格的汉堡菜单进行界面导航，并在文章阅读时禁用滑动手势，以避免与 WebView 滑动冲突。

### 技术栈

- **Jetpack Compose**：用于构建用户界面。
- **Material Design 3 (MD3)**：为应用提供现代化的 UI 设计。
- **ViewModel 和 LiveData**：用于管理 UI 数据和生命周期。
- **Retrofit**：用于网络请求和数据获取。
- **WebView**：用于显示文章内容，并支持动态内容注入和特定域名请求头的修改。

### 项目结构

- **MainActivity.kt**：主活动文件，负责初始化应用和管理导航。
- **MinifluxViewModel.kt**：ViewModel 文件，管理数据流、处理 API 调用、存储和更新数据。
- **FeedListScreen.kt**：显示用户订阅的 RSS 源列表。
- **CategoryListScreen.kt**：显示并管理 RSS 分类。
- **EntryListScreen.kt**：展示分类下的文章列表。
- **ArticleDetailScreen.kt**：展示文章的详细内容，支持特定域名的请求头修改。

### 文件说明

- **MainActivity.kt**：
   - 包含应用的核心导航逻辑和 MD3 样式的 TopAppBar 及 Drawer 菜单。
   - 处理文章阅读时禁用滑动手势，避免与 WebView 滑动冲突。
- **MinifluxViewModel.kt**：
   - 处理与 Miniflux API 的交互，包括获取 RSS 源、分类、文章及用户信息。
   - 提供 `feed_id` 的检查功能，用于文章请求头的条件性修改。
- **FeedListScreen.kt**：展示所有订阅的 RSS 源，并允许用户点击进入文章详情页面。
- **CategoryListScreen.kt**：展示并管理所有分类，并允许用户根据分类筛选文章。
- **EntryListScreen.kt**：展示选中的分类下的文章列表，并支持文章的点击导航。
- **ArticleDetailScreen.kt**：
   - 展示选中的文章内容，并通过 WebView 加载 HTML。
   - 支持根据 `feed_id` 动态注入 `Referer` 头和优化显示效果。

### 部署与运行

1. **Clone 仓库**：
   ```bash
   git clone <repository-url>
   ```
2. **打开 Android Studio**：导入项目并等待依赖加载完成。
3. **配置 API Token**：在应用第一次启动时，输入您的 Miniflux API Token。
4. **运行应用**：点击 `Run` 按钮，选择模拟器或物理设备进行调试。

### 注意事项

- **MD3 兼容性**：确保设备支持 Material Design 3。如果设备不支持，应用将回退到默认的 Material Design 2 样式，但功能不会受影响。
- **WebView 优化**：文章阅读界面通过 WebView 加载，已针对移动设备进行了字体和布局优化。如果使用高刷新率设备，确保内容加载流畅且不卡顿。
- **API 配置**：确保使用正确的 Miniflux API URL。

### 常见问题

1. **汉堡菜单滑动冲突**：文章阅读时已禁用滑动手势，确保 WebView 滑动时不会误触发汉堡菜单。
2. **分类数据为空**：确保在导航到分类页面时调用 `fetchCategories()`，并检查网络请求是否成功。
3. **文章内容加载卡顿**：通过优化 WebView 设置和减少不必要的重绘，提升加载和滚动性能。

### TODO

- 添加用户设置页面以管理 API Token 和其他用户信息。
- 增强错误处理和用户反馈机制。
- 支持多语言界面。
- 优化高刷新率屏幕上的显示和交互。

### 贡献指南

如果您对项目有改进建议或发现了 Bug，欢迎提交 Pull Request 或 Issue。

### 许可证

该项目基于 MIT 许可证开源。

---

这个 README 综合了我们之前讨论的所有内容，包括对文章阅读界面的优化、特定域名请求头的修改，以及应用的整体架构和技术栈说明，旨在帮助开发者快速理解和使用项目。如果有其他需求或改进，请随时更新 README。