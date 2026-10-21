package com.i.miniread.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.i.miniread.BuildConfig
import com.i.miniread.R
import com.i.miniread.network.Feed
import com.i.miniread.util.FeedSelection

/**
 * 订阅源白名单选择器。
 *
 * 设计要点（改动前必读）：
 * - `csv` 是**唯一真源**，只做增删，**从不因搜索过滤或空列表裁剪** —— 配置里有、但当前订阅
 *   列表里没有的编号（订阅源被删 / 尚未加载）会进「可能已删除」分区，而不是被静默丢掉。
 * - `feeds == null` 表示尚未加载，`emptyList()` 表示已加载但为空。`ViewModel.fetchFeeds()`
 *   失败时会 `postValue(emptyList())`，所以两者必须分开提示，且都不清空已选。
 * - 用自绘 `Dialog` 而非 `AlertDialog`：M3 1.2.1 的 `text` 槽没有内建滚动，`LazyColumn`
 *   直接放进去会撞上「infinite maximum height constraints」。
 * - E-Ink：不加阴影、不用灰底，用 1dp 黑框区分层次。
 */
@Composable
fun FeedWhitelistPickerDialog(
    initialCsv: String,
    feeds: List<Feed>?,
    onReload: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // 弹窗内的勾选是临时态：用 remember 即可（关掉弹窗就丢弃，符合「完成才生效」的语义）。
    // 不用 rememberSaveable —— Dialog 是独立组合/窗口，跨配置变更恢复收益很小，没必要冒风险。
    var csv by remember { mutableStateOf(initialCsv) }
    var query by remember { mutableStateOf("") }

    val options = remember(feeds) {
        feeds.orEmpty().map { FeedSelection.Option(it.id, it.title) }
    }
    val view = remember(csv, options, query) {
        FeedSelection.buildView(csv, options, query)
    }

    val eink = BuildConfig.IS_EINK
    val shownCount = view.available.size + view.missing.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (eink) Color.White else MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = if (eink) 0.dp else 6.dp,
            border = if (eink) BorderStroke(1.dp, Color.Black) else null
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_hidden_picker_title),
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.settings_hidden_picker_search_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(
                        R.string.settings_hidden_picker_selected_count,
                        view.selected.size,
                        shownCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { csv = FeedSelection.selectAll(csv, view.visibleIds) },
                        enabled = view.visibleIds.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.settings_hidden_picker_select_all))
                    }
                    TextButton(
                        onClick = { csv = FeedSelection.clearAll() },
                        enabled = view.selected.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.settings_hidden_picker_clear_all))
                    }
                }

                HorizontalDivider()

                // weight(fill = false)：给 LazyColumn 一个有界高度，条目少时弹窗不会被拉满。
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    when {
                        feeds == null -> PickerPlaceholder(
                            title = stringResource(R.string.settings_hidden_picker_loading),
                            hint = null,
                            showSpinner = true,
                            onReload = null
                        )

                        feeds.isEmpty() -> PickerPlaceholder(
                            title = stringResource(R.string.settings_hidden_picker_empty),
                            hint = stringResource(R.string.settings_hidden_picker_empty_hint),
                            showSpinner = false,
                            onReload = onReload
                        )

                        view.available.isEmpty() && view.missing.isEmpty() -> PickerPlaceholder(
                            title = stringResource(R.string.settings_hidden_picker_no_match, query),
                            hint = null,
                            showSpinner = false,
                            onReload = null
                        )

                        else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            if (view.missing.isNotEmpty()) {
                                item(key = "missing-header") {
                                    Text(
                                        text = stringResource(R.string.settings_hidden_picker_missing_header),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(view.missing, key = { "missing-$it" }) { id ->
                                    FeedPickerRow(
                                        id = id,
                                        title = stringResource(R.string.entry_list_untitled),
                                        checked = true,
                                        onToggle = { csv = FeedSelection.toggle(csv, id) }
                                    )
                                }
                                item(key = "missing-divider") { HorizontalDivider() }
                            }
                            items(view.available, key = { "feed-${it.id}" }) { option ->
                                FeedPickerRow(
                                    id = option.id,
                                    title = option.title,
                                    checked = option.id in view.selected,
                                    onToggle = { csv = FeedSelection.toggle(csv, option.id) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.feed_list_dialog_cancel))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { onConfirm(csv) }) {
                        Text(stringResource(R.string.settings_hidden_picker_done))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedPickerRow(
    id: Int,
    title: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val eink = BuildConfig.IS_EINK
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onToggle() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = if (eink) {
                CheckboxDefaults.colors(
                    checkedColor = Color.Black,
                    checkmarkColor = Color.White,
                    uncheckedColor = Color.DarkGray
                )
            } else {
                CheckboxDefaults.colors()
            }
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "#$id",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PickerPlaceholder(
    title: String,
    hint: String?,
    showSpinner: Boolean,
    onReload: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showSpinner) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        if (hint != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (onReload != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onReload) {
                Text(stringResource(R.string.settings_hidden_picker_reload))
            }
        }
    }
}
