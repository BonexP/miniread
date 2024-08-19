package com.i.miniread.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.i.miniread.network.AuthResponse
import com.i.miniread.network.Entry
import com.i.miniread.network.RetrofitInstance
import kotlinx.coroutines.launch

class MinifluxViewModel : ViewModel() {
    private val _feeds = MutableLiveData<List<Entry>>()
    val feeds: LiveData<List<Entry>> get() = _feeds

    private val _authToken = MutableLiveData<String?>()
    val authToken: LiveData<String?> get() = _authToken

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val response: AuthResponse = RetrofitInstance.api.login(username, password)
                _authToken.value = response.token
            } catch (e: Exception) {
                _authToken.value = null
            }
        }
    }

    fun fetchFeeds(authToken: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getFeeds(authToken)
                _feeds.postValue(response)
            } catch (e: Exception) {
                _feeds.postValue(emptyList())
            }
        }
    }
}
