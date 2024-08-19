package com.i.miniread.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.i.miniread.network.AuthResponse
import com.i.miniread.network.Entry
import com.i.miniread.network.RetrofitInstance
import kotlinx.coroutines.launch

class MinifluxViewModel : ViewModel() {
    private val _authToken = MutableLiveData<String?>()
    val authToken: LiveData<String?> get() = _authToken

    private val _feeds = MutableLiveData<List<Entry>>()
    val feeds: LiveData<List<Entry>> get() = _feeds

    fun login(username: String, password: String, onError:  (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response: AuthResponse = RetrofitInstance.api.login(username, password)
                _authToken.value = response.token
            } catch (e: Exception) {
                _authToken.value = null
                onError("登录失败，请检查您的凭据。")
            }
        }
    }

    fun fetchFeeds() {
        _authToken.value?.let { token ->
            viewModelScope.launch {
                try {
                    val response = RetrofitInstance.api.getFeeds(token)
                    _feeds.postValue(response)
                } catch (e: Exception) {
                    _feeds.postValue(emptyList())
                }
            }
        }
    }
}
