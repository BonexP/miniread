package com.i.miniread.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.i.miniread.network.Category
import com.i.miniread.network.Entry
import com.i.miniread.network.EntryAndStatus
import com.i.miniread.network.Feed
import com.i.miniread.network.FeedCreationRequest
import com.i.miniread.network.RetrofitInstance
import com.i.miniread.network.UserInfo
import com.i.miniread.util.DataStoreManager
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MinifluxViewModel(application: Application) : AndroidViewModel(application) {

    private val _authToken = MutableLiveData<String?>()
    val authToken: LiveData<String?> get() = _authToken

    private val _feeds = MutableLiveData<List<Feed>>()
    val feeds: LiveData<List<Feed>> get() = _feeds

    private val _entries = MutableLiveData<List<Entry>>()
    val entries: LiveData<List<Entry>> get() = _entries

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> get() = _categories

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _selectedEntry = MutableLiveData<Entry?>()
    val selectedEntry: LiveData<Entry?> get() = _selectedEntry

    private val _selectedEntryFeeds = MutableLiveData<List<Feed>>()
    val selectedEntryFeeds: LiveData<List<Feed>> get() = _selectedEntryFeeds

    private val _userInfo = MutableLiveData<UserInfo?>()
    val userInfo: LiveData<UserInfo?> get() = _userInfo

    private val _categoryUnreadCounts = MutableLiveData<Map<Int, Int>>()
    val categoryUnreadCounts: LiveData<Map<Int, Int>> get() = _categoryUnreadCounts

    private val _feedUnreadCounts = MutableLiveData<Map<Int, Int>>()
    val feedUnreadCounts: LiveData<Map<Int, Int>> get() = _feedUnreadCounts

    private val currentEntryList = mutableStateListOf<Entry>()

    fun loadEntryById(entryId: Int) {
        viewModelScope.launch {
            try {
                val entry = RetrofitInstance.api.getEntry(_authToken.value ?: "", entryId)
                _selectedEntry.postValue(entry)

            } catch (e: Exception) {
                _error.postValue("Failed to load entry: ${e.message}")
            }
        }
    }

    fun setAuthToken(token: String) {
        Log.d("MinifluxViewModel", "Auth token set: $token")
        _authToken.value = token
    }

    fun markEntryAsRead(entryId: Int) {
        _authToken.value?.let { token ->
            Log.d("MinifluxViewModel", "Mark Entry Read with token: $token")
            viewModelScope.launch {
                try {
                    val response = RetrofitInstance.api.markEntryAsUnread(
                        token, EntryAndStatus(
                            listOf(entryId), "read"
                        )
                    )
                    Log.d(
                        "MinifluxViewModel",
                        "Entry marked successfully: ${response.isSuccessful} "
                    )
                } catch (e: Exception) {
                    Log.e("MinifluxViewModel", "Error mark entry", e)
                }
            }
        } ?: Log.d("MinifluxViewModel", "No auth token available, cannot mark entry")
    }

    fun markEntryAsUnread(entryId: Int) {
        _authToken.value?.let { token ->
            Log.d("MinifluxViewModel", "Mark Entry Unread with token: $token")
            viewModelScope.launch {
                try {
                    val response = RetrofitInstance.api.markEntryAsUnread(
                        token, EntryAndStatus(
                            listOf(entryId)
                        )
                    )
                    Log.d(
                        "MinifluxViewModel",
                        "Entry marked successfully: ${response.isSuccessful} "
                    )
                } catch (e: Exception) {
                    Log.e("MinifluxViewModel", "Error mark entry", e)
                }
            }
        } ?: Log.d("MinifluxViewModel", "No auth token available, cannot mark entry")
    }


    fun toggleStarred(entryId: Int, b: Boolean) {

        _authToken.value?.let { token ->
            Log.d("MinifluxViewModel", "Toggle entry mark with token: $token")
            viewModelScope.launch {
                try {
                    val response = RetrofitInstance.api.toggleEntryBookMark(token, entryId)
                    Log.d(
                        "MinifluxViewModel",
                        "Toggle marked successfully: ${response.isSuccessful} "
                    )
                    if (response.isSuccessful) {
                        // Trigger reloading the entry to reflect the new state
                        loadEntryById(entryId)
                    }
                } catch (e: Exception) {
                    Log.e("MinifluxViewModel", "Error mark entry", e)
                }
            }
        } ?: Log.d("MinifluxViewModel", "No auth token available, cannot mark entry")
    }

    fun fetchFeeds() {
        _authToken.value?.let { token ->
            Log.d("MinifluxViewModel", "Fetching feeds with token: $token")
            viewModelScope.launch {
                try {
                    val response = RetrofitInstance.api.getFeeds(token)
                    Log.d("MinifluxViewModel", "Feeds fetched successfully: ${response.size} items")
                    _feeds.postValue(response)
                } catch (e: Exception) {
                    Log.e("MinifluxViewModel", "Error fetching feeds", e)
                    _feeds.postValue(emptyList())
                }
            }
        } ?: Log.d("MinifluxViewModel", "No auth token available, cannot fetch feeds")
    }



    fun fetchCategories() {
        _authToken.value?.let { token ->
            Log.d("MinifluxViewModel", "Fetching categories with token: $token")
            viewModelScope.launch {
                try {
                    val response = RetrofitInstance.api.getCategories(token)
                    Log.d(
                        "MinifluxViewModel",
                        "Categories fetched successfully: ${response.size} items"
                    )
                    // 在MinifluxViewModel的fetchCategories()成功回调中添加
                    _categories.postValue(response)
//                    fetchUnreadEntryCountsByCategory() // 新增

                } catch (e: Exception) {
                    Log.e("MinifluxViewModel", "Error fetching categories", e)
                    _categories.postValue(emptyList())
                }
            }
        } ?: Log.d("MinifluxViewModel", "No auth token available, cannot fetch categories")
    }

    fun fetchEntries(status: String? = "unread", categoryId: Int? = null) {
        _authToken.value?.let { token ->
            Log.d(
                "MinifluxViewModel",
                "Fetching entries with token: $token, status: $status, categoryId: $categoryId"
            )
            viewModelScope.launch {
                try {
                    val response = RetrofitInstance.api.getEntries(token, status, categoryId)
                    Log.d(
                        "MinifluxViewModel",
                        "Entries fetched successfully: ${response.entries.size} items"
                    )
                    _entries.value = response.entries
                    _entries.postValue(response.entries)
                } catch (e: Exception) {
                    Log.d("MinifluxViewModel", "Response: ")
                    Log.e("MinifluxViewModel", "Error fetching entries", e)
                    _entries.postValue(emptyList())
                }
            }
        } ?: Log.d("MinifluxViewModel", "No auth token available, cannot fetch entries")
    }

    fun fetchEntries(
        feed: Feed,
        status: String? = "unread",
        order: String = "id",
        direction: String = "desc"
    ) {
        _authToken.value?.let { token ->
            Log.d(
                "MinifluxViewModel",
                "Fetching entries with token: $token, status: $status"
            )
            viewModelScope.launch {
                try {
                    val response = RetrofitInstance.api.getFeedEntries(
                        token,
                        feed.id,
                        status,
                        order,
                        direction
                    )
//                    Log.d("MinifluxViewModel", "Response: $response")
                    Log.d(
                        "MinifluxViewModel",
                        "Entries fetched successfully: ${response.entries.size} items"
                    )
                    _entries.value = response.entries
                    _entries.postValue(response.entries)
                } catch (e: Exception) {
                    Log.d("MinifluxViewModel", "Response: ")
                    Log.e("MinifluxViewModel", "Error fetching entries", e)
                    _entries.postValue(emptyList())
                }
            }
        } ?: Log.d("MinifluxViewModel", "No auth token available, cannot fetch entries")
    }

    fun refreshEntries() {
        Log.d("refreshEntries", "refreshEntries: Using Default refreshEntries()!")
        fetchEntries()
    }

    fun refreshEntriesByFeed(feedId: Int)  {
        Log.d("refreshEntriesByFeed", "refreshEntriesByFeed: using feedID  $feedId")

        viewModelScope.launch {
            // Logic to fetch entries by feedId
            fetchEntries(Feed(feedId)) // Replace with actual method
        }
    }

    fun refreshEntriesByCategory(categoryId: Int) {
        Log.d("refreshEntriesByCategory", "refreshEntriesByCategory: using categoryId $categoryId")
        viewModelScope.launch {
            // Logic to fetch entries by feedId
            fetchEntries("unread", categoryId) // Replace with actual method
        }
    }

    fun refreshFeeds() {
        fetchFeeds()

    }

    fun refreshCategories() {
        fetchCategories()
    }

    fun createFeed(feedUrl: String, categoryId: Int? = null) {
        _authToken.value?.let { token ->
            Log.d(
                "MinifluxViewModel",
                "Creating feed with token: $token, feedUrl: $feedUrl, categoryId: $categoryId"
            )
            viewModelScope.launch {
                try {
                    val feedRequest =
                        FeedCreationRequest(feed_url = feedUrl, category_id = categoryId)
                    val response = RetrofitInstance.api.createFeed(token, feedRequest)
                    Log.d("MinifluxViewModel", "Feed created successfully: ${response.title}")
                    fetchFeeds() // Refresh the feed list after creation
                } catch (e: Exception) {
                    Log.e("MinifluxViewModel", "Error creating feed", e)
                }
            }
        } ?: Log.d("MinifluxViewModel", "No auth token available, cannot create feed")
    }

    fun deleteFeed(feedId: Int) {
        _authToken.value?.let { token ->
            Log.d("MinifluxViewModel", "Deleting feed with token: $token, feedId: $feedId")
            viewModelScope.launch {
                try {
                    RetrofitInstance.api.deleteFeed(token, feedId)
                    Log.d("MinifluxViewModel", "Feed deleted successfully: feedId $feedId")
                    fetchFeeds() // Refresh the feed list after deletion
                } catch (e: Exception) {
                    Log.e("MinifluxViewModel", "Error deleting feed", e)
                }
            }
        }
    }

    fun fetchUserInfo() {
        _authToken.value?.let { token ->
            Log.d("MinifluxViewModel", "Fetching user info with token: $token")
            viewModelScope.launch {
                try {
                    val response = RetrofitInstance.api.getUserInfo(token)
                    Log.d(
                        "MinifluxViewModel",
                        "User info fetched successfully: ${response.username}"
                    )
                    _userInfo.postValue(response)
                } catch (e: Exception) {
                    Log.e("MinifluxViewModel", "Error fetching user info", e)
                    _userInfo.postValue(null)
                }
            }
        } ?: Log.d("MinifluxViewModel", "No auth token available, cannot fetch user info")
    }

    fun clearError() {
        _error.value = null
    }

    fun fetchTodayEntries() {
        val status = "unread"
        _authToken.value?.let { token ->
            Log.d(
                "MinifluxViewModel",
                "Fetching today's entries with token: $token, status: $status"
            )
            viewModelScope.launch {
                try {
                    val unixTime = System.currentTimeMillis() / 1000
                    val befeore24h = unixTime - 86400
                    Log.d("Time", "unixtimestamp before 24h is $befeore24h")
                    val response =
                        RetrofitInstance.api.getTodayEntries(token, published_after = befeore24h)
//                    Log.d("MinifluxViewModel", "Response: $response")
                    Log.d(
                        "MinifluxViewModel",
                        "Entries fetched successfully: ${response.entries.size} items"
                    )
                    _entries.value = response.entries
                    _entries.postValue(response.entries)
                } catch (e: Exception) {
                    Log.d("MinifluxViewModel", "Response: ")
                    Log.e("MinifluxViewModel", "Error fetching entries", e)
                    _entries.postValue(emptyList())
                }
            }
        } ?: Log.d("MinifluxViewModel", "No auth token available, cannot fetch entries")
    }

    fun markCategoryAsRead(id: Int) {
        _authToken.value?.let { token ->
            Log.d("MinifluxViewModel", "Mark Entry Read with token: $token")
            viewModelScope.launch {
                try {
                    val response = RetrofitInstance.api.markCategoryAsRead(
                        token, id
                    )
                    Log.d(
                        "MinifluxViewModel",
                        "Category marked successfully: ${response.isSuccessful} "
                    )
                } catch (e: Exception) {
                    Log.e("MinifluxViewModel", "Error mark Category", e)
                }
            }
        } ?: Log.d("MinifluxViewModel", "No auth token available, cannot mark Category as read")
    }

    fun fetchCategoriesUnreadCount() {
        _authToken.value?.let { token ->
            Log.d("MinifluxViewModel", "Fetching unread entry counts by category with token: $token")
            viewModelScope.launch {
                try {
                    val categories = RetrofitInstance.api.getCategories(token)
                    val unreadCounts = mutableMapOf<Int, Int>()
                    Log.d("MinifluxViewModel", "getCategoriesUnreadCount: ")
                    for (category in categories) {
//                        val response = RetrofitInstance.api.getEntries(token, status = "unread", categoryId = category.id)
                        val categoryUnreadCount= category.unreadCount
//                        Log.d("MinifluxViewModel", "Entries fetched successfully: ${response.entries.size} items")
                        Log.d("MinifluxViewModel", "Category id: ${category.id} have $categoryUnreadCount unread items")
//                        Log.d("MinifluxViewModel", "Fetched entries: ${response.entries}")
                        unreadCounts[category.id] = categoryUnreadCount ?: 0
                    }

                    _categoryUnreadCounts.postValue(unreadCounts)
                } catch (e: Exception) {
                    Log.e("MinifluxViewModel", "Error fetching unread entry counts by category", e)
                    _categoryUnreadCounts.postValue(emptyMap())
                }
            }
        } ?: Log.d("MinifluxViewModel", "No auth token available, cannot fetch unread entry counts by category")
    }

    fun fetchFeedsUnreadCount() {
        _authToken.value?.let { token ->
            Log.d("MinifluxViewModel", "Fetching unread entry counts by feed with token: $token")
            viewModelScope.launch {
                try {
                    val unreadCounterList = RetrofitInstance.api.getFeedCounters(token).unreads
                    val unreadCounts = mutableMapOf<Int, Int>()

                    Log.d("MinifluxViewModel", "getFeedsUnreadCount: unreadCounterList: $unreadCounterList")

                    for ((feedIdStr, count) in unreadCounterList) {
                        val feedId = feedIdStr.toInt()
                        unreadCounts[feedId] = count
                        Log.d("MinifluxViewModel", "Feed id: $feedId have $count unread items")
                    }

                    Log.d("MinifluxViewModel", "unreadCounts: $unreadCounts")
                    _feedUnreadCounts.postValue(unreadCounts)
                } catch (e: Exception) {
                    Log.e("MinifluxViewModel", "Error fetching unread entry counts by feed", e)
                    _feedUnreadCounts.postValue(emptyMap())
                }
            }
        } ?: Log.d("MinifluxViewModel", "No auth token available, cannot fetch unread entry counts by feed")
    }
    fun fetchCategoryFeeds(categoryId: Int){
        _authToken.value?.let { token ->
            Log.d("MinifluxViewModel", "Fetching category $categoryId feeds with token: $token")
            viewModelScope.launch {
                try {
                    val response = RetrofitInstance.api.getCategoryFeeds(categoryId = categoryId, authToken = token)
                    Log.d("MinifluxViewModel", "Feeds category $categoryId fetched successfully: ${response.size} items")
                    _selectedEntryFeeds.postValue(response)
                } catch (e: Exception) {
                    Log.e("MinifluxViewModel", "Error fetching feeds", e)
                    _selectedEntryFeeds.postValue(emptyList())
                }
            }
        } ?: Log.d("MinifluxViewModel", "No auth token available, cannot fetch feeds")

    }

    fun markFeedAsRead(feedid: Int) {
        _authToken.value?.let { token ->
            Log.d("MinifluxViewModel", "Mark feed as  Read with token: $token")
            viewModelScope.launch {
                try {
                    val response = RetrofitInstance.api.markFeedAsRead(
                        token, feedid
                    )
                    Log.d(
                        "MinifluxViewModel",
                        "Mark feed as read successfully: ${response.code()==204} "
                    )
                } catch (e: Exception) {
                    Log.e("MinifluxViewModel", "Error mark feed as read", e)
                }
            }
        } ?: Log.d("MinifluxViewModel", "No auth token available, cannot mark entry")
    }


    // 确保Entry ID类型一致
    fun setCurrentEntryList(entries: List<Entry>) {
        currentEntryList.clear()
        currentEntryList.addAll(entries)
        Log.d("setCurrentEntryList", "Set current list: now list is ${entries.map { it.id }}")

    }

    // MinifluxViewModel.kt
    fun navigateToPreviousEntry(currentId: Int): Int? {
        Log.d("navigateToPreviousEntry", "navigateToPreviousEntry: Func call!")
        Log.d("navigateToPreviousEntry", "Set current list: now list is ${currentEntryList.map { it.id }};and whether list null is ${currentEntryList.isEmpty()}")
        if (currentEntryList.isEmpty()) return null

        Log.d("Navigation", "Current ID: $currentId (Type: ${currentId::class.java.simpleName})")

        val currentIndex = currentEntryList.indexOfFirst { it.id == currentId }

        Log.d("Navigation", "navigateToPreviousEntry: now var currentIndex is $currentIndex")
        return when {
            currentIndex == -1 -> null
            currentIndex > 0 -> {
                val prevId = currentEntryList[currentIndex - 1].id
                Log.d("Navigation", "Returning prevId: $prevId")
                prevId}
            else -> null
        }.also {
            if (it == null) Log.w("Navigation", "Invalid previous entry")
        }

    }

    fun navigateToNextEntry(currentId: Int): Int? {
        Log.d("navigateToNextEntry", "navigateToNextEntry: Func call!")
        Log.d("navigateToNextEntry", "Set current list: now list is ${currentEntryList.map { it.id }};and whether list null is ${currentEntryList.isEmpty()}")
        if (currentEntryList.isEmpty()) return null

        Log.d("Navigation", "Current ID: $currentId (Type: ${currentId::class.java.simpleName})")

        val currentIndex = currentEntryList.indexOfFirst { it.id == currentId }

        Log.d("Navigation", "navigateToNextEntry: now var currentIndex is $currentIndex")
        return when {
            currentIndex == -1 -> null
            currentIndex < currentEntryList.lastIndex -> {
                val nextId = currentEntryList[currentIndex + 1].id
                Log.d("Navigation", "Returning nextId: $nextId")
                nextId
            }
            else -> null
        }.also { if (it == null) Log.w("Navigation", "Invalid next entry") }
    }

    fun fetchFeedCounters() :  Map<String, Int> {

        var unreadCount= mutableMapOf<String,Int>()
        _authToken.value?.let { token ->
            Log.d("MinifluxViewModel", "Fetching feed counters with token: $token")
            viewModelScope.launch {
                try {
                    val response = RetrofitInstance.api.getFeedCounters(token)
                    Log.d("MinifluxViewModel", "Feed counters fetched successfully: ${response.unreads.size} items")
                    // 这里可以处理未读计数数据
                    unreadCount= response.unreads as MutableMap<String, Int>
                } catch (e: Exception) {
                    Log.e("MinifluxViewModel", "Error fetching feed counters", e)
                }
            }
        } ?: Log.d("MinifluxViewModel", "No auth token available, cannot fetch feed counters")
        return unreadCount

    }

    // 保存 Feed 的自定义排序
    fun saveFeedOrder(feedList: List<Feed>) {
        viewModelScope.launch {
            try {
                // 转换为 Map<FeedId, OrderIndex>
                val feedOrderMap = feedList.mapIndexed { index, feed ->
                    feed.id to index
                }.toMap()
                DataStoreManager.saveFeedOrder(feedOrderMap)
                Log.d("MinifluxViewModel", "Feed order saved successfully to DataStore")
            } catch (e: Exception) {
                Log.e("MinifluxViewModel", "Error saving feed order", e)
            }
        }
    }

    // 加载自定义排序的 Feeds
    suspend fun loadCustomFeedOrder(feeds: List<Feed>): List<Feed> {
        return try {
            val feedOrderMap = DataStoreManager.getFeedOrder()
            if (feedOrderMap.isEmpty()) {
                // 如果没有保存的排序，返回按标题排序的列表
                feeds.sortedBy { it.title }
            } else {
                // 按照保存的顺序排列
                feeds.sortedBy { feed -> feedOrderMap[feed.id] ?: Int.MAX_VALUE }
            }
        } catch (e: Exception) {
            Log.e("MinifluxViewModel", "Error loading feed order", e)
            feeds.sortedBy { it.title }
        }
    }
}

