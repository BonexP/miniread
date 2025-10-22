package com.i.miniread.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.materialIcon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.i.miniread.network.Feed
import com.i.miniread.viewmodel.MinifluxViewModel
import kotlinx.coroutines.launch

@Composable
fun FeedListScreen(
    viewModel: MinifluxViewModel,
    onFeedSelected: (Int) -> Unit,
) {
    val feeds by viewModel.feeds.observeAsState(emptyList())
    val feedUnreadCounts by viewModel.feedUnreadCounts.observeAsState(emptyMap())
    var isEditMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 在FeedListScreen顶部添加，用于获取未读计数
    LaunchedEffect(Unit) {
        viewModel.fetchFeedsUnreadCount()
        viewModel.fetchFeeds()
    }

    // 使用 remember 保存可变的排序列表
    val sortedFeeds = remember { mutableStateListOf<Feed>() }

    // 当 feeds 变化时，加载自定义排序
    LaunchedEffect(feeds) {
        if (feeds.isNotEmpty()) {
            val customSorted = viewModel.loadCustomFeedOrder(feeds.filter { !it.disabled })
            sortedFeeds.clear()
            sortedFeeds.addAll(customSorted)
        }
    }

    Log.d("FeedListScreen", "Number of feeds: ${feeds.size}")
    Log.d("FeedListScreen", "Number of Enabled feeds: ${sortedFeeds.size}")

    Box(modifier = Modifier.fillMaxSize()) {
        if (sortedFeeds.isEmpty()) {
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 88.dp
                )
            ) {
                itemsIndexed(
                    items = sortedFeeds,
                    key = { _, feed -> feed.id }
                ) { index, feed ->
                    FeedItem(
                        feed = feed,
                        unreadCount = feedUnreadCounts[feed.id] ?: 0,
                        isEditMode = isEditMode,
                        onMoveUp = if (index > 0) {
                            {
                                val temp = sortedFeeds[index]
                                sortedFeeds[index] = sortedFeeds[index - 1]
                                sortedFeeds[index - 1] = temp
                            }
                        } else null,
                        onMoveDown = if (index < sortedFeeds.size - 1) {
                            {
                                val temp = sortedFeeds[index]
                                sortedFeeds[index] = sortedFeeds[index + 1]
                                sortedFeeds[index + 1] = temp
                            }
                        } else null,
                        onClick = {
                            if (!isEditMode) {
                                Log.d("FeedListScreen", "Feed Using: $feed")
                                onFeedSelected(feed.id)
                            }
                        },
                        onMarkAsRead = {
                            viewModel.markFeedAsRead(feed.id)
                            Log.d("FeedList", "FeedListScreen: invoke onMarkAsRead in FeedlistScreen!")
                        }
                    )
                }
            }
        }

        // 浮动操作按钮 - 切换编辑模式
        FloatingActionButton(
            onClick = {
                if (isEditMode) {
                    // 退出编辑模式时保存排序
                    scope.launch {
                        viewModel.saveFeedOrder(sortedFeeds)
                    }
                }
                isEditMode = !isEditMode
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = if (isEditMode) Icons.Default.Done else Icons.Default.Edit,
                contentDescription = if (isEditMode) "保存排序" else "编辑排序"
            )
        }
    }
}

@Composable
fun FeedItem(
    feed: Feed,
    unreadCount: Int = feed.unreadCount,
    isEditMode: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onClick: () -> Unit,
    onMarkAsRead: () -> Unit
) {
    val title = feed.title
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("标记订阅源为已读") },
            text = { Text("确定要将${feed.title}订阅源的所有条目标记为已读吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onMarkAsRead()
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
            .clickable(enabled = !isEditMode) { onClick() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 编辑模式下显示拖拽手柄
                if (isEditMode) {
                    Row {
                        if (onMoveUp != null) {
                            IconButton(onClick = onMoveUp) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move Up"
                                )
                            }
                        }
                        if (onMoveDown != null) {
                            IconButton(onClick = onMoveDown) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move Down"
                                )

                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (unreadCount > 0) "$unreadCount 未读条目" else "全部已读",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // 非编辑模式下显示标记已读按钮
                if (!isEditMode) {
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
}
