package com.i.miniread.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.i.miniread.network.Category
import com.i.miniread.viewmodel.MinifluxViewModel

@Composable
fun CategoryListScreen(viewModel: MinifluxViewModel, onCategorySelected: (Int) -> Unit) {
    Log.d("CategoryListScreen", "CategoryListScreen: Enter CategoryListScreen")
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
                CategoryItem(
                    category = category,
                    onClick = {
                        viewModel.fetchEntries(categoryId = category.id, status = "unread")
                        onCategorySelected(category.id) // Navigate to EntryListScreen
                    },
                    onMarkAsRead = { viewModel.markCategoryAsRead(category.id) },
                    onShowSubscriptions = { viewModel.fetchChildSubscriptions(category.id) }
                )
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
fun CategoryItem(
    category: Category,
    onClick: () -> Unit,
    onMarkAsRead: () -> Unit,
    onShowSubscriptions: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ensure Column occupies remaining space
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${category.title} articles",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp) // Adjusted padding for spacing
                    )
                }

                // Button to show subscriptions with icon
                IconButton(onClick = onShowSubscriptions) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Show Subscriptions"
                    )
                }

                // Button to mark as read with icon
                IconButton(onClick = onMarkAsRead) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Mark as Read"
                    )
                }
            }
        }
    }
}
