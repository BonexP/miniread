package com.i.miniread.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.i.miniread.BuildConfig
import com.i.miniread.R

/**
 * 设置帮助页。纯说明，不改任何配置。
 *
 * 定位实现：`initialFirstVisibleItemIndex` 在首次组合就读取路由参数，所以不会「先渲染
 * 顶部再跳下去」；`LaunchedEffect` 里的 `scrollToItem` 只是兜底。
 *
 * ⚠️ 用 `scrollToItem` 而不是 `animateScrollToItem`：E-Ink 上动画滚动会拖影。
 *
 * @param sectionId 来自 `?section=`，对应 [HelpSection.id]；未知值则停在顶部
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHelpScreen(navController: NavController, sectionId: String) {
    // 第 0 项是页头 intro，所以小节序号要 +1
    val targetIndex = remember(sectionId) {
        HelpSection.fromId(sectionId)?.let { it.ordinal + 1 }
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = targetIndex ?: 0
    )
    LaunchedEffect(targetIndex) {
        if (targetIndex != null) listState.scrollToItem(targetIndex)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_help_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(key = "intro") {
                Text(
                    text = stringResource(R.string.settings_help_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(HelpSection.values(), key = { it.id }) { section ->
                HelpSectionBlock(section)
            }
        }
    }
}

@Composable
private fun HelpSectionBlock(section: HelpSection) {
    val eink = BuildConfig.IS_EINK
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(section.titleRes),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(section.bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp)
        )
        val exampleRes = section.exampleRes
        if (exampleRes != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (eink) Color.White else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (eink) Color.Black else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_help_example_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(exampleRes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
