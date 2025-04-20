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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.BottomAppBar
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.i.miniread.util.PreferenceManager
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
                ArticleWebView(
                    context = context,
                    content = selectedEntry!!.content,
                    feedId = selectedEntry!!.feed_id,
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
    BottomAppBar {
        ActionButton(icon = Icons.Default.CheckCircle, description = "Mark as Read") {
            Log.d("ArticleDetailScreen", "Mark Entry as Read")
            viewModel.markEntryAsRead(entryId)
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Marked as read")
            }
        }
        ActionButton(icon = Icons.Outlined.CheckCircle, description = "Mark as unread") {
            viewModel.markEntryAsUnread(entryId)
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Marked as unread")
            }
        }

        ActionButton(icon = Icons.Default.Share, description = "Share") {
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
        ActionButton(icon = Icons.Default.ExitToApp, description = "Open External") {
            Log.d("ArticleDetailScreen", "Open Entry External")
            selectedEntry?.let {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.url))
                context.startActivity(intent)

            }
        }
        // ArticleDetailScreen.kt
        ActionButton(icon = Icons.Default.ArrowBack, description = "Previous") {
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
                    snackbarHostState.showSnackbar("已经是第一篇了")
                }
            }
        }

        ActionButton(icon = Icons.Default.ArrowForward, description = "Next") {
            viewModel.navigateToNextEntry(entryId)?.let { nextId ->
                // 使用查询参数格式导航
                navController.navigate("articleDetail?entryId=$nextId") {
                    popUpTo("articleDetail?entryId=$entryId") {
                        inclusive = true
                        saveState = true
                    }
                    restoreState = true
                }
            } ?: run {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("已经是最后一篇了")
                }
            }
        }


//        ActionButton(icon=Icons.Default.Refresh, description = "refresh view"){
//            Log.d("ArticleDetailScreen", "refresh webview")
//
//
//        }
        Spacer(modifier = Modifier.weight(1f))
        selectedEntry?.let { entry ->
            val isBookmarked = entry.starred
            Switch(
                checked = isBookmarked,
                onCheckedChange = { checked ->
                    viewModel.toggleStarred(entry.id, checked)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(if (checked) "Bookmarked" else "Removed from bookmarks")
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
fun ActionButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = description)
    }
}

private const val isOutputHtmlContent = false
private fun Context.isTargetDomain() =
    PreferenceManager.baseUrl.contains("pi.lifeo3.icu", true)

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
            }
            setBackgroundColor(0x00000000)

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
    Log.d("Interceptor", "BASE_URL: ${PreferenceManager.baseUrl}")
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
        val htmlContent = cachedHtmlTemplate!!
            .replace("\$normalize_css", cachedNormalizeCss!!)
            .replace("\$custom_css", cachedCustomCss!!)
            .replace("\$content", content)
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


