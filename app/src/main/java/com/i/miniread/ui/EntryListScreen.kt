package com.i.miniread.ui

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.i.miniread.network.Entry
import com.i.miniread.viewmodel.MinifluxViewModel
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EntryListScreen(
    viewModel: MinifluxViewModel,
    navController: NavController,
    feedId: Int? = null,
    categoryId: Int? = null
) {
    Log.d("EntryListScreen", "EntryListScreen: EnterEntryScreen!")
    Log.d("EntryListScreen", "EntryListScreen: feedId $feedId categoryId $categoryId")
    val entries by viewModel.entries.observeAsState(emptyList())

    // Initialize the entries when entering the screen
    LaunchedEffect(feedId, categoryId) {
        if (feedId != null) {
            viewModel.refreshEntriesByFeed(feedId)
        } else if (categoryId != null) {
            viewModel.refreshEntriesByCategory(categoryId)
        }
    }
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(entries) { entry ->
            EntryItem(viewModel, entry, onClick = {
                navController.navigate("articleDetail?entryId=${entry.id}")
            })
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EntryItem(viewModel: MinifluxViewModel, entry: Entry, onClick: () -> Unit) {
    val isVisible = remember { mutableStateOf(true) }
    val swipeThreshold = 100f
    var offsetX by remember { mutableFloatStateOf(0f) }

    AnimatedVisibility(
        visible = isVisible.value,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background to indicate swipe action
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(16.dp),
//                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Mark as Read",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .padding(vertical = 8.dp)
                    .clickable { onClick() }
                    .border(BorderStroke(0.5.dp, Color.Black), shape = RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                offsetX = (offsetX + dragAmount).coerceIn(-swipeThreshold * 2, 0f)
                                Log.d(
                                    "EntryItem",
                                    "EntryItem: Dragging entry with id ${entry.id}, offsetX: $offsetX"
                                )
                            },
                            onDragEnd = {
                                if (offsetX < -swipeThreshold) {
                                    Log.d(
                                        "EntryItem",
                                        "EntryItem: Swiped sufficiently on entry with id ${entry.id}, marking as read"
                                    )
                                    // Mark entry as read when swiped sufficiently
                                    viewModel.markEntryAsRead(entry.id)
                                    isVisible.value = false
                                } else {
                                    Log.d(
                                        "EntryItem",
                                        "EntryItem: Swipe not sufficient for entry with id ${entry.id}, resetting"
                                    )
                                    offsetX = 0f
                                }
                            }
                        )
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = entry.title ?: "Untitled",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = entry.feed.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    entry.published_at?.let {
                        Text(
                            text = localizePublishTime(it),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

    }
}


@RequiresApi(Build.VERSION_CODES.O)
fun localizePublishTime(publishedAt: String): String {
    // 解析文章发布时间
    val utcTime = OffsetDateTime.parse(publishedAt)

    // 将时间转换为本地时区
    val localTime = utcTime.atZoneSameInstant(ZoneId.systemDefault())

    // 定义你想要的日期格式，这里以 "yyyy-MM-dd HH:mm" 为例
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    // 返回格式化后的本地化时间字符串
    return localTime.format(formatter)
}