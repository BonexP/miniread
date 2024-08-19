package com.i.miniread.ui

import android.util.Log
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
import com.i.miniread.network.Category
import com.i.miniread.viewmodel.MinifluxViewModel

@Composable
fun CategoryListScreen(viewModel: MinifluxViewModel) {
    val categories by viewModel.categories.observeAsState(emptyList())
    val error by viewModel.error.observeAsState()

    if (error != null) {
        Text(
            text = error ?: "Unknown error occurred",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(categories) { category ->
                CategoryItem(category, onClick = {
                    viewModel.fetchEntries(categoryId = category.id)
                })
            }
        }
        if (categories.isEmpty()) {
            Text(
                text = "No categories available",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Log.d("CategoryListScreen", "there are some categories")
        }
    }
}

@Composable
fun CategoryItem(category: Category, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Text(text = category.title, style = MaterialTheme.typography.titleLarge)
    }
}
