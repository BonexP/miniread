package com.i.miniread.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.i.miniread.viewmodel.MinifluxViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TodayEntryListScreen(viewModel: MinifluxViewModel, navController: NavController) {
    // 在进入此屏幕时仅加载当天条目
    LaunchedEffect(Unit) {
        viewModel.fetchTodayEntries()
    }

    // 复用现有的 EntryListScreen 逻辑
    val todayEntries by viewModel.entries.observeAsState(emptyList())
    if (todayEntries.isEmpty()) {
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
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(todayEntries) { entry ->
                EntryItem(
                    viewModel, entry,
                    onClick = {
                        navController.navigate("articleDetail?entryId=${entry.id}")
                    },

                    )
            }
        }
    }
}
