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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.i.miniread.viewmodel.MinifluxViewModel

@Composable
fun ArticleDetailScreen(viewModel: MinifluxViewModel = viewModel(), entryId: Int) {
    // Load the entry when the screen is composed
    LaunchedEffect(entryId) {
        viewModel.loadEntryById(entryId)
    }

    val tag = "ArticleDetailScreen"
    Log.d(tag,"Now viewing entryId=${entryId}")
    // Observe the selected entry
    val entry = viewModel.selectedEntry.observeAsState().value
    Log.d(tag, "ArticleDetailScreen: entry value :${entry} ")

    if (entry == null) {
        // Display a loading message or an error if the entry is not available
        Text(
            text = "Loading...",
            modifier = Modifier.padding(16.dp)
        )
    } else {
        // Display the article content
        Column(modifier = Modifier.padding(16.dp)) {
            entry.title?.let {
                Text(text = it, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                entry.content?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }
}