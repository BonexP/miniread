package com.i.miniread.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.i.miniread.network.Entry
import com.i.miniread.network.RetrofitInstance
import kotlinx.coroutines.launch

class MinifluxViewModel : ViewModel() {
    private val _feeds = MutableLiveData<List<Entry>>()
    val feeds: LiveData<List<Entry>> get() = _feeds

    fun fetchFeeds(authToken: String) {
        viewModelScope.launch {
            try {
                // 通过 Retrofit 实例正确调用 API 获取数据
                val response = RetrofitInstance.api.getFeeds(authToken)
                _feeds.postValue(response)
            } catch (e: Exception) {
                // 捕获并打印异常信息
                e.printStackTrace()
            }
        }
    }
}
