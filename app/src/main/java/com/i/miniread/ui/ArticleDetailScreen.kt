package com.i.miniread.ui

import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when {
            selectedEntry == null || selectedEntry?.id != entryId -> {
                Text(text = "Loading...", modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                // 使用 remember 对 WebView 进行缓存
                val webView = remember {
                    WebView(context).apply {
                        webViewClient = WebViewClient()
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
                            textZoom = 125  // 增大文字大小，提升可读性
                        }
                        setBackgroundColor(0x00000000)  // 透明背景，避免不必要的重绘
                    }
                }

                // 定义适合移动设备的HTML样式
                val htmlContent = """
                    <html>
                    <head>
                        <style>
                            body { font-size: 18px; line-height: 1.6; margin: 0; padding: 16px; color: #333333; }
                            img { max-width: 100%; height: auto; }
                        </style>
                    </head>
                    <body>
                        ${selectedEntry!!.content}
                    </body>
                    </html>
                """.trimIndent()

                // 仅在 selectedEntry 内容变化时加载数据
                LaunchedEffect(selectedEntry?.content) {
                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }

                AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
