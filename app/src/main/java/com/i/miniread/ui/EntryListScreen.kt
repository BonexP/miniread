package com.i.miniread.ui

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.i.miniread.network.Entry
import com.i.miniread.viewmodel.MinifluxViewModel
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EntryListScreen(viewModel: MinifluxViewModel, navController: NavController, feedId: Int? = null, categoryId: Int? = null) {
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
            EntryItem(entry, onClick = {
                navController.navigate("articleDetail?entryId=${entry.id}")
            })
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EntryItem(entry: Entry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = entry.title ?: "Untitled",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text= entry.feed.title,
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