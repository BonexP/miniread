package com.i.miniread.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.i.miniread.network.Entry
import com.i.miniread.viewmodel.MinifluxViewModel

@Composable
fun EntryListScreen(viewModel: MinifluxViewModel, navController: NavController) {
    val entries by viewModel.entries.observeAsState(emptyList())

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(entries) { entry ->
            EntryItem(entry, onClick = {
                viewModel.loadEntryById(entry.id)
                navController.navigate("articleDetail?entryId=${entry.id}")
            })
        }
    }
}

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
        }
    }
}
