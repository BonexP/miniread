package com.i.miniread.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.i.miniread.network.Feed
import com.i.miniread.viewmodel.MinifluxViewModel

@Composable
fun FeedListScreen(viewModel: MinifluxViewModel, onFeedSelected: (Int) -> Unit) {
    val feeds by viewModel.feeds.observeAsState(emptyList())

    val feedsWithOutDisabled = feeds.filter { !it.disabled }.sortedBy { it.title }
    Log.d("FeedListScreen", "Number of feeds: ${feeds.size}")
    Log.d("FeedListScreen", "Number of Enabled feeds: ${feedsWithOutDisabled.size}")

//    Log.d("FeedListScreen","Feeds: $feeds")
// Show placeholder if entries list is empty
    if (feedsWithOutDisabled.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "无条目可显示",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Gray
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(feedsWithOutDisabled) { feed ->
                FeedItem(feed, onClick = {
                    Log.d("FeedListScreen", "Feed Using:  $feed")
                    onFeedSelected(feed.id)
                })
            }
        }
    }
}

@Composable
fun FeedItem(feed: Feed, onClick: () -> Unit) {
    val title = feed.title
    val id = feed.id

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
//        shape = MaterialTheme.shapes.medium,
//        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = id.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = feed.disabled.toString(),
                color = MaterialTheme.colorScheme.onSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
