package com.i.miniread.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.i.miniread.MainActivity
import com.i.miniread.R
import com.i.miniread.util.ArticleHtml
import com.i.miniread.util.DomainHelper
import com.i.miniread.util.InterceptionConfig
import com.i.miniread.util.InterceptionRuntime
import com.i.miniread.util.ReaderThemeResolver
import com.i.miniread.util.ReadingPositionResolver
import com.i.miniread.util.ScrollSnapshot
import com.i.miniread.viewmodel.MinifluxViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import okhttp3.Response
import java.io.File
import java.util.concurrent.TimeUnit

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ArticleDetailScreen(viewModel: MinifluxViewModel, entryId: Int, navController: NavController) {
    Log.d("ArticleDetailScreen", "ArticleDetailScreen: now get entryId is $entryId")
    val selectedEntry by viewModel.selectedEntry.observeAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val markedReadMsg = stringResource(R.string.article_snackbar_marked_read)

    LaunchedEffect(entryId, selectedEntry?.id) {
        if (selectedEntry?.id != entryId) {
            viewModel.loadEntryById(entryId)
        }
    }

    Scaffold(
        bottomBar = { ArticleActionsBar(viewModel, entryId, snackbarHostState,navController) },
        snackbarHost = { MinireadSnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(10.dp)
        ) {
            if (selectedEntry == null || selectedEntry?.id != entryId) {
                Text(
                    text = stringResource(R.string.article_loading),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleLarge
                )
            } else {
                selectedEntry?.let { entry ->
                    // Gson 可能把声明为非空的 feed 落成 null（见 GsonParsingTest），故用安全调用兜底。
                    @Suppress("UNNECESSARY_SAFE_CALL")
                    val feedTitle: String? = entry.feed?.title

                    // TODO(reader-header): Compose 顶栏信息栏（订阅源 / 发布时间）本轮不做，
                    //   元信息只在 WebView 正文内注入。后续若做：在 Scaffold 加 topBar，复用
                    //   ArticleHtml.formatPublishedAt 避免两套时间口径；E-Ink 下顶栏需与 48dp 底栏同风格
                    //   （纯白、tonalElevation = 0.dp）。

                    // 大字符串拼接用 remember 缓存：仅当元信息/正文变化时重建，避免每次重组重复拼接整篇文章。
                    // ⚠️ key 必须含 feedTitle 与 published_at：只 key title+content 会让 meta 永不重建。
                    // ⚠️ 不要加 starred 或 entry 对象：收藏 toggle 会重载 entry，进 key 就重载页面、丢阅读位置。
                    // ⚠️ 标题与订阅源名是**文本**（转义），正文是 Miniflux 返回的 HTML（原样透传）。
                    val webViewHtml = remember(
                        entry.title,
                        entry.content,
                        feedTitle,
                        entry.published_at
                    ) {
                        ArticleHtml.buildContent(
                            title = entry.title,
                            feedTitle = feedTitle,
                            publishedAt = entry.published_at,
                            content = entry.content
                        )
                    }
                    ArticleWebView(
                        content = webViewHtml,
                        feedId = entry.feed_id,
                        onScrollToBottom = {
                            Log.d("ArticleDetailScreen", "Article scrolled to end!")
                            viewModel.markEntryAsRead(entryId)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(markedReadMsg)
                            }
                        },
                        // 阅读进度：旋转 / resize / WebView 重建后恢复（见 AGENTS.md §8 已知坑 13）
                        onRequestSavedPosition = { viewModel.getReadingPosition(entryId) },
                        onPositionChanged = { viewModel.saveReadingPosition(entryId, it) }
                    )
                }
            }
        }
    }
}

