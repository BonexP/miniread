package com.i.miniread.ui

import android.annotation.SuppressLint
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.i.miniread.viewmodel.MinifluxViewModel
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ArticleDetailScreen(viewModel: MinifluxViewModel, entryId: Int) {
    val selectedEntry by viewModel.selectedEntry.observeAsState()
    val context = LocalContext.current

    // 仅在需要时加载数据
    LaunchedEffect(entryId, selectedEntry?.id) {
        if (selectedEntry?.id != entryId) {
            viewModel.loadEntryById(entryId)
        }
    }

    val tag = "ArticleDetailScreen"
    Log.d(tag, "Now viewing entryId=$entryId")
    Log.d(tag, "ArticleDetailScreen: entry value: ${selectedEntry?.id}")
    Log.d(tag, "ArticleDetailScreen: entry feedid: ${selectedEntry?.feed_id}")

    Scaffold(
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = {
                    Log.d("ArticleDetailScreen", "Mark Entry as read")
                    viewModel.markEntryAsRead(entryId)
                }) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Mark As Read"
                    )
                }
                IconButton(onClick = {
                    Log.d("ArticleDetailScreen", "Bookmark Entry")
                    // Add bookmark logic here
                }) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Bookmark"
                    )
                }
                IconButton(onClick = {
                    Log.d("ArticleDetailScreen", "Favorite Entry")
                    // Add favorite logic here
                }) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite"
                    )
                }
                IconButton(onClick = {
                    Log.d("ArticleDetailScreen", "Share Entry")
                    // Add share logic here
                }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)) {
            when {
                selectedEntry == null || selectedEntry?.id != entryId -> {
                    Text(text = "Loading...", modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    val shouldInterceptRequests = selectedEntry?.feed_id in listOf(26, 38, 52, 51)

                    // 使用 remember 对 WebView 进行缓存
                    val webView = remember {
                        WebView(context).apply {
                            webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    if (shouldInterceptRequests) {
                                        if (request?.url?.host?.contains("cdnfile.sspai.com") == true) {
                                            Log.d(tag, "shouldInterceptRequest: intercept request work!")
                                            val modifiedRequest = Request.Builder()
                                                .url(request.url.toString())
                                                .header("Referer", "https://sspai.com/")
                                                .build()

                                            val client = OkHttpClient()
                                            val response: Response = client.newCall(modifiedRequest).execute()

                                            return WebResourceResponse(
                                                response.header("Content-Type", "text/html"),
                                                response.header("Content-Encoding", "utf-8"),
                                                response.body?.byteStream()
                                            )
                                        } else if (request?.url?.host?.contains("sinaimg.cn") == true) {
                                            Log.d(tag, "shouldInterceptRequest: intercept request work! sinaimg.cn!")
                                            val modifiedRequest = Request.Builder()
                                                .url(request.url.toString())
                                                .header("Referer", "https://weibo.com/")
                                                .build()

                                            val client = OkHttpClient()
                                            val response: Response = client.newCall(modifiedRequest).execute()

                                            return WebResourceResponse(
                                                response.header("Content-Type", "text/html"),
                                                response.header("Content-Encoding", "utf-8"),
                                                response.body?.byteStream()
                                            )
                                        }
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }
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
                            }
                            setBackgroundColor(0x00000000)
                        }
                    }

                    val htmlContent = selectedEntry!!.content?.let { buildHtmlContent(it) }

                    LaunchedEffect(selectedEntry?.content) {
                        if (htmlContent != null) {
                            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                        }
                    }

                    AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
                }
            }
        }
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
