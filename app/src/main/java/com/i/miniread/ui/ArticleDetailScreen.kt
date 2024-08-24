package com.i.miniread.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import com.i.miniread.viewmodel.MinifluxViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ArticleDetailScreen(viewModel: MinifluxViewModel, entryId: Int) {
    val selectedEntry by viewModel.selectedEntry.observeAsState()
    val context = LocalContext.current

    LaunchedEffect(entryId, selectedEntry?.id) {
        if (selectedEntry?.id != entryId) {
            viewModel.loadEntryById(entryId)
        }
    }

    Scaffold(
        bottomBar = { ArticleActionsBar(viewModel, entryId) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (selectedEntry == null || selectedEntry?.id != entryId) {
                Text(text = "Loading...", modifier = Modifier.align(Alignment.Center))
            } else {
                ArticleWebView(
                    context = context,
                    content = selectedEntry!!.content,
                    feedId = selectedEntry!!.feed_id,
                    onScrollToBottom = {
                        Log.d("ArticleDetailScreen", "ArticleDetailScreen: Article scroll to end!")
                        // Mark the article as read when scrolled to the bottom
                        viewModel.markEntryAsRead(entryId)
                    }
                )
            }
        }
    }
}

@Composable
fun ArticleActionsBar(viewModel: MinifluxViewModel, entryId: Int) {
    BottomAppBar {
        ActionButton(icon = Icons.Default.CheckCircle, description = "Mark as Read") {
            Log.d("ArticleDetailScreen", "Mark Entry as Read")
            viewModel.markEntryAsRead(entryId)
        }
        ActionButton(icon = Icons.Default.ThumbUp, description = "Bookmark") {
            Log.d("ArticleDetailScreen", "Bookmark Entry")
            // Add bookmark logic here
        }
        ActionButton(icon = Icons.Default.Favorite, description = "Favorite") {
            Log.d("ArticleDetailScreen", "Favorite Entry")
            // Add favorite logic here
        }
        ActionButton(icon = Icons.Default.Share, description = "Share") {
            Log.d("ArticleDetailScreen", "Share Entry")
            // Add share logic here
        }
    }
}

@Composable
fun ActionButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = description)
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
    val shouldInterceptRequests = feedId in listOf(26, 38, 52, 51)
    val coroutineScope = rememberCoroutineScope()
    val hasMarkedAsRead = remember { mutableStateOf(false) }
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

                    view?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                        if (hasMarkedAsRead.value) return@setOnScrollChangeListener

                        val remainingScroll = view.contentHeight * view.scale - (scrollY + view.height)
                        if (scrollY > oldScrollY && remainingScroll < 50) {
                            scrollDebounceJob?.cancel()
                            scrollDebounceJob = coroutineScope.launch {
                                delay(500)
                                if (remainingScroll < 50) {
                                    Log.d("ArticleWebView", "Debounced bottom reached, marking as read.")
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

    val htmlContent = content?.let { buildHtmlContent(it) }

    LaunchedEffect(htmlContent) {
        htmlContent?.let {
            webView.loadDataWithBaseURL(null, it, "text/html", "UTF-8", null)
        }
    }

    AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
}

fun interceptWebRequest(request: WebResourceRequest): WebResourceResponse? {
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

fun buildHtmlContent(content: String): String {
    return """
        <html>
        <head>
            <style>
                body { font-size: 3rem; line-height: 1.6; margin: 0; padding: 16px; color: #333333; }
                img { max-width: 100%; height: auto; }
            </style>
        </head>
        <body>
            $content
        </body>
        </html>
    """.trimIndent()
}
