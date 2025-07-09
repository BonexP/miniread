package com.i.miniread.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.i.miniread.network.Feed
import com.i.miniread.viewmodel.MinifluxViewModel

@Composable
fun FeedListScreen(
    viewModel: MinifluxViewModel,
    onFeedSelected: (Int) -> Unit,
) {
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
                }, onMarkAsRead = {
                    viewModel.markFeedAsRead(feed.id)
                    Log.d("FeedList", "FeedListScreen: invoke onMarkAsRad in FeedlistScreen!")
                })
            }
        }
    }
}

@Composable
fun FeedItem(
    feed: Feed,
    unreadCount: Int = feed.unreadCount,
    //TODO 这里同样，同时传入feed和unreadCount并不美观，需要在后层聚合
    onClick: () -> Unit,
    onMarkAsRead: () -> Unit
) {
    val title = feed.title
    val id = feed.id
    var showConfirmDialog by remember { mutableStateOf(false) } // 新增状态
    // 新增确认对话框
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("标记订阅源为已读") },
            text = { Text("确定要将${feed.title}订阅源的所有条目标记为已读吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onMarkAsRead() // 实际执行标记操作
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            )
            {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text =  if (unreadCount > 0) "$unreadCount 未读条目" else "全部已读" ,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp) // Adjusted padding for spacing
                    )

                }
                // Button to mark as read with icon
                IconButton(onClick = { showConfirmDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Mark as Read"
                    )
                }
            }
        }
    }
}
