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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import com.i.miniread.util.DomainHelper
import com.i.miniread.util.InterceptionConfig
import com.i.miniread.util.InterceptionRuntime
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    // 大字符串拼接用 remember 缓存：仅当标题/正文变化时重建，避免每次重组重复拼接整篇文章。
                    // key 只取这两个字段（不用 entry 对象——收藏 toggle 会产生新实例但内容相同，白重算）。
                    // 值相等 → LaunchedEffect(content) 不会重启 → 不重载，语义不变。
                    val webViewHtml = remember(entry.title, entry.content) {
                        "<h1>${entry.title ?: ""}</h1>${entry.content ?: ""}"
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
    BottomAppBar(
        modifier = barModifier,
        // E-Ink 版本使用纯白背景并去掉 tonalElevation，避免灰底与残影
        containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isEink) 0.dp else BottomAppBarDefaults.ContainerElevation,
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        // 标记为已读按钮
        ActionButton(icon = Icons.Default.CheckCircle, description = stringResource(R.string.article_action_mark_read), compact = isEink) {
            Log.d("ArticleDetailScreen", "Mark Entry as Read")
            viewModel.markEntryAsRead(entryId)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(markedReadMsg)
            }
        // 标记为未读按钮
        }
        ActionButton(icon = Icons.Outlined.CheckCircle, description = stringResource(R.string.article_action_mark_unread), compact = isEink) {
            viewModel.markEntryAsUnread(entryId)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(markedUnreadMsg)
            }
        }
        //分享按钮
        ActionButton(icon = Icons.Default.Share, description = stringResource(R.string.article_action_share), compact = isEink) {
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
        //外部打开按钮
        ActionButton(icon = Icons.Default.ExitToApp, description = stringResource(R.string.article_action_open_external), compact = isEink) {
            Log.d("ArticleDetailScreen", "Open Entry External")
            selectedEntry?.let {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.url))
                context.startActivity(intent)

            }
        }
        //上一篇按钮
        ActionButton(icon = Icons.Default.ArrowBack, description = stringResource(R.string.article_action_previous), compact = isEink) {
            viewModel.navigateToPreviousEntry(entryId)?.let { prevId ->
                Log.d("ArticleDetailScreen", "Previous Entry: $prevId (Type: ${prevId::class.java.simpleName})")

                // 使用查询参数格式导航
                navController.navigate("articleDetail?entryId=$prevId") {

                    // 保持导航栈整洁
                    popUpTo("articleDetail?entryId=$prevId") {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            } ?: run {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(firstEntryMsg)
                }
            }
        }
        //下一篇按钮
        ActionButton(icon = Icons.Default.ArrowForward, description = stringResource(R.string.article_action_next), compact = isEink) {
            viewModel.navigateToNextEntry(entryId)?.let { nextId ->
                Log.d("ArticleDetailScreen", "Next Entry: $nextId (Type: ${nextId::class.java.simpleName})")

                // 使用查询参数格式导航
                navController.navigate("articleDetail?entryId=$nextId") {

                    // 保持导航栈整洁
                    popUpTo("articleDetail?entryId=$nextId") {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            } ?: run {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(lastEntryMsg)
                }
            }
        }

//todo 刷新视图
//        ActionButton(icon=Icons.Default.Refresh, description = "refresh view"){
//            Log.d("ArticleDetailScreen", "refresh webview")
//
//
//        }
        Spacer(modifier = Modifier.weight(1f))
        selectedEntry?.let { entry ->
            //收藏按钮
            val isBookmarked = entry.starred
            Switch(
                checked = isBookmarked,
                onCheckedChange = { checked ->
                    viewModel.toggleStarred(entry.id, checked)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(if (checked) bookmarkedMsg else unbookmarkedMsg)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    description: String,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = if (compact) Modifier.size(40.dp) else Modifier
    ) {
        Icon(imageVector = icon, contentDescription = description)
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
private var cachedCustomCss: String? = null
private var cachedHtmlTemplate: String? = null

// 异步加载并缓存文件内容的函数
fun loadHtmlContentAsync(context: Context, content: String, onHtmlReady: (String) -> Unit) {
    val isDarkMode = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    // 检测是否为平板设备
    val isTablet = context.resources.configuration.screenWidthDp >= 600

    CoroutineScope(Dispatchers.IO).launch {
        if (cachedNormalizeCss == null) {
            cachedNormalizeCss = readAssetFile(context, "normalize.css")
        }

        if (cachedCustomCss == null) {
            cachedCustomCss =
                readAssetFile(context, if (isDarkMode) "customdark.css" else "custom.css")
            Log.d("mycachedCustomCss", "loadHtmlContentAsync: $cachedCustomCss")
        }

        // 平板设备加载额外的优化CSS
        val tabletCss = if (isTablet) {
            readAssetFile(context, "tablet_styles.css")
        } else {
            ""
        }

        if (cachedHtmlTemplate == null) {
            cachedHtmlTemplate = readAssetFile(context, "template.html")
        }

        // 构建HTML内容，为平板添加额外的CSS
        val htmlContent = cachedHtmlTemplate?.let { template ->
            cachedNormalizeCss?.let { normalizeCss ->
                cachedCustomCss?.let { customCss ->
                    template
                        .replace("\$normalize_css", normalizeCss)
                        .replace("\$custom_css", "$customCss\n$tabletCss")
                        .replace("\$content", content)
                }
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
