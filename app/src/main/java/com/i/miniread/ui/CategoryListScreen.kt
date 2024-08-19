package com.i.miniread.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.i.miniread.viewmodel.MinifluxViewModel

@Composable
fun CategoryListScreen(viewModel: MinifluxViewModel) {
    val categories by viewModel.categories.observeAsState(emptyList())

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(categories) { category ->
            CategoryItem(category, onClick = {
                viewModel.fetchEntries(categoryId = category.id)
            })
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
        Text(text = category.title, style = MaterialTheme.typography.headlineLarge)
    }
}
