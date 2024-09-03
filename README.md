### 项目简介

本项目是一个使用 Jetpack Compose 和 Material Design 3 (MD3) 构建的现代化 Miniflux Android 阅读客户端。该应用让用户轻松管理和访问他们的 RSS 订阅源，分类查看内容，提供无缝的文章阅读体验，所有界面都基于 Material Design 3 设计标准，确保时尚与实用并重。

### 主要功能

1. **用户登录**：支持通过 API Token 登录，并将其安全地存储在本地，以便自动登录。
2. **查看 RSS 源**：展示用户订阅的 RSS 源列表，点击即可查看源内文章。
3. **分类管理**：浏览不同的分类，轻松切换查看不同主题的内容。
4. **文章阅读**：
    - 提供详细的文章阅读界面，支持显示文章内容及其发布时间。
    - 对特定 `feed_id` 进行优化处理（例如：针对 `cdnfile.sspai.com` 的请求自动添加 `Referer` 头 `https://sspai.com/`）。
    - 支持动态生成 HTML 内容，并注入自定义 CSS 和 JavaScript，以优化文章在移动设备上的显示效果。
5. **汉堡菜单导航**：MD3 风格的汉堡菜单导航，允许用户在不同视图间流畅切换。文章阅读时会禁用滑动手势，防止与 WebView 滑动操作发生冲突。

### 技术栈

- **Jetpack Compose**：使用声明式 UI 框架构建用户界面。
- **Material Design 3 (MD3)**：为应用提供时尚现代的 UI 设计。
- **ViewModel 和 LiveData**：确保 UI 数据管理与生命周期的自动同步。
- **Retrofit**：负责所有网络请求和数据同步。
- **WebView**：用于渲染文章内容，并支持高级的动态内容注入。

### 项目结构

- **MainActivity.kt**：应用的核心活动，负责初始化 UI 和管理导航逻辑。
- **MinifluxViewModel.kt**：核心数据处理层，负责 API 调用、数据存储和 UI 数据流管理。
- **FeedListScreen.kt**：展示用户订阅的 RSS 源列表。
- **CategoryListScreen.kt**：展示和管理用户的分类，并允许根据分类筛选文章。
- **EntryListScreen.kt**：显示特定分类或源下的文章列表。
- **ArticleDetailScreen.kt**：文章阅读界面，支持 WebView 显示和内容优化。

### 文件说明

- **MainActivity.kt**：
    - 包含应用的导航逻辑和 UI 布局，使用 MD3 风格的 TopAppBar 和 Drawer 菜单。
    - 在文章阅读时禁用汉堡菜单的滑动手势，确保 WebView 滑动不被打断。
- **MinifluxViewModel.kt**：
    - 与 Miniflux API 交互，获取和管理 RSS 源、分类、文章及用户信息。
    - 针对特定 `feed_id` 实现请求头的条件性修改，优化文章加载。
- **FeedListScreen.kt**：展示订阅的 RSS 源，并支持导航至文章列表页面。
- **CategoryListScreen.kt**：管理和展示分类，提供按分类筛选文章的功能。
- **EntryListScreen.kt**：展示选定分类或源下的文章列表，支持文章点击导航。
- **ArticleDetailScreen.kt**：
    - 使用 WebView 渲染文章内容，支持根据 `feed_id` 动态注入 `Referer` 头。
    - 为移动设备优化字体和布局，提升阅读体验。

### 部署与运行

1. **克隆仓库**：
   ```bash
   git clone <repository-url>
   ```
2. **打开 Android Studio**：导入项目并等待依赖加载完成。
3. **配置 API Token**：在首次启动时，输入并保存您的 Miniflux API Token。
4. **运行应用**：点击 `Run` 按钮，选择模拟器或物理设备调试应用。

### 注意事项

- **Material Design 3 兼容性**：应用依赖于 MD3 特性，确保设备支持。如不支持，应用将回退至 Material Design 2 样式，功能保持一致。
- **WebView 优化**：文章阅读页面经过精心优化，适用于移动设备。如遇性能问题，请检查设备设置和网络环境。
- **API 配置**：确保 Miniflux API URL 正确无误，避免数据同步问题。

### 常见问题

1. **汉堡菜单滑动冲突**：在文章阅读时已禁用汉堡菜单滑动手势，确保 WebView 交互顺畅。
2. **分类数据加载失败**：请确保导航至分类页面时调用 `fetchCategories()` 并检查网络状态。
3. **文章加载缓慢**：检查网络连接，并考虑优化 WebView 设置以提升加载速度。

### TODO

- 添加用户设置页面，支持 API Token 和其他用户信息管理。
- 提升错误处理能力，增强用户反馈机制。
- 支持多语言界面，满足全球用户需求。
- 优化高刷新率设备的显示和交互体验。

### 贡献指南

欢迎对本项目提出改进建议或报告 Bug。您可以通过提交 Pull Request 或 Issue 参与项目开发。

### 许可证

本项目基于 MIT 许可证开源发布。

---

这个 README 经过调整后，更加清晰地表达了项目的功能和结构，并结合我们之前的讨论进行了补充和优化。如果需要进一步修改或添加内容，请随时提出！