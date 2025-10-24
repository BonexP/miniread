package com.i.miniread.ui

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.i.miniread.viewmodel.MinifluxViewModel

@Composable
fun SubFeedScreen(viewModel: MinifluxViewModel, categoryId: Int, onFeedSelected: (Int) -> Unit) {
    val feeds by viewModel.selectedEntryFeeds.observeAsState(emptyList())
    val feedUnreadCounts by viewModel.feedUnreadCounts.observeAsState(emptyMap())

    // 获取分类下的订阅源和未读计数
    LaunchedEffect(categoryId) {
        viewModel.fetchCategoryFeeds(categoryId)
        viewModel.fetchFeedsUnreadCount()
    }

    val feedsWithOutDisabled = feeds.filter { !it.disabled }.sortedBy { it.title }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(feedsWithOutDisabled) { feed ->
            FeedItem(
                feed,
                unreadCount = feedUnreadCounts[feed.id] ?: 0,
                onClick = { onFeedSelected(feed.id) },
                onMarkAsRead={
                    viewModel.markFeedAsRead(feed.id)
                    Log.d("SubFeedScreen", "FeedListScreen: invoke onMarkAsRad in SubFeedScreen!")
                }
            )
        }
    }
}
