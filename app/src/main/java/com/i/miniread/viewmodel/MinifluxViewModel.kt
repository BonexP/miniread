package com.i.miniread.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.i.miniread.network.Category
import com.i.miniread.network.Entry
import com.i.miniread.network.EntryAndStatus
import com.i.miniread.network.FeedCreationRequest
import com.i.miniread.network.RetrofitInstance
import com.i.miniread.network.UserInfo
import kotlinx.coroutines.launch

class MinifluxViewModel : ViewModel() {
    private val _authToken = MutableLiveData<String?>()
    val authToken: LiveData<String?> get() = _authToken

    private val _feeds = MutableLiveData<List<Entry>>()
    val feeds: LiveData<List<Entry>> get() = _feeds

    private val _entries = MutableLiveData<List<Entry>>()
    val entries: LiveData<List<Entry>> get() = _entries

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> get() = _categories

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _selectedEntry = MutableLiveData<Entry?>()
    val selectedEntry: LiveData<Entry?> get() = _selectedEntry

    private val _userInfo = MutableLiveData<UserInfo?>()
    val userInfo: LiveData<UserInfo?> get() = _userInfo

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

    fun markEntryAsRead(entryId: Int){
        _authToken.value?.let { token ->
            Log.d("MinifluxViewModel", "Mark Entry Read with token: $token")
            viewModelScope.launch {
                try {
                    val response = RetrofitInstance.api.markEntryAsUnread(token, EntryAndStatus(entryId,"read"))
                    Log.d("MinifluxViewModel", "Entry marked successfully: $response items")
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
                    _categories.postValue(response)
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

    fun fetchEntries(feed: Entry ,status: String?=  "unread") {
        _authToken.value?.let { token ->
            Log.d(
                "MinifluxViewModel",
                "Fetching entries with token: $token, status: $status"
            )
            viewModelScope.launch {
                try {
                    val response = RetrofitInstance.api.getFeedEntries(token, feed.id)
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
}
