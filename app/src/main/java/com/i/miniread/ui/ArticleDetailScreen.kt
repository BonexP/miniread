package com.i.miniread.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
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
                selectedEntry?.content?.let { Log.d("ArticleWebViewContentInject", it) }
                selectedEntry?.let { entry ->
                    ArticleWebView(
                        content = "<h1>${entry.title ?: ""}</h1>${entry.content ?: ""}",
                        feedId = entry.feed_id,
                        onScrollToBottom = {
                            Log.d("ArticleDetailScreen", "Article scrolled to end!")
                            viewModel.markEntryAsRead(entryId)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(markedReadMsg)
                            }
                        }
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
    onScrollToBottom: () -> Unit
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
    val contentHeightState = remember { mutableStateOf<Float?>(null) }
    var scrollDebounceJob by remember { mutableStateOf<Job?>(null) }
    val updatedOnScrollToBottom by rememberUpdatedState(onScrollToBottom)

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

                    // 延迟执行，确保资源加载完毕
                    view?.postDelayed({
                        view.evaluateJavascript(
                            "(function() { return document.body.scrollHeight; })();"
                        ) { result ->
                            val contentHeight = result?.toFloatOrNull()
                            if (contentHeight != null) {
                                contentHeightState.value = contentHeight
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
                        }
                    }, 500) // 延迟 500ms 执行

                    view?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
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
        onDispose { (context as? MainActivity)?.setActiveWebView(null) }
    }

    // HTML 内容就绪后加载，且仅在 htmlContent 变化时执行一次：
    // 收藏 toggle / 标记已读等引发的重组不会重载页面，阅读进度得以保留。
    // ⚠️ 不要把 loadDataWithBaseURL 放在组合体主体里——那样每次重组都会重载页面。
    LaunchedEffect(htmlContent) {
        htmlContent?.let {
            webView.loadDataWithBaseURL(null, it, "text/html", "UTF-8", null)
        }
    }

    AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
}

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
