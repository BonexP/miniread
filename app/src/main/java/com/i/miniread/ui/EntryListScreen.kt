package com.i.miniread.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.i.miniread.network.Entry
import com.i.miniread.viewmodel.MinifluxViewModel

@Composable
fun EntryListScreen(viewModel: MinifluxViewModel = viewModel()) {
    val entries by viewModel.entries.observeAsState(emptyList())

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(entries) { entry ->
            EntryItem(entry, onClick = {
                // Navigate to ArticleDetailScreen here
                viewModel.loadEntryById(entry.id)
            })
        }
    }
}

@Composable
fun EntryItem(entry: Entry, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        entry.title?.let { Text(text = it, style = MaterialTheme.typography.titleLarge) }
        Text(text = (entry.content?.take(100) ?: "" ) + "...", style = MaterialTheme.typography.bodyMedium)
    }
}
