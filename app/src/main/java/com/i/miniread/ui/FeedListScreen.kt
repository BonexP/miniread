package com.i.miniread.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.i.miniread.network.Entry
import com.i.miniread.viewmodel.MinifluxViewModel


@Composable
fun FeedListScreen(viewModel: MinifluxViewModel) {
    val feeds by viewModel.feeds.observeAsState(emptyList())

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(feeds) { feed ->
            FeedItem(feed, onClick = {
                // 处理点击事件，跳转到文章详情界面
            })
        }
    }
}


@Composable
fun FeedItem(feed: Entry, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Text(text = feed.title, style = MaterialTheme.typography.headlineLarge)
        Text(text = feed.published_at, style = MaterialTheme.typography.bodyLarge)
    }
}
