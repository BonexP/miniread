package com.i.miniread.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.i.miniread.viewmodel.MinifluxViewModel

@Composable
fun ArticleDetailScreen(viewModel: MinifluxViewModel) {
    val selectedEntry = viewModel.selectedEntry.observeAsState()

    selectedEntry.value?.let { entry ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(text = entry.title ?: "Untitled", style = MaterialTheme.typography.headlineLarge)
            Text(text = entry.published_at ?: "Unknown Date", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = entry.content ?: "No content available", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
