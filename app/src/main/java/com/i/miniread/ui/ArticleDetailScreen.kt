package com.i.miniread.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import com.i.miniread.network.Entry
import com.i.miniread.viewmodel.MinifluxViewModel

@Composable
fun ArticleDetailScreen(viewModel: MinifluxViewModel, entryId: LiveData<Entry?>) {
    val entries = viewModel.entries.observeAsState().value
    val entry = entries?.find { it.id.equals(entryId)  }

    if (entry == null) {
        Text(
            text = "Article not found",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
    } else {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = entry.title ?: "No title", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = entry.content ?: "No content available", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
