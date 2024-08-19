package com.i.miniread.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.i.miniread.network.Entry
import com.i.miniread.network.RetrofitInstance
import kotlinx.coroutines.launch

class MinifluxViewModel : ViewModel() {
    private val _authToken = MutableLiveData<String?>()
    val authToken: LiveData<String?> get() = _authToken

    private val _feeds = MutableLiveData<List<Entry>>()
    val feeds: LiveData<List<Entry>> get() = _feeds

    fun setAuthToken(token: String) {
        Log.d("MinifluxViewModel", "Auth token set: $token")
        _authToken.value = token
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
}
