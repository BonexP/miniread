package com.i.miniread.ui

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.i.miniread.viewmodel.MinifluxViewModel

@Composable
fun ArticleDetailScreen(viewModel: MinifluxViewModel, entryId: Int) {
    val selectedEntry by viewModel.selectedEntry.observeAsState()


    // 仅在需要时加载数据
    LaunchedEffect(entryId) {
        if (selectedEntry?.id != entryId) {
            viewModel.loadEntryById(entryId)
        }
    }
    val tag = "ArticleDetailScreen"
    Log.d(tag, "Now viewing entryId=$entryId")
    Log.d(tag, "ArticleDetailScreen: entry value: ${selectedEntry?.id}")

    if (selectedEntry == null || selectedEntry?.id != entryId) {
        Text(text = "Loading...", modifier = Modifier.padding(16.dp))
    } else {
        Column(modifier = Modifier.padding(16.dp)) {
            selectedEntry!!.title?.let { Text(text = it, style = MaterialTheme.typography.headlineMedium) }
            Spacer(modifier = Modifier.height(8.dp))
            selectedEntry!!.content?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}
