package com.i.miniread.ui

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

fun buildHtmlContent(content: String): String {
    return """
        <html>
        <head>
            <style>
                body { font-size: 18px; line-height: 1.6; margin: 0; padding: 16px; color: #333333; }
                img { max-width: 100%; height: auto; }
            </style>
        </head>
        <body>
            $content
        </body>
        </html>
    """.trimIndent()
}

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

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when {
            selectedEntry == null || selectedEntry?.id != entryId -> {
                Text(text = "Loading...", modifier = Modifier.align(Alignment.Center))
            }
            else -> {
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
                            textZoom = 125  // 提高字体可读性
                        }
                        setBackgroundColor(0x00000000)
                    }
                }

                // 动态生成 HTML 内容
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
