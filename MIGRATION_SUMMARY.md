# 数据持久化迁移总结

## 迁移完成时间
2025-10-22

## 迁移概述
成功将整个应用的数据持久化策略统一为 **DataStore**，实现了轻量、现代且优雅的存储方案。

---

## ✅ 已完成的工作

### 1. **添加 DataStore 依赖**
- 在 `gradle/libs.versions.toml` 中添加了 DataStore 版本
- 在 `app/build.gradle.kts` 中添加了 `androidx-datastore-preferences` 依赖
- **移除了所有 Room 相关依赖**（room-common, room-ktx, room-compiler, ksp）

### 2. **创建新的 DataStoreManager**
**文件位置**: `app/src/main/java/com/i/miniread/util/DataStoreManager.kt`

**功能**:
- ✅ 存储/读取 baseUrl
- ✅ 存储/读取 apiToken
- ✅ 存储/读取 Feed 排序（以 JSON 格式）
- ✅ 支持 suspend 函数和 Flow API
- ✅ 使用 Gson 序列化复杂数据

### 3. **更新的文件列表**

#### MainActivity.kt
- ✅ 替换 `PreferenceManager` 为 `DataStoreManager`
- ✅ 在 `onCreate` 中使用协程读取数据
- ✅ 在 `MainContent` 中使用 `LaunchedEffect` 和 `rememberCoroutineScope`

#### MinifluxViewModel.kt
- ✅ 移除所有 Room 相关 import
- ✅ 删除数据库实例化代码
- ✅ 更新 `saveFeedOrder()` 方法使用 DataStore
- ✅ 更新 `loadCustomFeedOrder()` 方法使用 DataStore

#### ArticleDetailScreen.kt
- ✅ 替换 `PreferenceManager` 为 `DataStoreManager`
- ✅ 在 `isTargetDomain()` 中使用 `runBlocking` 读取数据
- ✅ 在 `interceptWebRequest()` 中使用 `runBlocking` 读取数据

#### LoginScreen.kt
- ✅ 移除未使用的 `PreferenceManager` import

### 4. **删除的文件**
- ❌ `FeedOrderDao.kt` - Room DAO（已删除）
- ❌ `FeedOrderEntity.kt` - Room Entity（已删除）
- ❌ `EntryDao.kt` - 未使用的 DAO（已删除）
- ❌ `EntryEntity.kt` - 未使用的 Entity（已删除）
- ❌ `AppDatabase.kt` - Room 数据库（已删除）
- ❌ `PreferenceManager.kt` - 旧的 SharedPreferences 管理器（已删除）

---

## 📊 迁移前后对比

| 数据类型 | 迁移前 | 迁移后 |
|---------|--------|--------|
| **配置数据**<br>(baseUrl, apiToken) | SharedPreferences | DataStore |
| **Feed 排序** | Room 数据库 | DataStore (JSON) |
| **未使用的 Entry 存储** | Room Entity/DAO（未使用） | 已删除 |

---

## 🎯 优势分析

### 1. **代码简化**
- **减少文件数**: 删除 6 个文件（5 个 Room 相关 + 1 个 SharedPreferences）
- **统一 API**: 所有数据操作都通过 DataStoreManager
- **无需样板代码**: 不再需要 Entity、DAO、Database 声明

### 2. **现代化**
- ✅ Google 推荐的现代存储方案
- ✅ 原生支持 Kotlin 协程和 Flow
- ✅ 类型安全的 API
- ✅ 更好的错误处理

### 3. **轻量级**
- 无需 Room 编译器处理（移除 KSP 依赖）
- 更小的 APK 体积
- 更快的编译速度
- 完美符合你的"轻量在线阅读器"定位

### 4. **灵活性**
- 使用 Gson 序列化复杂数据（Feed 排序）
- 易于扩展新的配置项
- 支持数据迁移（如果需要）

---

## 📝 数据存储格式

### DataStore Preferences 键值对:
```
miniread_prefs {
    "base_url": "https://your-miniflux-instance.com"
    "api_token": "your-api-token-here"
    "feed_order": "{\"26\":0,\"38\":1,\"52\":2}"  // JSON 格式的 Map<Int, Int>
}
```

---

## ⚠️ 重要提示

### 数据迁移
如果用户已经安装了旧版本应用：
- ✅ **配置数据会丢失**: 用户需要重新登录
- ✅ **Feed 排序会丢失**: 会恢复默认排序（按标题）
- ✅ **无其他影响**: 所有数据都来自服务器

### 可选的数据迁移方案
如果你希望保留老用户数据，可以在 `MainActivity.onCreate()` 中添加一次性迁移代码：

```kotlin
// 迁移旧数据（仅执行一次）
lifecycleScope.launch {
    if (DataStoreManager.getBaseUrl().isEmpty()) {
        // 从 SharedPreferences 读取旧数据
        val oldPrefs = getSharedPreferences("miniread_prefs", Context.MODE_PRIVATE)
        val oldBaseUrl = oldPrefs.getString("base_url", "")
        val oldToken = oldPrefs.getString("api_token", "")
        
        if (!oldBaseUrl.isNullOrEmpty() && !oldToken.isNullOrEmpty()) {
            DataStoreManager.setBaseUrl(oldBaseUrl)
            DataStoreManager.setApiToken(oldToken)
            Log.d("Migration", "Migrated old preferences to DataStore")
        }
    }
}
```

---

## ✨ 总结

### 成就解锁
- ✅ 删除 Room 数据库（过度设计）
- ✅ 删除 SharedPreferences（已过时）
- ✅ 统一使用 DataStore（现代化）
- ✅ 代码更简洁优雅
- ✅ 应用更轻量快速

### 技术栈更新
```diff
- Room Database (SQLite)
- SharedPreferences
+ DataStore Preferences
```

### 代码统计
- **删除**: ~200 行（6 个文件）
- **新增**: ~100 行（1 个文件）
- **净减少**: ~100 行代码

---

## 🚀 下一步建议

1. **测试应用**: 确保登录、Feed 排序功能正常
2. **清理缓存**: 卸载重装应用，测试全新安装体验
3. **版本升级**: 建议升级版本号并在更新日志中说明
4. **可选优化**: 
   - 添加数据备份/恢复功能
   - 使用 Proto DataStore 替代 Preferences（类型更安全）

---

## 📞 技术支持

如有问题，请检查：
1. DataStore 初始化是否在 Application 或 Activity.onCreate 中调用
2. 所有协程操作是否在正确的作用域中
3. 是否正确处理了 suspend 函数调用

**迁移完成！享受轻量优雅的新架构吧！** 🎉

