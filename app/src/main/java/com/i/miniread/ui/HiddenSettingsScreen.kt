package com.i.miniread.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.i.miniread.R
import com.i.miniread.Screen
import com.i.miniread.util.FieldReport
import com.i.miniread.util.InactiveReason
import com.i.miniread.util.InterceptionClipboardFormat
import com.i.miniread.util.InterceptionDiagnosticsAnalyzer
import com.i.miniread.util.InterceptionExamples
import com.i.miniread.util.InterceptionRawConfig
import com.i.miniread.util.InterceptionRuntime
import com.i.miniread.util.InterceptionSettingsCodec
import com.i.miniread.viewmodel.MinifluxViewModel
import kotlinx.coroutines.launch

/**
 * 隐藏的高级设置页（主页顶栏标题连续点击 7 次进入，见 MainActivity）。
 *
 * 用途：配置「定向请求拦截」（总开关 / 目标域名 / feed 白名单 / Referer 规则）。
 *
 * ⚠️ 四个编辑状态必须是 `rememberSaveable`：点「?」跳帮助页会让本 Composable 离开组合，
 * 用普通 `remember` 会在返回时把用户刚改的内容全部丢掉（必现，不是概率问题）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenSettingsScreen(navController: NavController, viewModel: MinifluxViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 顶部状态展示走 StateFlow，只反映**已保存**的配置
    val baseUrl by InterceptionRuntime.baseUrl.collectAsState()
    val runtimeSettings by InterceptionRuntime.settings.collectAsState()

    // 本地草稿：初值取自当前生效配置快照
    val initial = remember { InterceptionRuntime.currentSettings() }
    var enabled by rememberSaveable { mutableStateOf(initial.enabled) }
    var domainsText by rememberSaveable {
        mutableStateOf(InterceptionSettingsCodec.formatDomains(initial.targetDomains))
    }
    var feedIdsText by rememberSaveable {
        mutableStateOf(InterceptionSettingsCodec.formatFeedIds(initial.feedIds))
    }
    var refererRulesText by rememberSaveable {
        mutableStateOf(InterceptionSettingsCodec.formatRefererRules(initial.refererRules))
    }

    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showFillConfirm by rememberSaveable { mutableStateOf(false) }

    // null = 尚未加载（fetchFeeds 失败会 postValue(emptyList())，不能与「已加载为空」混淆）
    val feedsOrNull by viewModel.feeds.observeAsState()

    val diag = remember(enabled, domainsText, feedIdsText, refererRulesText, baseUrl) {
        InterceptionDiagnosticsAnalyzer.analyze(
            InterceptionRawConfig(
                enabled = enabled,
                domains = domainsText,
                feedIds = feedIdsText,
                refererRules = refererRulesText
            ),
            baseUrl
        )
    }

    LaunchedEffect(showPicker) {
        if (showPicker && feedsOrNull == null) viewModel.fetchFeeds()
    }

    // 需要提前取出的文案（stringResource 只能在 Composable 作用域调用）
    val savedMsg = stringResource(R.string.settings_hidden_saved)
    val resetMsg = stringResource(R.string.settings_hidden_reset_done)
    val copiedMsg = stringResource(R.string.settings_hidden_copied)
    val importedMsg = stringResource(R.string.settings_hidden_imported)
    val importFailedMsg = stringResource(R.string.settings_hidden_import_failed)
    val saveFailedMsg = stringResource(R.string.settings_hidden_save_failed)
    val fillDoneMsg = stringResource(R.string.settings_hidden_fill_example_done)
    val noServerMsg = stringResource(R.string.settings_hidden_fill_example_no_server)

    val openHelp: (HelpSection) -> Unit = { section ->
        navController.navigate(Screen.SettingsHelp.route + "?section=" + section.id)
    }

    // 示例全部从**当前服务器**派生，代码里不留任何私有实例地址。
    // 已有的订阅源编号原样保留；没填过才退化为「前 3 个已加载订阅源」。
    val applyExample: () -> Unit = {
        val example = InterceptionExamples.build(
            baseUrl = baseUrl,
            currentFeedIds = feedIdsText,
            availableFeedIds = feedsOrNull.orEmpty().map { it.id }
        )
        if (example.domains.isBlank()) {
            Toast.makeText(context, noServerMsg, Toast.LENGTH_SHORT).show()
        } else {
            enabled = example.enabled
            domainsText = example.domains
            feedIdsText = example.feedIds
            refererRulesText = example.refererRules
            Toast.makeText(context, fillDoneMsg, Toast.LENGTH_SHORT).show()
        }
    }

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
                },
                actions = {
                    IconButton(onClick = { openHelp(HelpSection.Overview) }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.settings_help_title),
                            modifier = Modifier.size(20.dp)
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
            StatusBlock(
                baseUrl = baseUrl,
                savedActive = runtimeSettings.isActive,
                draftActive = diag.isActive,
                reasons = diag.inactiveReasons,
                baseUrlMatches = diag.baseUrlMatches
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
            SettingsFieldHeader(
                titleRes = R.string.settings_hidden_domains,
                onHelp = { openHelp(HelpSection.Domains) }
            )
            OutlinedTextField(
                value = domainsText,
                onValueChange = { domainsText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.settings_hidden_domains_placeholder)) },
                supportingText = {
                    FieldSupportingText(diag.domains, R.string.settings_hidden_domains_hint)
                },
                isError = diag.domains.hasError,
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. feed 白名单
            SettingsFieldHeader(
                titleRes = R.string.settings_hidden_feed_ids,
                onHelp = { openHelp(HelpSection.FeedIds) }
            )
            OutlinedTextField(
                value = feedIdsText,
                onValueChange = { feedIdsText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.settings_hidden_feed_ids_placeholder)) },
                supportingText = {
                    FieldSupportingText(diag.feedIds, R.string.settings_hidden_feed_ids_manual)
                },
                isError = diag.feedIds.hasError,
                minLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_hidden_picker_open))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Referer 规则
            SettingsFieldHeader(
                titleRes = R.string.settings_hidden_referer_rules,
                onHelp = { openHelp(HelpSection.Referer) }
            )
            OutlinedTextField(
                value = refererRulesText,
                onValueChange = { refererRulesText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.settings_hidden_referer_placeholder)) },
                supportingText = {
                    FieldSupportingText(diag.rules, R.string.settings_hidden_referer_rules_hint)
                },
                isError = diag.rules.hasError,
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5. 保存
            Button(
                onClick = {
                    scope.launch {
                        val raw = InterceptionRawConfig(
                            enabled = enabled,
                            domains = domainsText,
                            feedIds = feedIdsText,
                            refererRules = refererRulesText
                        )
                        try {
                            InterceptionRuntime.save(InterceptionSettingsCodec.parse(raw))
                        } catch (e: Exception) {
                            // DataStore 写盘失败（磁盘满 / 权限异常）时不要静默崩在协程里
                            Log.e("HiddenSettings", "save failed", e)
                            Toast.makeText(context, saveFailedMsg, Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val message = if (diag.droppedTotal > 0) {
                            context.getString(
                                R.string.settings_hidden_saved_with_ignored,
                                diag.droppedTotal
                            )
                        } else {
                            savedMsg
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
                        try {
                            InterceptionRuntime.reset()
                        } catch (e: Exception) {
                            Log.e("HiddenSettings", "reset failed", e)
                            Toast.makeText(context, saveFailedMsg, Toast.LENGTH_SHORT).show()
                            return@launch
                        }
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
                    val text = InterceptionClipboardFormat.buildExportText(
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

                    val imported = InterceptionClipboardFormat.parseImported(text)
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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val hasInput = enabled ||
                        domainsText.isNotBlank() ||
                        feedIdsText.isNotBlank() ||
                        refererRulesText.isNotBlank()
                    if (hasInput) {
                        showFillConfirm = true
                    } else {
                        applyExample()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_hidden_fill_example))
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { openHelp(HelpSection.Overview) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_help_title))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showPicker) {
        FeedWhitelistPickerDialog(
            initialCsv = feedIdsText,
            feeds = feedsOrNull,
            onReload = { viewModel.fetchFeeds() },
            onDismiss = { showPicker = false },
            onConfirm = { csv ->
                feedIdsText = csv
                showPicker = false
            }
        )
    }

    if (showFillConfirm) {
        AlertDialog(
            onDismissRequest = { showFillConfirm = false },
            title = { Text(stringResource(R.string.settings_hidden_fill_example_confirm_title)) },
            text = { Text(stringResource(R.string.settings_hidden_fill_example_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFillConfirm = false
                        applyExample()
                    }
                ) {
                    Text(stringResource(R.string.feed_list_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFillConfirm = false }) {
                    Text(stringResource(R.string.feed_list_dialog_cancel))
                }
            }
        )
    }
}

/** 顶部状态：已保存的开关状态 + 草稿为何还不会生效。 */
@Composable
private fun StatusBlock(
    baseUrl: String,
    savedActive: Boolean,
    draftActive: Boolean,
    reasons: List<InactiveReason>,
    baseUrlMatches: Boolean?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_hidden_current_base_url, baseUrl),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = stringResource(
                if (savedActive) R.string.settings_hidden_active else R.string.settings_hidden_inactive
            ),
            style = MaterialTheme.typography.bodyMedium
        )

        if (!draftActive) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_hidden_inactive_reasons_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.settings_hidden_draft_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            reasons.forEach { reason ->
                Text(
                    text = "· " + stringResource(reason.messageRes()),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            val showBaseUrlHint = baseUrlMatches == false &&
                reasons.none { it == InactiveReason.NoDomains }
            if (showBaseUrlHint) {
                Text(
                    text = "· " + stringResource(R.string.settings_hidden_reason_base_url),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** 字段标题 + 「?」，点开直达帮助页对应小节。 */
@Composable
private fun SettingsFieldHeader(@StringRes titleRes: Int, onHelp: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f)
        )
        // 保持 IconButton 默认 48dp 触控区（E-Ink 红线），只缩小内部图标
        IconButton(onClick = onHelp) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(
                    R.string.settings_help_open_for,
                    stringResource(titleRes)
                ),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 字段下方的提示：优先说「会被丢弃什么」，其次说「注意什么」，都没有才显示静态 hint。
 * 文案与 [InterceptionDiagnosticsAnalyzer] 的结论一一对应，不要在 UI 里另算。
 */
@Composable
private fun FieldSupportingText(report: FieldReport, @StringRes hintRes: Int) {
    val error = report.errors.firstOrNull()
    val warning = report.warnings.firstOrNull()
    when {
        error != null -> Column {
            Text(
                stringResource(
                    R.string.settings_hidden_field_summary,
                    report.validCount,
                    report.droppedCount
                )
            )
            Text("${error.raw.take(24)} · ${stringResource(error.code.messageRes())}")
        }

        warning != null -> Column {
            Text(
                stringResource(
                    R.string.settings_hidden_field_warning_prefix,
                    stringResource(warning.code.messageRes())
                )
            )
            Text(warning.raw.take(24))
        }

        else -> Text(stringResource(hintRes))
    }
}