@Composable
fun ArticleActionsBar(
    viewModel: MinifluxViewModel,
    entryId: Int,
    snackbarHostState: SnackbarHostState,
    navController: NavController
) {
    val coroutineScope = rememberCoroutineScope()
    val markedReadMsg = stringResource(R.string.article_snackbar_marked_read)
    val markedUnreadMsg = stringResource(R.string.article_snackbar_marked_unread)
    val firstEntryMsg = stringResource(R.string.article_snackbar_first_entry)
    val lastEntryMsg = stringResource(R.string.article_snackbar_last_entry)
    val bookmarkedMsg = stringResource(R.string.article_snackbar_bookmarked)
    val unbookmarkedMsg = stringResource(R.string.article_snackbar_unbookmarked)
    val selectedEntry by viewModel.selectedEntry.observeAsState()
    val context = LocalContext.current
    val isEink = com.i.miniread.BuildConfig.IS_EINK
    val barModifier = if (isEink) {
        Modifier
            .height(48.dp)
            .drawBehind {
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx()
                )
            }
    } else {
        Modifier
    }

    // ===== 动作回调与无障碍文案 =====
    // 抽成局部变量：E-Ink（7 键图标）与 standard（6 键 + 开关）两条布局共用同一套行为，
    // 避免改一处忘另一处。行为本身与改动前逐字一致。
    val markReadDesc = stringResource(R.string.article_action_mark_read)
    val markUnreadDesc = stringResource(R.string.article_action_mark_unread)
    val shareDesc = stringResource(R.string.article_action_share)
    val openExternalDesc = stringResource(R.string.article_action_open_external)
    val previousDesc = stringResource(R.string.article_action_previous)
    val nextDesc = stringResource(R.string.article_action_next)
    val addBookmarkDesc = stringResource(R.string.article_action_add_bookmark)
    val removeBookmarkDesc = stringResource(R.string.article_action_remove_bookmark)

    val onMarkRead: () -> Unit = {
        Log.d("ArticleDetailScreen", "Mark Entry as Read")
        viewModel.markEntryAsRead(entryId)
        coroutineScope.launch { snackbarHostState.showSnackbar(markedReadMsg) }
    }
    val onMarkUnread: () -> Unit = {
        viewModel.markEntryAsUnread(entryId)
        coroutineScope.launch { snackbarHostState.showSnackbar(markedUnreadMsg) }
    }
    val onShare: () -> Unit = {
        Log.d("ArticleDetailScreen", "Share Entry")
        selectedEntry?.let {
            val shareIntent = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, it.title)
                    putExtra(Intent.EXTRA_TEXT, "${it.title}\n${it.url}")
                },
                null
            )
            context.startActivity(shareIntent)
        }
    }
    val onOpenExternal: () -> Unit = {
        Log.d("ArticleDetailScreen", "Open Entry External")
        selectedEntry?.let {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.url)))
        }
    }
    // 上一篇 / 下一篇：用查询参数格式导航，popUpTo 自身保证栈不增长
    val onPrevious: () -> Unit = {
        viewModel.navigateToPreviousEntry(entryId)?.let { prevId ->
            navController.navigate("articleDetail?entryId=$prevId") {
                popUpTo("articleDetail?entryId=$prevId") { inclusive = true }
                launchSingleTop = true
            }
        } ?: run {
            coroutineScope.launch { snackbarHostState.showSnackbar(firstEntryMsg) }
        }
    }
    val onNext: () -> Unit = {
        viewModel.navigateToNextEntry(entryId)?.let { nextId ->
            navController.navigate("articleDetail?entryId=$nextId") {
                popUpTo("articleDetail?entryId=$nextId") { inclusive = true }
                launchSingleTop = true
            }
        } ?: run {
            coroutineScope.launch { snackbarHostState.showSnackbar(lastEntryMsg) }
        }
    }
    val onToggleStar: (Int, Boolean) -> Unit = { id, checked ->
        viewModel.toggleStarred(id, checked)
        coroutineScope.launch {
            snackbarHostState.showSnackbar(if (checked) bookmarkedMsg else unbookmarkedMsg)
        }
    }

    BottomAppBar(
        modifier = barModifier,
        // E-Ink 版本使用纯白背景并去掉 tonalElevation，避免灰底与残影
        containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isEink) 0.dp else BottomAppBarDefaults.ContainerElevation,
        // E-Ink 的 7 个按钮用 weight 均分，横向内边距全部让给按钮；
        // standard 保留 8dp（行为与改动前一致）
        contentPadding = PaddingValues(horizontal = if (isEink) 0.dp else 8.dp)
    ) {
        if (isEink) {
            // ===== E-Ink：7 键图标化，布局对齐 v0.3.3（issue #11） =====
            // 顺序：上一篇 → 下一篇 → 已读 → 未读 → 收藏 → 分享 → 外部打开。
            // 不用 Switch：墨水屏上开关状态不易辨认，改用实心/空心星 + 统一纯黑 tint。
            // ⚠️ weight(1f) 是必需的：360dp 宽 ÷ 7 ≈ 51dp 恰好达标 48dp 最小触控尺寸，
            //    换成固定 48dp + SpaceBetween 会在 320dp 设备上溢出被裁。
            val slot = Modifier.weight(1f).fillMaxHeight()
            EInkActionButton(Icons.AutoMirrored.Filled.ArrowBack, previousDesc, onPrevious, slot)
            EInkActionButton(Icons.AutoMirrored.Filled.ArrowForward, nextDesc, onNext, slot)
            EInkActionButton(Icons.Default.CheckCircle, markReadDesc, onMarkRead, slot)
            EInkActionButton(Icons.Outlined.CheckCircle, markUnreadDesc, onMarkUnread, slot)
            // 收藏：用 selectedEntry 判空而不是 `?: Spacer(slot)`——后者靠 let 返回 Unit
            // 落进 Elvis 的右侧分支，能跑但语义隐晦，评审时容易被误读成「星标按钮可能不显示」。
            val starredEntry = selectedEntry
            if (starredEntry != null) {
                IconToggleButton(
                    checked = starredEntry.starred,
                    onCheckedChange = { onToggleStar(starredEntry.id, it) },
                    modifier = slot
                ) {
                    Icon(
                        // 实心 = 已收藏，空心 = 未收藏：墨水屏没有彩色语义，只能靠形状区分。
                        // ⚠️ 不能用 Icons.Outlined.Star：material-icons-core 里它与 Filled.Star 是
                        //    同一份 path（单闭合轮廓、无内孔），画出来仍是实心；空心星走 extended 包的 StarBorder。
                        imageVector = if (starredEntry.starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (starredEntry.starred) removeBookmarkDesc else addBookmarkDesc,
                        tint = Color.Black
                    )
                }
            } else {
                // entry 未到达时占位，避免 7 格在加载完成后整体跳动
                Spacer(slot)
            }
            EInkActionButton(Icons.Default.Share, shareDesc, onShare, slot)
            EInkActionButton(Icons.AutoMirrored.Filled.ExitToApp, openExternalDesc, onOpenExternal, slot)
        } else {
            // ===== standard：保持改动前的 6 键 + 弹性占位 + 收藏开关 =====
            ActionButton(icon = Icons.Default.CheckCircle, description = markReadDesc, onClick = onMarkRead)
            ActionButton(icon = Icons.Outlined.CheckCircle, description = markUnreadDesc, onClick = onMarkUnread)
            ActionButton(icon = Icons.Default.Share, description = shareDesc, onClick = onShare)
            ActionButton(icon = Icons.Default.ExitToApp, description = openExternalDesc, onClick = onOpenExternal)
            ActionButton(icon = Icons.Default.ArrowBack, description = previousDesc, onClick = onPrevious)
            ActionButton(icon = Icons.Default.ArrowForward, description = nextDesc, onClick = onNext)

//todo 刷新视图
//            ActionButton(icon=Icons.Default.Refresh, description = "refresh view"){
//                Log.d("ArticleDetailScreen", "refresh webview")
//            }
            Spacer(modifier = Modifier.weight(1f))
            selectedEntry?.let { entry ->
                //收藏按钮
                Switch(
                    checked = entry.starred,
                    onCheckedChange = { onToggleStar(entry.id, it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

/** standard 分支的普通图标按钮（沿用 IconButton 默认 48dp 触控区）。 */
@Composable
fun ActionButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = description)
    }
}

/**
 * E-Ink 分支的图标按钮：统一纯黑 tint，尺寸由外层 `Modifier.weight(1f).fillMaxHeight()` 决定。
 *
 * ⚠️ 不要再把尺寸压到 40dp（旧实现的 `compact` 分支）：那低于 Material3 的 48dp 最小交互尺寸，
 * 是 issue #11 里「失去墨水屏优化」的可访问性代价之一。高度由 48dp 底栏撑满即可。
 */
@Composable
private fun EInkActionButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(imageVector = icon, contentDescription = description, tint = Color.Black)
    }
}

private const val isOutputHtmlContent = false

// 拦截请求共用单例：复用连接池并设置超时（原实现每次 new OkHttpClient()，无连接池、无超时）
private val interceptionHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ArticleWebView(
    content: String?,
    feedId: Int?,
    onScrollToBottom: () -> Unit,
    onRequestSavedPosition: () -> ScrollSnapshot? = { null },
    onPositionChanged: (ScrollSnapshot) -> Unit = {}
) {
    val context = LocalContext.current
    // 读内存快照判定：不再在组合期 runBlocking 读盘；隐藏设置页改完配置后重新进入文章页即生效
    val interceptionSettings by InterceptionRuntime.settings.collectAsState()
    val currentBaseUrl by InterceptionRuntime.baseUrl.collectAsState()
    val shouldInterceptRequests = remember(interceptionSettings, currentBaseUrl, feedId) {
        DomainHelper.isTargetDomain(interceptionSettings, currentBaseUrl) &&
            feedId != null && feedId in interceptionSettings.feedIds
    }
    val coroutineScope = rememberCoroutineScope()
    val hasMarkedAsRead = remember { mutableStateOf(false) }
    var scrollDebounceJob by remember { mutableStateOf<Job?>(null) }
    val updatedOnScrollToBottom by rememberUpdatedState(onScrollToBottom)

    // ===== 阅读位置：写入与恢复所需状态 =====
    // 用 rememberUpdatedState 包装，避免下方 remember{} 内创建的 webViewClient 闭包捕获过期 lambda
    val currentRequestSavedPosition by rememberUpdatedState(onRequestSavedPosition)
    val updatedOnPositionChanged by rememberUpdatedState(onPositionChanged)
    // 每次 onPageFinished 自增；> 0 表示页面至少完成过一次加载（恢复的前置条件）
    var pageLoadTick by remember { mutableIntStateOf(0) }
    // 上次已处理的 pageLoadTick：区分「真·页面加载事件」与「纯布局变化（旋转/分屏）」
    var lastHandledPageLoadTick by remember { mutableIntStateOf(-1) }
    // WebView 是否已被 onRelease 销毁：防止 postDelayed 回调对已销毁的 WebView 调用 JS
    var released by remember { mutableStateOf(false) }
    // 程序化滚动后的抑制时间窗（SystemClock.uptimeMillis 基准）：防止恢复被误判为「滚到底」
    var scrollGuardUntil by remember { mutableLongStateOf(0L) }
    // 滚动写入节流状态
    var lastSavedScrollY by remember { mutableIntStateOf(0) }
    var lastSavedAtMs by remember { mutableLongStateOf(0L) }

    /**
     * 记录当前滚动位置（节流）。
     * 只触碰上面这些 remember 出来的稳定对象，因此被 remember{} 内 webViewClient 的旧闭包
     * 调用也安全（读写拿到的都是当前值）。
     *
     * @param force 强制补记（onPageFinished / onDispose）。强制路径**不允许用 0 覆盖**已保存的
     * 位置：页面刚加载完、或 WebView 因旋转重建时 scrollY 本就是 0；用户真正「手动滚回顶部」
     * 已由滚动监听（非 force）写入。
     */
    fun recordScrollPosition(view: WebView, force: Boolean = false) {
        val snapshot = measureScrollSnapshot(view)
        if (snapshot.contentHeight <= 0 || snapshot.viewportHeight <= 0) return
        if (force && snapshot.scrollY <= 0) return

        val nowMs = SystemClock.uptimeMillis()
        if (!force) {
            if (snapshot.scrollY == lastSavedScrollY) return
            val deltaPx = kotlin.math.abs(snapshot.scrollY - lastSavedScrollY)
            if (deltaPx < SCROLL_SAVE_MIN_DELTA_PX &&
                nowMs - lastSavedAtMs < SCROLL_SAVE_MAX_INTERVAL_MS
            ) return
        }

        lastSavedScrollY = snapshot.scrollY
        lastSavedAtMs = nowMs
        updatedOnPositionChanged(snapshot)
    }

    val webView = remember {
        WebView(context).apply {
            // ===== E-Ink 优化：强化焦点管理 =====
            // ⚠️ 注意：E-Ink 版本需要 WebView 获得焦点以支持音量键翻页
            // 原因：MainActivity 的 onKeyDown 方法依赖 currentFocus 获取 WebView
            if (com.i.miniread.BuildConfig.IS_EINK) {
                // 初始化后立即请求焦点
                post {
                    requestFocus()
                    Log.d("WebViewFocus", "Initial focus request: hasFocus=${hasFocus()}")
                }
                // 延迟 500ms 再次请求焦点，确保焦点获取
                postDelayed({
                    requestFocus()
                    Log.d("WebViewFocus", "Delayed focus request (500ms): hasFocus=${hasFocus()}")
                }, 500)
            }
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                loadsImagesAutomatically = true
                textZoom = 125
                
                // ===== E-Ink 优化：隐藏滚动条 =====
                // ⚠️ 原因：墨水屏刷新滚动条会产生残影和闪烁
                // 在 E-Ink 设备上禁用垂直滚动条以避免刷新问题
                if (com.i.miniread.BuildConfig.IS_EINK) {
                    isVerticalScrollBarEnabled = false
                }
                
                // ===== E-Ink 优化：触摸焦点 =====
                // 改善触摸交互体验，确保触摸时能获得焦点
                if (com.i.miniread.BuildConfig.IS_EINK) {
                    isFocusable = true
                    isFocusableInTouchMode = true
                }
            }
            
            setBackgroundColor(0x00000000)
            
            // E-Ink 版本额外的焦点请求
            if (com.i.miniread.BuildConfig.IS_EINK) {
                requestFocusFromTouch()
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url.toString()
                    // 检查是否是外部链接，如果是则使用外部浏览器打开
                    if (url.startsWith("http") || url.startsWith("https")) {
                        val intent = Intent(Intent.ACTION_VIEW, request?.url)
                        context.startActivity(intent)
                        return true // 返回 true 表示我们处理了该链接
                    }
                    return false
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    return if (shouldInterceptRequests && request != null) {
                        interceptWebRequest(request, interceptionSettings.refererRules)
                    } else {
                        super.shouldInterceptRequest(view, request)
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d("ArticleWebView", "Page finished loading")
                    pageLoadTick += 1 // 驱动阅读位置恢复 Effect（页面加载完成的信号）

                    // 延迟执行，确保资源加载完毕
                    view?.postDelayed({
                        // WebView 可能已被 onRelease 销毁（快速离开页面）→ 回调里先防御
                        if (released) return@postDelayed
                        view.evaluateJavascript(
                            "(function() { return document.body.scrollHeight; })();"
                        ) { result ->
                            val contentHeight = result?.toFloatOrNull()
                            if (contentHeight != null) {
                                val webViewHeight = view.height.toFloat()

                                if (contentHeight <= webViewHeight) {
                                    if (!hasMarkedAsRead.value) {
                                        Log.d(
                                            "ArticleWebView",
                                            "Content does not fill the screen, marking as read."
                                        )
                                        updatedOnScrollToBottom()
                                        hasMarkedAsRead.value = true
                                    }
                                }
                            }
                            // 页面稳定后回写一次真实位置（force：不会用 0 覆盖已保存位置）
                            recordScrollPosition(view, force = true)
                        }
                    }, 500) // 延迟 500ms 执行

                    view?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                        // ⚠️ 必须先记录位置、再判抑制窗口：恢复后的真实落点要立刻回写
                        recordScrollPosition(view)
                        // 程序化恢复滚动期间不判定「滚到底」（避免重复标记已读 + 弹 Snackbar）
                        if (SystemClock.uptimeMillis() < scrollGuardUntil) {
                            return@setOnScrollChangeListener
                        }
                        if (hasMarkedAsRead.value) return@setOnScrollChangeListener

                        val remainingScroll =
                            view.contentHeight * view.scale - (scrollY + view.height)
                        if (scrollY > oldScrollY && remainingScroll < 50) {
                            scrollDebounceJob?.cancel()
                            scrollDebounceJob = coroutineScope.launch {
                                delay(500)
                                if (remainingScroll < 50) {
                                    Log.d(
                                        "ArticleWebView",
                                        "Debounced bottom reached, marking as read."
                                    )
                                    updatedOnScrollToBottom()
                                    hasMarkedAsRead.value = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    var htmlContent by remember { mutableStateOf<String?>(null) }

    // 异步加载HTML内容
    LaunchedEffect(content) {
        content?.let {
            loadHtmlContentAsync(context, it) { loadedHtml ->
                htmlContent = loadedHtml
            }
        }
    }

    // 将 WebView 登记到 MainActivity，供 E-Ink 音量键翻页使用；离开页面时清理，避免泄漏
    DisposableEffect(webView) {
        (context as? MainActivity)?.setActiveWebView(webView)
        onDispose {
            // 只做解登记，不碰 view：落盘与销毁统一由 AndroidView.onRelease 负责，
            // 两者执行顺序无官方保证，解耦后无论谁先谁后都安全。
            (context as? MainActivity)?.setActiveWebView(null)
        }
    }

    // HTML 内容就绪后加载，且仅在 htmlContent 变化时执行一次：
    // 收藏 toggle / 标记已读等引发的重组不会重载页面，阅读进度得以保留。
    // ⚠️ 不要把 loadDataWithBaseURL 放在组合体主体里——那样每次重组都会重载页面。
    LaunchedEffect(htmlContent) {
        htmlContent?.let {
            webView.loadDataWithBaseURL(null, it, "text/html", "UTF-8", null)
        }
    }

    // ===== 恢复阅读位置 =====
    // key 只有两个：
    //   · pageLoadTick —— 真·页面加载完成（首次进入 / 从列表重进 / 进程恢复 / WebView 被重建）
    //   · sizeSignature —— 窗口尺寸变化（旋转 / 分屏；页面通常**没有**重新加载）
    // 两类事件的处理不同：加载事件直接走比例恢复兜底；纯布局变化先用
    // ReadingPositionResolver.needsRestoreAfterLayoutChange 判断原生落点是否已正确，
    // 正确则绝不 scrollTo（WebView 原生保留的位置比比例换算更准）。
    // ⚠️ 绝不能把 content / savedPosition / entryId 加入 key：
    //    收藏 toggle 等「仅重组、不重载」的场景会误触发恢复，造成位置跳动。
    val configuration = LocalConfiguration.current
    val sizeSignature = configuration.screenWidthDp to configuration.screenHeightDp

    // ===== 主题样式表随深色模式切换刷新 =====
    // uiMode 已在 Manifest 的 configChanges 中声明 → 切换深浅色不会重建 Activity、页面也不会重载，
    // 页面里内联的主题 CSS 仍是旧主题（且 loadHtmlContentAsync 的主题缓存按明/暗分别保存）。
    // 处理：只就地替换 <style id="miniread-theme-css"> 的 textContent，**不**走 loadDataWithBaseURL，
    // 因此阅读位置、滚动状态都不受影响（正文内容与布局尺寸均未变）。
    // ⚠️ key 用 pageLoadTick（新页面按当前主题构建，需覆盖一次）与 isDarkTheme（切换时刷新）。
    //    key 取**解析后的布尔值**而不是 uiModeNight：E-Ink 恒浅色，系统深浅色变化不改变 isDarkTheme，
    //    因此不会触发一次无意义的整页 CSS 重设（墨水屏上那是可见的闪烁）。
    val isDarkTheme = ReaderThemeResolver.isDarkMode(
        isEink = com.i.miniread.BuildConfig.IS_EINK,
        systemNightMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    )
    LaunchedEffect(pageLoadTick, isDarkTheme) {
        if (pageLoadTick == 0 || released) return@LaunchedEffect
        val css = withContext(Dispatchers.IO) {
            themeCssFor(context, isDarkTheme)
        }
        if (released) return@LaunchedEffect
        applyThemeCss(webView, css)
    }

    LaunchedEffect(pageLoadTick, sizeSignature) {
        if (pageLoadTick == 0) return@LaunchedEffect   // 页面尚未 onPageFinished

        val isPageLoadEvent = pageLoadTick != lastHandledPageLoadTick
        if (isPageLoadEvent) {
            lastHandledPageLoadTick = pageLoadTick     // 真·加载事件 → 走下面的恢复兜底
        } else {
            // 纯布局变化（旋转/分屏，页面未重载）：先等一次重排，再判断原生落点是否已正确。
            // 旋转会引发 CSS 重排（width/height 对调），Chromium 可能只保留像素偏移而没做滚动锚定；
            // 判据用「文档比例偏差」：偏差小 → 原生已正确，绝不 scrollTo（多动一次反而把位置挪走）；
            // 偏差大 / 位置丢失 → 交给下面的比例映射兜底纠正。
            delay(SCROLL_LAYOUT_SETTLE_DELAY_MS)
            val native = measureScrollSnapshot(webView)
            if (!ReadingPositionResolver.needsRestoreAfterLayoutChange(
                    native, currentRequestSavedPosition()
                )
            ) {
                scrollDebounceJob?.cancel()
                recordScrollPosition(webView, force = true) // 归一化落盘（新布局下的真实位置）
                Log.d(
                    "ArticleWebView",
                    "Layout change: native position ok (y=${native.scrollY}), skip restore"
                )
                return@LaunchedEffect
            }
            Log.d("ArticleWebView", "Layout change: native position off, restoring by ratio")
        }

        val saved = currentRequestSavedPosition() ?: return@LaunchedEffect
        if (saved.scrollY <= 0) return@LaunchedEffect    // 无需恢复（零开销早退）

        Log.d("ArticleWebView", "Restoring position: $saved, size=$sizeSignature")
        delay(SCROLL_RESTORE_SETTLE_DELAY_MS)           // 等 WebView 按新视口完成重排

        var resolved = 0
        var latest = measureScrollSnapshot(webView)
        for (attempt in 0..SCROLL_RESTORE_MAX_ATTEMPTS) {
            latest = measureScrollSnapshot(webView)
            if (latest.contentHeight > 0 && latest.viewportHeight > 0) {
                resolved = ReadingPositionResolver.resolve(saved, latest)
                if (resolved > 0) break
            }
            // 图片 / 字体晚到会改变内容高度，重试吸收；E-Ink 无需更长等待
            if (attempt < SCROLL_RESTORE_MAX_ATTEMPTS) delay(SCROLL_RESTORE_RETRY_DELAY_MS)
        }
        if (resolved <= 0) {
            Log.w("ArticleWebView", "Restore aborted: layout not ready, saved=$saved")
            return@LaunchedEffect
        }

        scrollDebounceJob?.cancel()                     // 清掉可能残留的「滚到底」防抖任务
        scrollGuardUntil = SystemClock.uptimeMillis() + SCROLL_GUARD_MS
        webView.scrollTo(0, resolved)                   // 每次恢复只滚动一次（E-Ink 少一次屏刷）
        Log.d("ArticleWebView", "Restored scrollY=$resolved")
        // 归一化落盘：存实际落点（scrollTo 后立刻读 scrollY 可能还是旧值，故直接写 resolved）
        updatedOnPositionChanged(
            ScrollSnapshot(
                scrollY = resolved,
                contentHeight = latest.contentHeight,
                viewportHeight = latest.viewportHeight
            )
        )
    }

    AndroidView(
        factory = { webView },
        modifier = Modifier.fillMaxSize(),
        onRelease = { view ->
            // 离开阅读页：先补记最终阅读位置，再彻底销毁 WebView（顺序不可颠倒——
            // 先 destroy 的话最后一次位置就白丢了）。WebView 持有 native 资源，
            // 不显式 destroy 会随每次重建泄漏。
            released = true
            recordScrollPosition(view, force = true)
            view.stopLoading()
            view.loadUrl("about:blank") // 中断残留 JS / 请求回调
            (view.parent as? android.view.ViewGroup)?.removeView(view)
            view.destroy()
        }
    )
}

/** 纯布局变化（旋转/分屏、页面未重载）后，观察原生落点前的等待时间：一次重排的余量。 */
private const val SCROLL_LAYOUT_SETTLE_DELAY_MS = 150L
/** 恢复前等待 WebView 按新视口完成重排的时间：2~3 帧重排 + 一次 JS 往返的余量。 */
private const val SCROLL_RESTORE_SETTLE_DELAY_MS = 250L
/** 内容高度尚未稳定时的重试间隔与最大次数。 */
private const val SCROLL_RESTORE_RETRY_DELAY_MS = 150L
private const val SCROLL_RESTORE_MAX_ATTEMPTS = 3
/** 程序化滚动后抑制「滚到底」误判的时间窗。 */
private const val SCROLL_GUARD_MS = 300L
/** 滚动写入节流：位移闸 / 时间闸（慢速拖动也能落盘）。 */
private const val SCROLL_SAVE_MIN_DELTA_PX = 64
private const val SCROLL_SAVE_MAX_INTERVAL_MS = 300L

/**
 * 读取 WebView 当前滚动快照。
 * ⚠️ 三者同为 **view px**，contentHeight **不乘 scale**——写入与恢复必须走同一口径，
 * 否则比例换算会引入 scale 比值的系统偏差。
 * （注意：下方滚动到底判定里的 `contentHeight * scale` 是既有口径，本轮按约定未改动。）
 */
private fun measureScrollSnapshot(webView: WebView): ScrollSnapshot = ScrollSnapshot(
    scrollY = webView.scrollY,
    contentHeight = webView.contentHeight,
    viewportHeight = webView.height
)

fun interceptWebRequest(
    request: WebResourceRequest,
    refererRules: List<Pair<String, String>>
): WebResourceResponse? {
    val tag = "ArticleDetailScreen"
    return try {
        val modifiedRequest = Request.Builder()
            .url(request.url.toString())
            .header("Referer", InterceptionConfig.refererFor(request.url.host, refererRules))
            .build()

        val response: Response = interceptionHttpClient.newCall(modifiedRequest).execute()

        WebResourceResponse(
            response.header("Content-Type", "text/html"),
            response.header("Content-Encoding", "utf-8"),
            response.body?.byteStream()
        )
    } catch (e: Exception) {
        Log.e(tag, "Error intercepting web request: ${e.message}")
        null
    }
}

// 读取文件内容的函数
fun readAssetFile(context: Context, fileName: String): String {
    return context.assets.open(fileName).bufferedReader().use { it.readText() }
}


private var cachedNormalizeCss: String? = null
private var cachedThemeCssLight: String? = null
private var cachedThemeCssDark: String? = null
private var cachedTabletCss: String? = null   // 平板样式：始终注入，是否生效由 CSS 媒体查询决定
private var cachedHtmlTemplate: String? = null

/**
 * 读取主题样式表（明 / 暗**各缓存一份**）。
 *
 * ⚠️ 不要退回「只缓存一份」的写法：那样首次读取的主题会被进程级缓存钉死，
 * 之后无论系统切到哪一侧都会注入同一份 CSS（深浅色切换不生效）。
 */
fun themeCssFor(context: Context, isDarkMode: Boolean): String {
    val cached = if (isDarkMode) cachedThemeCssDark else cachedThemeCssLight
    if (cached != null) return cached

    val loaded = readAssetFile(context, if (isDarkMode) "customdark.css" else "custom.css")
    if (isDarkMode) cachedThemeCssDark = loaded else cachedThemeCssLight = loaded
    return loaded
}

/**
 * 就地替换页面内的主题样式表（`<style id="miniread-theme-css">`）。
 *
 * 只改样式文本、不触发页面重载 —— 阅读位置、滚动状态与已读判定都不受影响。
 * CSS 通过 JSON 字符串字面量注入，避免引号 / 换行破坏脚本。
 */
private fun applyThemeCss(webView: WebView, css: String) {
    val script = "(function(){var e=document.getElementById('miniread-theme-css');" +
        "if(e){e.textContent=${JSONObject.quote(css)};}return true;})()"
    webView.evaluateJavascript(script, null)
}

// 异步加载并缓存文件内容的函数
fun loadHtmlContentAsync(context: Context, content: String, onHtmlReady: (String) -> Unit) {
    // ⚠️ 主题判定必须走 ReaderThemeResolver：E-Ink 恒浅色（与 Compose 侧一致），
    //    standard 跟随系统。此前这里直接读系统 uiMode，导致 E-Ink 版正文跟着系统变深。
    //    运行期就地刷新的那一处（ArticleWebView 的 LaunchedEffect）必须用同一判据。
    val isDarkMode = ReaderThemeResolver.isDarkMode(
        isEink = com.i.miniread.BuildConfig.IS_EINK,
        systemNightMode = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    )

    CoroutineScope(Dispatchers.IO).launch {
        if (cachedNormalizeCss == null) {
            cachedNormalizeCss = readAssetFile(context, "normalize.css")
        }

        // 主题样式表：按当前 uiMode 取对应那一份（明/暗分别缓存）
        val themeCss = themeCssFor(context, isDarkMode)

        // 平板样式「始终注入」：是否生效交给 CSS 媒体查询
        // （(min-width:600px) and (min-height:480px)，高度守卫排除手机横屏）。
        // ⚠️ 不要恢复 Android 侧 screenWidthDp 一次性判定：那会让手机横屏被误限宽，
        //    且页面加载后旋转不会重新判定（正文贴左 / 右侧留白）。
        if (cachedTabletCss == null) {
            cachedTabletCss = readAssetFile(context, "tablet_styles.css")
        }

        if (cachedHtmlTemplate == null) {
            cachedHtmlTemplate = readAssetFile(context, "template.html")
        }

        // 构建HTML内容（主题 CSS 与 tablet CSS 分开注入，便于 uiMode 切换时只刷新主题块）
        val tabletCss = cachedTabletCss ?: ""
        val htmlContent = cachedHtmlTemplate?.let { template ->
            cachedNormalizeCss?.let { normalizeCss ->
                template
                    .replace("\$normalize_css", normalizeCss)
                    .replace("\$custom_css", themeCss)
                    .replace("\$tablet_css", tabletCss)
                    .replace("\$content", content)
            }
        } ?: run {
            Log.e("ArticleWebView", "Failed to load HTML template or CSS")
            "<html><body><h1>Error</h1><p>Failed to load content template</p></body></html>"
        }
//        Log.d("loadHtmlContentAsync", "loadHtmlContentAsync: htmlContent $htmlContent")

        if (isOutputHtmlContent) {
            CoroutineScope(Dispatchers.IO).launch {

                val file = saveHtmlToFile(context, htmlContent)
                Log.d("loadHtmlContentAsync", "HTML content saved to: ${file.absolutePath}")
            }
        }
        // 在主线程更新UI
        withContext(Dispatchers.Main) {
            onHtmlReady(htmlContent)
        }
    }
}

fun saveHtmlToFile(context: Context, htmlContent: String): File {
    val file = File(context.cacheDir, "output.html")
    file.writeText(htmlContent)
    return file
}
