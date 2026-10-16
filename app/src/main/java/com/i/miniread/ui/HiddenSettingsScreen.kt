package com.i.miniread.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.i.miniread.R
import com.i.miniread.util.InterceptionRawConfig
import com.i.miniread.util.InterceptionRuntime
import com.i.miniread.util.InterceptionSettingsCodec
import kotlinx.coroutines.launch

/** 从剪贴板导入时解析出的 4 个字段（未解析） */
private data class ImportedConfig(
    val enabled: Boolean,
    val domains: String,
    val feedIds: String,
    val refererRules: String
)

/** 组装导出到剪贴板的文本：4 行 key=value，方便用户手改与备份。 */
private fun buildExportText(
    enabled: Boolean,
    domains: String,
    feedIds: String,
    refererRules: String
): String = buildString {
    append("enabled=").append(enabled).append('\n')
    append("domains=").append(domains).append('\n')
    append("feedIds=").append(feedIds).append('\n')
    append("refererRules=").append(refererRules)
}

/** 解析剪贴板文本；无法识别任何已知 key 时返回 null（视为解析失败）。 */
private fun parseImported(text: String): ImportedConfig? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null

    var enabled = "false"
    var domains = ""
    var feedIds = ""
    var refererRules = ""
    var matched = false

    trimmed.split('\n').forEach { rawLine ->
        val line = rawLine.trim()
        val idx = line.indexOf('=')
        if (idx <= 0) return@forEach
        val key = line.substring(0, idx).trim()
        val value = line.substring(idx + 1).trim()
        when (key) {
            "enabled" -> { enabled = value; matched = true }
            "domains" -> { domains = value; matched = true }
            "feedIds" -> { feedIds = value; matched = true }
            "refererRules" -> { refererRules = value; matched = true }
        }
    }

    if (!matched) return null
    return ImportedConfig(
        enabled = enabled.equals("true", ignoreCase = true),
        domains = domains,
        feedIds = feedIds,
        refererRules = refererRules
    )
}

/**
 * 隐藏的高级设置页。
 *
 * 入口：在主页顶栏标题连续点击 7 次（见 MainActivity）。
 * 用途：配置"定向请求拦截"（总开关 / 目标域名 / feed 白名单 / Referer 规则）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 顶部状态展示走 StateFlow，配置变更后自动刷新
    val baseUrl by InterceptionRuntime.baseUrl.collectAsState()
    val runtimeSettings by InterceptionRuntime.settings.collectAsState()

    // 本地编辑状态：初值取自当前生效配置快照
    val initial = remember { InterceptionRuntime.currentSettings() }
    var enabled by remember { mutableStateOf(initial.enabled) }
    var domainsText by remember {
        mutableStateOf(InterceptionSettingsCodec.formatDomains(initial.targetDomains))
    }
    var feedIdsText by remember {
        mutableStateOf(InterceptionSettingsCodec.formatFeedIds(initial.feedIds))
    }
    var refererRulesText by remember {
        mutableStateOf(InterceptionSettingsCodec.formatRefererRules(initial.refererRules))
    }

    // 需要提前取出的文案（stringResource 只能在 Composable 作用域调用）
    val savedMsg = stringResource(R.string.settings_hidden_saved)
    val resetMsg = stringResource(R.string.settings_hidden_reset_done)
    val copiedMsg = stringResource(R.string.settings_hidden_copied)
    val importedMsg = stringResource(R.string.settings_hidden_imported)
    val importFailedMsg = stringResource(R.string.settings_hidden_import_failed)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_hidden_title)) },
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 顶部：当前 baseUrl 与是否生效
            Text(
                text = stringResource(R.string.settings_hidden_current_base_url, baseUrl),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    if (runtimeSettings.isActive) R.string.settings_hidden_active
                    else R.string.settings_hidden_inactive
                ),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. 总开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_hidden_enable),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_hidden_enable_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 目标域名
            OutlinedTextField(
                value = domainsText,
                onValueChange = { domainsText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_hidden_domains)) },
                supportingText = { Text(stringResource(R.string.settings_hidden_domains_hint)) },
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. feed 白名单
            OutlinedTextField(
                value = feedIdsText,
                onValueChange = { feedIdsText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_hidden_feed_ids)) },
                supportingText = { Text(stringResource(R.string.settings_hidden_feed_ids_hint)) },
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Referer 规则
            Text(
                text = stringResource(R.string.settings_hidden_referer_rules),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = refererRulesText,
                onValueChange = { refererRulesText = it },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text(stringResource(R.string.settings_hidden_referer_rules_hint)) },
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5. 按钮行
            Button(
                onClick = {
                    scope.launch {
                        val raw = InterceptionRawConfig(
                            enabled = enabled,
                            domains = domainsText,
                            feedIds = feedIdsText,
                            refererRules = refererRulesText
                        )
                        InterceptionRuntime.save(InterceptionSettingsCodec.parse(raw))
                        Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_hidden_save))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    scope.launch {
                        InterceptionRuntime.reset()
                        // 同步重置本地编辑状态
                        enabled = false
                        domainsText = ""
                        feedIdsText = ""
                        refererRulesText = ""
                        Toast.makeText(context, resetMsg, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_hidden_reset))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val text = buildExportText(
                        enabled = enabled,
                        domains = domainsText,
                        feedIds = feedIdsText,
                        refererRules = refererRulesText
                    )
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("MiniRead", text))
                    Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_hidden_export))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val text = clipboard.primaryClip
                        ?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)
                        ?.coerceToText(context)
                        ?.toString()
                        .orEmpty()

                    val imported = parseImported(text)
                    if (imported == null) {
                        Toast.makeText(context, importFailedMsg, Toast.LENGTH_SHORT).show()
                    } else {
                        enabled = imported.enabled
                        domainsText = imported.domains
                        feedIdsText = imported.feedIds
                        refererRulesText = imported.refererRules
                        Toast.makeText(context, importedMsg, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_hidden_import))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
