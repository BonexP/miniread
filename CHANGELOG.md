# Changelog

All notable changes to MiniRead will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-11-24

### Added
- Initial stable release of MiniRead
- Complete Miniflux RSS reader client implementation
- Support for two product flavors:
  - **Standard**: Full-featured version with Material Design 3, dynamic colors, and animations
  - **E-Ink**: Optimized version for e-ink devices with high contrast, black & white UI, and no animations
- User authentication with Miniflux API token
- RSS feed management (view, add, delete, refresh)
- Category management and browsing
- Article list view with unread/starred status
- Article reading with optimized WebView rendering
- Mark articles as read/unread
- Bookmark (star) functionality
- Share articles
- Open articles in external browser
- Navigate between articles (previous/next)
- Today's articles view (last 24 hours)
- Custom feed sorting with persistent order
- Unread count display for feeds and categories
- Mark entire category/feed as read
- Material Design 3 theming
- Dark mode support
- Dynamic color support (Android 12+)
- Volume key navigation for e-ink devices
- Dual installation support (both variants can be installed simultaneously)

### Technical Features
- Jetpack Compose UI framework
- Material Design 3 components
- MVVM architecture with ViewModel and LiveData
- Retrofit for API communication
- DataStore for secure token storage
- WebView with custom CSS and JavaScript injection
- Responsive layout for different screen sizes
- Build variants for different device types

### Documentation
- Comprehensive README with setup and usage instructions
- BUILD_GUIDE for detailed build instructions
- Technical documentation in `/doc` folder
- MIT License

### Fixed
- Removed hardcoded domain references for better portability
- Removed deprecated jcenter repository
- Cleaned up TODO comments and example code
- Updated version numbering to 1.0.0
- Removed "alpha" suffix from release builds
- Fixed GitHub Actions workflow badges in README

### Security
- Secure API token storage using DataStore
- HTTPS communication for all API calls

### Known Limitations
- No offline caching (planned for future release)
- No push notifications (planned for future release)

## [0.3.1] - Previous Development Version

### Development Phase
- Feature development and testing
- Multiple bug fixes and improvements
- E-Ink variant implementation
- Flavor system setup

---

[1.0.0]: https://github.com/BonexP/miniread/releases/tag/v1.0.0
