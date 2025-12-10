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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.twotone.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.i.miniread.util.DataStoreManager
import com.i.miniread.viewmodel.MinifluxViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ArticleDetailScreen(viewModel: MinifluxViewModel, entryId: Int, navController: NavController) {
    Log.d("ArticleDetailScreen", "ArticleDetailScreen: now get entryId is $entryId")
    val selectedEntry by viewModel.selectedEntry.observeAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
                    text = "Loading...",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleLarge
                )
            } else {
                selectedEntry?.content?.let { Log.d("ArticleWebViewContentInject", it) }
                selectedEntry?.let { entry ->
                    ArticleWebView(
                        context = context,
                        content = "<h1>${entry.title ?: ""}</h1>${entry.content ?: ""}",
                        feedId = entry.feed_id,
                        onScrollToBottom = {
                            Log.d("ArticleDetailScreen", "Article scrolled to end!")
                            viewModel.markEntryAsRead(entryId)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Marked as read")
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
    val selectedEntry by viewModel.selectedEntry.observeAsState()
    val context = LocalContext.current

    // 定义 E-Ink 下的修饰符：纯白背景、去灰底、加顶部分割线
    val barModifier = if (com.i.miniread.BuildConfig.IS_EINK) {
        Modifier
            .height(48.dp) // 紧凑高度
            .drawBehind {
                // 绘制顶部分割线
                drawLine(
                    color = androidx.compose.ui.graphics.Color.Black,
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
        // 关键：E-Ink 模式下纯白背景
        containerColor = if (com.i.miniread.BuildConfig.IS_EINK)
            androidx.compose.ui.graphics.Color.White
        else
            MaterialTheme.colorScheme.surface,
        // 关键：E-Ink 模式下去除 Elevation 导致的灰色滤镜
        tonalElevation = if (com.i.miniread.BuildConfig.IS_EINK) 0.dp else BottomAppBarDefaults.ContainerElevation,
        // 移除默认的 Padding，让我们自己控制布局更紧凑
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        // 使用 Row 来平均分布按钮，实现“紧凑但整齐”的效果
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, // 或者 SpaceEvenly
            verticalAlignment = Alignment.CenterVertically
    ) {

            // --- 导航组 ---

            // 上一篇
            EInkAwareIconButton(
                onClick = {
            viewModel.navigateToPreviousEntry(entryId)?.let { prevId ->
                navController.navigate("articleDetail?entryId=$prevId") {
                    popUpTo("articleDetail?entryId=$prevId") {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            } ?: run {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("已经是第一篇了")
                }
            }
                },
                icon = Icons.AutoMirrored.Filled.ArrowBack, // 使用 AutoMirrored 适应 RTL
                description = "Previous"
            )

            // 下一篇
            EInkAwareIconButton(
                onClick = {
            viewModel.navigateToNextEntry(entryId)?.let { nextId ->
                navController.navigate("articleDetail?entryId=$nextId") {
                    popUpTo("articleDetail?entryId=$nextId") {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            } ?: run {
                        coroutineScope.launch { snackbarHostState.showSnackbar("已经是最后一篇了") }
                }
                },
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                description = "Next"
            )

            // --- 状态组 ---

            // 标记已读
            EInkAwareIconButton(
                onClick = {
                    viewModel.markEntryAsRead(entryId)
                    coroutineScope.launch { snackbarHostState.showSnackbar("Marked as read") }
                },
                icon = Icons.Default.CheckCircle,
                description = "Mark Read"
            )

            // 标记未读 (如果需要保留的话)
            EInkAwareIconButton(
                onClick = {
                    viewModel.markEntryAsUnread(entryId)
                    coroutineScope.launch { snackbarHostState.showSnackbar("Marked as unread") }
                },
                icon = Icons.Outlined.CheckCircle,
                description = "Mark Unread"
            )

            // --- 系统/功能组 ---

            // 收藏 (原本是 Switch，现在改为图标切换，更紧凑)
        selectedEntry?.let { entry ->
            val isBookmarked = entry.starred
                IconToggleButton(
                checked = isBookmarked,
                onCheckedChange = { checked ->
                    viewModel.toggleStarred(entry.id, checked)
                    coroutineScope.launch {
                            snackbarHostState.showSnackbar(if (checked) "Bookmarked" else "Removed bookmark")
                        }
                    }
                ) {
                    Icon(
                        // 选中是用实心星，未选中用空心星
                        imageVector = if (isBookmarked) Icons.Filled.Star else Icons.TwoTone.Star,
                        contentDescription = "Bookmark",
                        // E-Ink 颜色处理
                        tint = if (com.i.miniread.BuildConfig.IS_EINK) {
                            androidx.compose.ui.graphics.Color.Black // 墨水屏始终黑色，靠实心/空心区分
                        } else {
                             if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            // 分享
            EInkAwareIconButton(
                onClick = {
                    selectedEntry?.let {
                        val shareIntent = Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, it.title)
                                putExtra(Intent.EXTRA_TEXT, "${it.title}\n${it.url}")
                            }, null
                        )
                        context.startActivity(shareIntent)
                    }
                },
                icon = Icons.Default.Share,
                description = "Share"
                )

            // 外部打开
            EInkAwareIconButton(
                onClick = {
                    selectedEntry?.let {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.url))
                        context.startActivity(intent)
                    }
                },
                icon = Icons.Default.ExitToApp,
                description = "External"
            )
        }
    }
}

// 封装一个简单的 Helper 组件来处理颜色逻辑，减少重复代码
@Composable
private fun EInkAwareIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            // E-Ink 模式下强制使用纯黑，普通模式使用主题色
            tint = if (com.i.miniread.BuildConfig.IS_EINK)
                androidx.compose.ui.graphics.Color.Black
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}
@Composable
fun ActionButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = description)
    }
}

private const val isOutputHtmlContent = false
private fun Context.isTargetDomain(): Boolean {
    // 使用 runBlocking 同步读取 DataStore 数据
    return runBlocking {
        val baseUrl = DataStoreManager.getBaseUrl()
        baseUrl.contains("pi.lifeo3.icu", true)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ArticleWebView(
    context: Context,
    content: String?,
    feedId: Int?,
    onScrollToBottom: () -> Unit
) {
    val context = LocalContext.current
    val shouldInterceptByDomain = context.isTargetDomain()
    val shouldInterceptRequests = shouldInterceptByDomain && feedId in listOf(26, 38, 52, 51)
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
                        interceptWebRequest(request)
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

    // 当HTML内容加载完成后，更新WebView的内容
    htmlContent?.let {
        webView.loadDataWithBaseURL(null, it, "text/html", "UTF-8", null)
    }
    AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
}

fun interceptWebRequest(request: WebResourceRequest): WebResourceResponse? {
    val baseUrl = runBlocking { DataStoreManager.getBaseUrl() }
    Log.d("Interceptor", "BASE_URL: $baseUrl")
    Log.d("Interceptor", "Request host: ${request.url.host}")

    val tag = "ArticleDetailScreen"
    return try {
        val modifiedRequest = Request.Builder()
            .url(request.url.toString())
            .header(
                "Referer",
                if (request.url.host?.contains("sspai.com") == true) "https://sspai.com/"
                else if (request.url.host?.contains("sinaimg.cn") == true) "https://weibo.com/"
                else ""
            )
            .build()

        val client = OkHttpClient()
        val response: Response = client.newCall(modifiedRequest).execute()

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
    CoroutineScope(Dispatchers.IO).launch {
        if (cachedNormalizeCss == null) {
            cachedNormalizeCss = readAssetFile(context, "normalize.css")
        }

        if (cachedCustomCss == null) {
            cachedCustomCss =
                readAssetFile(context, if (isDarkMode) "customdark.css" else "custom.css")
            Log.d("mycachedCustomCss", "loadHtmlContentAsync: $cachedCustomCss")
        }
        if (cachedHtmlTemplate == null) {
            cachedHtmlTemplate = readAssetFile(context, "template.html")
        }

        // 构建HTML内容
        val htmlContent = cachedHtmlTemplate?.let { template ->
            cachedNormalizeCss?.let { normalizeCss ->
                cachedCustomCss?.let { customCss ->
                    template
                        .replace("\$normalize_css", normalizeCss)
                        .replace("\$custom_css", customCss)
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
