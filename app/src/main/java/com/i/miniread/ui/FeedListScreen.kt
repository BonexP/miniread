package com.i.miniread.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.i.miniread.Screen
import com.i.miniread.network.Entry
import com.i.miniread.viewmodel.MinifluxViewModel

@Composable
fun FeedListScreen(viewModel: MinifluxViewModel, navController: NavController) {
    val feeds by viewModel.feeds.observeAsState(emptyList())

    Log.d("FeedListScreen", "Number of feeds: ${feeds.size}")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(feeds) { feed ->
            FeedItem(feed, onClick = {
                viewModel.setSelectedEntry(feed)
                navController.navigate(Screen.ArticleDetail.route)
            })
        }
    }
}

@Composable
fun FeedItem(feed: Entry, onClick: () -> Unit) {
    val title = feed.title ?: "Untitled"
    val publishedAt = feed.published_at ?: "Unknown Date"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = publishedAt, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