data class FeedWithUnreadCount(
    val id: Int,
    val title: String, // 或其他字段
    val unreadCount: Int // 未读计数
)
// ViewModel状态枚举
sealed class FeedState {
    object Loading : FeedState() // 加载中
    data class Success(val feeds: List<FeedWithUnreadCount>) : FeedState() // 成功，包含合并数据
    data class Error(val message: String) : FeedState() // 错误
}
class FeedListViewModel(private val apiService: MinifluxViewModel) : ViewModel() {
    private val _feedState = MutableStateFlow<FeedState>(FeedState.Loading)
    val feedState: StateFlow<FeedState> = _feedState.asStateFlow()
    // 缓存计数在内存中，减少重复请求
    private var cachedCounters: Map<Int, Int>? = null // 键是订阅源ID，值是未读计数
    // 初始化时加载数据（当ViewModel创建时调用，或在UI导航时触发）
    init {
        loadFeedsAndCounters()
    }
    // 核心函数：并发加载订阅源列表和计数
    fun loadFeedsAndCounters() {
        viewModelScope.launch {
            _feedState.value = FeedState.Loading // 设置加载状态
            try {
                // 使用SupervisorJob防止一个请求失败影响另一个
                val deferredFeeds = async(SupervisorJob()) { apiService.fetchFeeds() }
                val deferredCounters = async(SupervisorJob()) { apiService.fetchFeedCounters() }
                val feeds = deferredFeeds.await()
                val counters = deferredCounters.await()
                // 合并数据：遍历feeds列表，匹配counters中的未读计数

            } catch (e: Exception) {
                _feedState.value = FeedState.Error("Failed to load feeds: ${e.message}") // 处理错误
            }
        }
    }

    // 如果在其他界面需要特定订阅源的未读计数（可选）
    fun getUnreadCountForFeed(feedId: Int): Int {
        return cachedCounters?.get(feedId) ?: 0 // 从内存缓存中获取，默认0
    }
}