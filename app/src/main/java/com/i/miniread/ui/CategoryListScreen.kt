package com.i.miniread.ui

import android.util.Log
import androidx.compose.foundation.border
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.i.miniread.BuildConfig
import com.i.miniread.network.Category
import com.i.miniread.viewmodel.MinifluxViewModel

@Composable
fun CategoryListScreen(
    viewModel: MinifluxViewModel,
    onCategorySelected: (Int) -> Unit,
    onShowSubscriptions: (Int) -> Unit
) {
    // 在CategoryListScreen顶部添加
    LaunchedEffect(Unit) {
        viewModel.fetchCategoriesUnreadCount()
        viewModel.fetchCategories() // 确保已存在
        viewModel.fetchFeedsUnreadCount()
    }

    Log.d("CategoryListScreen", "CategoryListScreen: Enter CategoryListScreen")
    val categories by viewModel.categories.observeAsState(emptyList())
    val error by viewModel.error.observeAsState()
    val unreadCounts by viewModel.categoryUnreadCounts.observeAsState(emptyMap())
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
                    unreadCount = unreadCounts[category.id]?:0,
                    onClick = {
                        viewModel.fetchEntries(categoryId = category.id, status = "unread")
                        onCategorySelected(category.id) // Navigate to EntryListScreen
                    },
                    onMarkAsRead = { viewModel.markCategoryAsRead(category.id) },
                    onShowSubscriptions = { onShowSubscriptions(category.id) }
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
    unreadCount: Int,
    //TODO 这里unreadCount和Category的unreadCount有重复,可以精简代码，直接传入category,让UI直接使用category.unreadCount
    onClick: () -> Unit,
    onMarkAsRead: () -> Unit,
    onShowSubscriptions: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) } // 新增对话框状态

    // 新增确认对话框
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    "标记分类为已读",
                    color = if (BuildConfig.IS_EINK) Color.Black else MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "确定要将${category.title}分类下的所有条目标记为已读吗？此操作不可撤销。",
                    color = if (BuildConfig.IS_EINK) Color.Black else MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onMarkAsRead() // 执行实际标记操作
                    }
                ) {
                    Text(
                        "确认",
                        color = if (BuildConfig.IS_EINK) Color.Black else MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false }
                ) {
                    Text(
                        "取消",
                        color = if (BuildConfig.IS_EINK) Color.Black else MaterialTheme.colorScheme.primary
                    )
                }
            },
            containerColor = if (BuildConfig.IS_EINK) Color.White else AlertDialogDefaults.containerColor,
            modifier = if (BuildConfig.IS_EINK) {
                Modifier.border(
                    width = 2.dp,
                    color = Color.Black,
                    shape = AlertDialogDefaults.shape
                )
            } else {
                Modifier
            }
        )
    }

    Card(
        colors = if (BuildConfig.IS_EINK) {
            CardDefaults.cardColors(
                containerColor = Color.White
            )
        } else {
            CardDefaults.cardColors()
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(
                if (BuildConfig.IS_EINK) {
                    Modifier.border(
                        width = 2.dp,
                        color = Color.Black,
                        shape = MaterialTheme.shapes.medium
                    )
                } else {
                    Modifier
                }
            )
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
                        color = if (BuildConfig.IS_EINK) Color.Black else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text =  if (unreadCount > 0) "$unreadCount 未读条目" else "全部已读" ,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (BuildConfig.IS_EINK) Color.DarkGray else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp) // Adjusted padding for spacing
                    )
                }
                // Button to mark as read with icon
                IconButton(onClick = { showConfirmDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Mark as Read"
                    )
                }
                // Button to show subscriptions with icon
                IconButton(onClick = onShowSubscriptions) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Show Subscriptions"
                    )
                }
            }
        }
    }
}
