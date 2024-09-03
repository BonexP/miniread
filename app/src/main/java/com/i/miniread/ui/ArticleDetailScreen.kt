package com.i.miniread.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
                        Toast.makeText(context,"Marked as read",Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun ArticleActionsBar(viewModel: MinifluxViewModel, entryId: Int) {
    val selectedEntry by viewModel.selectedEntry.observeAsState()
    val context = LocalContext.current
    BottomAppBar {
        ActionButton(icon = Icons.Default.CheckCircle, description = "Mark as Read") {
            Log.d("ArticleDetailScreen", "Mark Entry as Read")
            viewModel.markEntryAsRead(entryId)
        }
        ActionButton(icon = Icons.Outlined.CheckCircle, description = "Mark as unread") {
            viewModel.markEntryAsUnread(entryId)
            Toast.makeText(context,"Marked as unread",Toast.LENGTH_SHORT).show()

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
        Spacer(modifier = Modifier.weight(1f))
        // Replace the bookmark button with a switch
        selectedEntry?.let { entry ->
            val isBookmarked = entry.starred
            Switch(
                checked = isBookmarked,
                onCheckedChange = { checked ->
                    viewModel.toggleStarred(entry.id, checked)
                    Toast.makeText(context, if (checked) "Bookmarked" else "Removed from bookmarks", Toast.LENGTH_SHORT).show()
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
                                        Log.d("ArticleWebView", "Content does not fill the screen, marking as read.")
                                        updatedOnScrollToBottom()
                                        hasMarkedAsRead.value = true
                                    }
                                }
                            }
                        }
                    }, 500) // 延迟 500ms 执行

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
            
<style>/*! normalize.css v8.0.1 | MIT License | github.com/necolas/normalize.css */

/* Document
   ========================================================================== */

/**
 * 1. Correct the line height in all browsers.
 * 2. Prevent adjustments of font size after orientation changes in iOS.
 */

html {
  line-height: 1.15; /* 1 */
  -webkit-text-size-adjust: 100%; /* 2 */
}

/* Sections
   ========================================================================== */

/**
 * Remove the margin in all browsers.
 */

body {
  margin: 0;
}

/**
 * Render the `main` element consistently in IE.
 */

main {
  display: block;
}

/**
 * Correct the font size and margin on `h1` elements within `section` and
 * `article` contexts in Chrome, Firefox, and Safari.
 */

h1 {
  font-size: 2em;
  margin: 0.67em 0;
}

/* Grouping content
   ========================================================================== */

/**
 * 1. Add the correct box sizing in Firefox.
 * 2. Show the overflow in Edge and IE.
 */

hr {
  box-sizing: content-box; /* 1 */
  height: 0; /* 1 */
  overflow: visible; /* 2 */
}

/**
 * 1. Correct the inheritance and scaling of font size in all browsers.
 * 2. Correct the odd `em` font sizing in all browsers.
 */

pre {
  font-family: monospace, monospace; /* 1 */
  font-size: 1em; /* 2 */
}

/* Text-level semantics
   ========================================================================== */

/**
 * Remove the gray background on active links in IE 10.
 */

a {
  background-color: transparent;
}

/**
 * 1. Remove the bottom border in Chrome 57-
 * 2. Add the correct text decoration in Chrome, Edge, IE, Opera, and Safari.
 */

abbr[title] {
  border-bottom: none; /* 1 */
  text-decoration: underline; /* 2 */
  text-decoration: underline dotted; /* 2 */
}

/**
 * Add the correct font weight in Chrome, Edge, and Safari.
 */

b,
strong {
  font-weight: bolder;
}

/**
 * 1. Correct the inheritance and scaling of font size in all browsers.
 * 2. Correct the odd `em` font sizing in all browsers.
 */

code,
kbd,
samp {
  font-family: monospace, monospace; /* 1 */
  font-size: 1em; /* 2 */
}

/**
 * Add the correct font size in all browsers.
 */

small {
  font-size: 80%;
}

/**
 * Prevent `sub` and `sup` elements from affecting the line height in
 * all browsers.
 */

sub,
sup {
  font-size: 75%;
  line-height: 0;
  position: relative;
  vertical-align: baseline;
}

sub {
  bottom: -0.25em;
}

sup {
  top: -0.5em;
}

/* Embedded content
   ========================================================================== */

/**
 * Remove the border on images inside links in IE 10.
 */

img {
  border-style: none;
}

/* Forms
   ========================================================================== */

/**
 * 1. Change the font styles in all browsers.
 * 2. Remove the margin in Firefox and Safari.
 */

button,
input,
optgroup,
select,
textarea {
  font-family: inherit; /* 1 */
  font-size: 100%; /* 1 */
  line-height: 1.15; /* 1 */
  margin: 0; /* 2 */
}

/**
 * Show the overflow in IE.
 * 1. Show the overflow in Edge.
 */

button,
input { /* 1 */
  overflow: visible;
}

/**
 * Remove the inheritance of text transform in Edge, Firefox, and IE.
 * 1. Remove the inheritance of text transform in Firefox.
 */

button,
select { /* 1 */
  text-transform: none;
}

/**
 * Correct the inability to style clickable types in iOS and Safari.
 */

button,
[type="button"],
[type="reset"],
[type="submit"] {
  -webkit-appearance: button;
}

/**
 * Remove the inner border and padding in Firefox.
 */

button::-moz-focus-inner,
[type="button"]::-moz-focus-inner,
[type="reset"]::-moz-focus-inner,
[type="submit"]::-moz-focus-inner {
  border-style: none;
  padding: 0;
}

/**
 * Restore the focus styles unset by the previous rule.
 */

button:-moz-focusring,
[type="button"]:-moz-focusring,
[type="reset"]:-moz-focusring,
[type="submit"]:-moz-focusring {
  outline: 1px dotted ButtonText;
}

/**
 * Correct the padding in Firefox.
 */

fieldset {
  padding: 0.35em 0.75em 0.625em;
}

/**
 * 1. Correct the text wrapping in Edge and IE.
 * 2. Correct the color inheritance from `fieldset` elements in IE.
 * 3. Remove the padding so developers are not caught out when they zero out
 *    `fieldset` elements in all browsers.
 */

legend {
  box-sizing: border-box; /* 1 */
  color: inherit; /* 2 */
  display: table; /* 1 */
  max-width: 100%; /* 1 */
  padding: 0; /* 3 */
  white-space: normal; /* 1 */
}

/**
 * Add the correct vertical alignment in Chrome, Firefox, and Opera.
 */

progress {
  vertical-align: baseline;
}

/**
 * Remove the default vertical scrollbar in IE 10+.
 */

textarea {
  overflow: auto;
}

/**
 * 1. Add the correct box sizing in IE 10.
 * 2. Remove the padding in IE 10.
 */

[type="checkbox"],
[type="radio"] {
  box-sizing: border-box; /* 1 */
  padding: 0; /* 2 */
}

/**
 * Correct the cursor style of increment and decrement buttons in Chrome.
 */

[type="number"]::-webkit-inner-spin-button,
[type="number"]::-webkit-outer-spin-button {
  height: auto;
}

/**
 * 1. Correct the odd appearance in Chrome and Safari.
 * 2. Correct the outline style in Safari.
 */

[type="search"] {
  -webkit-appearance: textfield; /* 1 */
  outline-offset: -2px; /* 2 */
}

/**
 * Remove the inner padding in Chrome and Safari on macOS.
 */

[type="search"]::-webkit-search-decoration {
  -webkit-appearance: none;
}

/**
 * 1. Correct the inability to style clickable types in iOS and Safari.
 * 2. Change font properties to `inherit` in Safari.
 */

::-webkit-file-upload-button {
  -webkit-appearance: button; /* 1 */
  font: inherit; /* 2 */
}

/* Interactive
   ========================================================================== */

/*
 * Add the correct display in Edge, IE 10+, and Firefox.
 */

details {
  display: block;
}

/*
 * Add the correct display in all browsers.
 */

summary {
  display: list-item;
}

/* Misc
   ========================================================================== */

/**
 * Add the correct display in IE 10+.
 */

template {
  display: none;
}

/**
 * Add the correct display in IE 10.
 */

[hidden] {
  display: none;
}
</style>
<style>/*
  * Magick CSS
  * by: winterveil (https://github.com/wintermute-cell/)
  * license: MIT
  * version: 1.0.5
 */
 
 @charset "UTF-8";
 
 /* Importing the fonts. */
 @import url('https://fonts.googleapis.com/css2?family=Averia+Libre:ital,wght@0,400;0,700;1,400;1,700&family=Averia+Serif+Libre:ital,wght@0,300;0,700;1,300;1,700&family=Caveat&family=Courier+Prime:ital,wght@0,400;0,700;1,400;1,700&family=Spectral:ital,wght@0,300;0,600;1,300;1,600&display=swap&family=Noto+Sans+Symbols+2&display=swap');
 
 /* Simple CSS Reset */
 * {
     margin: 0;
     padding: 0;
     box-sizing: border-box;
 }
 
 :root {
     /* Theme colors */
     --fg: #0e0e0e;
     --bg: #fefefe;
     --form-bg: #fbfbfb;
     --form-fg: #00004d;
     --form-fg-placeholder: #00004d9a;
 }
 
 @media (prefers-color-scheme: dark) {
     :root {
         --fg: #fefefe;
         --bg: #0e0e0e;
         --form-bg: #1a1a1a;
         --form-fg: #fefefe;
         --form-fg-placeholder: #fefefe9a;
     }
 }
 
 /*
     1) Modify the base font-size to 62.5% so that 1.6rem = 16px.
     2) Set box-sizing globally to handle padding and border widths.
 */
 html {
     font-size: 62.5%; /* 1 */
     box-sizing: border-box; /* 2 */
 }
 
 /* Use smaller sizes on mobile devices. */
 @media (max-width: 600px) {
     html {
         font-size: 56%;
     }
 }
 
 /*
     1) Set the base font-size to 18px with a normal weight.
     2) Set the text and background colors to match the theme.
     3) Use the 'Averia Serif Libre' font for the body text.
     4) Reset the counter for sidenotes.
 */
 body {
     font-size: 1.8rem; /* 1 */
     background-color: var(--bg); /* 2 */
     color: var(--fg); /* 2 */
     font-family: "Averia Serif Libre", serif; /* 3 */
     font-style: normal; /* 3 */
     line-height: 2.2rem; /* 3 */
     font-weight: 300; /* 3 */
 
     counter-reset: sidenote-counter; /* 4 */
 }
 
 /*
     1) Center the main content.
     2) Set the width of the element to 760px, with lower padding on mobile devices.
     3) Relative position as the default allows for absolute positioning of child elements.
 */
 article,
 main {
     margin: auto; /* 1 */
     max-width: 76rem; /* 2 */
     padding: 0 1rem; /* 2 */
     width: 100%; /* 2 */
     position: relative; /* 3 */
 }
 
 @media (max-width: 600px) {
     article,
     main {
         padding: 0 0.2rem; /* 2 */
     }
 }
 
 /* ========================================================================================================================= */
 /*  Structure & Layout ===================================================================================================== */
 /* ========================================================================================================================= */
 
 /* Display the header, main, and footer sections as distinctly separate blocks. */
 header,
 section,
 footer {
     margin: 0.7rem;
     padding: 0.7rem;
 }
 
 /* On mobile devices, a smaller margin looks more fitting due to the smaller view. */
 @media (max-width: 600px) {
     header,
     section,
     footer {
         margin-top: 0.2rem;
         margin-bottom: 0.2rem;
     }
 }
 
 /* Avoid double margin on the last child of each section */
 header>*:last-child,
 section>*:last-child,
 footer>*:last-child {
     margin-bottom: 0;
 }
 
 /* Add large margins to the header to visually separate it from the main content. */
 header {
     margin-top: 12rem;
     margin-bottom: 8rem;
 }
 
 /* On mobile devices, reduce the margin around the header to save space. */
 @media (max-width: 600px) {
     header {
         margin: 0;
         padding: 1.2rem;
     }
 }
 
 /*
     1) Center align the text in the footer.
     2) Add a margin to the top of the footer to visually separate it from the main content.
     3) Add a margin to the bottom of the footer to not have it stuck to the bottom of the page.
 */
 footer {
     text-align: center; /* 1 */
     margin-top: 5rem; /* 2 */
     margin-bottom: 2rem; /* 3 */
 }
 
 /* ========================================================================================================================= */
 /*  Typography & Links ===================================================================================================== */
 /* ========================================================================================================================= */
 
 /* Add some space between paragraphs. */
 p {
     margin: 1.6rem 0; /* 1 */
 }
 
 /* Remove link color. */
 a {
     color: var(--fg);
 }
 
 /* Make string a little more bold, to adjust for the font. */
 b,
 strong {
     font-weight: 600;
 }
 
 /* Make emphasized text a little larger, to adjust for the font. */
 i,
 em {
     font-size: calc(1em + 0.1rem);
 }
 
 /*
     1) Set the font-family, color, and font-style for the headings.
     2) Add a margin to the top and bottom of the headings.
 */
 h1,
 h2,
 h3,
 h4 {
     font-family: "Averia Libre", cursive; /* 1 */
     color: var(--fg); /* 1 */
     font-style: normal; /* 1 */
     font-weight: 600; /* 1 */
     margin: 1.6rem 0 1.6rem 0; /* 2 */
 }
 
 /* h1 headings are uppercase and 2x the size of the base font. */
 h1 {
     text-transform: uppercase;
     font-size: 3.6rem;
     line-height: 3.3rem;
 }
 
 /* h2 headings are uppercase and 1.250x (major third) the size of the base font. */
 h2 {
     font-size: 2.25rem;
     text-transform: uppercase;
     margin: 1.2rem 0 1.2rem 0;
 }
 
 /* h3 headings are 1.125x (major second) the size of the base font. */
 h3 {
     font-size: 2.025rem;
 }
 
 /* Add a decorative element before h3 headings. */
 h3:before {
     font-family: "Noto Sans Symbols 2", sans-serif;
     content: "🙛 ";
 }
 
 /* h4 headings are the same size as h3 headings, but italic and without the decorative element. */
 h4 {
     font-style: italic;
     font-size: 2.025rem;
 }
 
 /* A uniquely styled h1 for the header */
 header h1 {
     font-size: 4rem;
     color: var(--fg);
     text-align: center;
     padding: 4rem 0 1.2rem 0;
     margin-bottom: 1rem;
 }
 
 /* Additional decorations for the header h1 */
 header h1:before,
 header h1:after {
     content: "✦";
     color: var(--fg);
     font-size: 1.5rem;
     vertical-align: middle;
     padding: 0 0.5rem;
 }
 
 /*
     1) Remove any list symbols.
     2) Center align the nav links.
     3) Add a margin to the top of the nav links.
     4) Remove the default padding from the list.
 */
 header nav ul {
     list-style-type: none; /* 1 */
     text-align: center; /* 2 */
     margin-top: 1.0rem; /* 3 */
     padding-inline-start: 0; /* 4 */
 }
 
 /* Display the navigation links as a centered, horizontal list. */
 header nav ul li {
     display: inline;
     margin: 0 1.2rem;
 }
 
 /* Remove default link styles. */
 header nav ul li a {
     text-decoration: none;
     color: var(--fg);
 }
 
 /* Add a hover effect to the navigation links. */
 header nav ul li a::before {
     content: "❯ "; /* 1 */
     opacity: 0; /* 1 */
 }
 header nav ul li a:hover::before {
     opacity: 1; /* 1 */
 }
 
 /* ===================================================================================================================== */
 /*       Lists     ===================================================================================================== */
 /* ===================================================================================================================== */
 
 /*
     1) Add some indentation to the list items.
     2) Add a margin to the top and bottom of the list.
 */
 :where(main ol, main ul) {
     margin-inline-start: 0; /* 1 */
     padding-inline-start: 3rem; /* 1 */
     margin: 0.8rem 0; /* 2 */
 }
 
 /* Add some vertical space around a definition list. */
 dl {
     margin: 0.8rem 0;
 }
 
 /* Add an indent to the definition term. */
 dd {
     margin: 0 1.6rem;
 }
 
 /* ===================================================================================================================== */
 /*      Media      ===================================================================================================== */
 /* ===================================================================================================================== */
 
 /*
     1) Set the maximum width of the image to 100% so they don't overflow the main column.
     2) Set the height in respect to the width to prevent the image from stretching.
     3) Add some margin to standalone images.
 */
 img {
     max-width: 100%; /* 1 */
     height: auto; /* 2 */
     margin: 0.8rem 0; /* 3 */
 }
 
 /* Images in figures should not have their own margins */
 figure img {
     margin: 0;
     padding: 0;
 }
 
 /*
     1) Set margins and padding for figures.
     2) Center align any text inside figures.
 */
 figure {
     margin: 2rem 0; /* 1 */
     padding: 0; /* 1 */
     text-align: center; /* 2 */
 }
 
 /* Center align any element that is part of a figure */
 figure * {
     margin-left: auto;
     margin-right: auto;
 }
 
 /* We don't want to center prealigned text or code in figures */
 figure code,
 figure pre {
     text-align: left;
 }
 
 /* Set the typography for the figure captions */
 figcaption {
     margin: 0.8rem 0;
     font-size: 1.8rem;
 }
 
 /* ===================================================================================================================== */
 /*  Forms & Inputs ===================================================================================================== */
 /* ===================================================================================================================== */
 
 
 /*
     1) Display the form elements in a grid layout, two columns wide.
     2) Add some space between the form elements.
     3) Set padding and margin for the form.
     4) Give the form a pop out paper note look.
 */
 form {
     display: grid; /* 1 */
     grid-template-columns: 1fr 1fr; /* 1 */
     gap: 10px; /* 2 */
     padding: 1rem; /* 3 */
     margin: 0.8rem 0; /* 3 */
     box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.1); /* 4 */
     background-color: var(--form-bg); /* 4 */
 }
 
 /* Make form inputs and labels span two columns. (They get their own line) */
 form input[type="email"],
 form input[type="number"],
 form input[type="password"],
 form input[type="search"],
 form input[type="tel"],
 form input[type="text"],
 form input[type="url"],
 form label,
 form fieldset,
 form textarea {
     grid-column: span 2;
 }
 
 /* 
     1) Adjust the elements to take up full width of their grid cell.
     2) Prevent textarea from being resized horizontally and overflowing the main column.
 */
 form input,
 form button,
 form textarea {
     width: 100%; /* 1 */
     resize: vertical; /* 2 */
 }
 
 /*
     1) Prevent radio and checkbox from taking up full width, so they can be put next to each other.
     2) Add space between radio and checkbox options.
 */
 input[type="radio"],
 input[type="checkbox"] {
     width: auto; /* 1 */
     margin-right: 0.5rem; /* 2 */
 }
 input[type="radio"] + label,
 input[type="checkbox"] + label {
     margin-right: 2rem; /* 2 */
 }
 
 /* Add a disabled variant for radio and checkbox inputs */
 input[type="radio"]:disabled + label,
 input[type="checkbox"]:disabled + label {
     text-decoration: line-through;
     opacity: 0.5;
 }
 
 /*
     1) Remove the default appearance of the input.
     2) Set padding and margin for the input.
     3) Apply some custom styles in place of the default ones.
     4) Give the input a handwritten look.
 */
 input[type="email"],
 input[type="number"],
 input[type="password"],
 input[type="search"],
 input[type="tel"],
 input[type="text"],
 input[type="url"],
 textarea,
 select {
     -webkit-appearance: none; /* 1 */
     -moz-appearance: none; /* 1 */
     appearance: none; /* 1 */
     box-shadow: none; /* 1 */
     box-sizing: inherit; /* 1 */
     border: none; /* 1 */
 
     padding: .4rem 1rem; /* 2 */
     margin-bottom: 1.6rem; /* 2 */
 
     font-size: 2rem; /* 3 */
     color: var(--fg); /* 3 */
     background-color: transparent; /* 3 */
     border-bottom: 1px solid var(--fg); /* 3 */
     border-radius: 0; /* 3 */
     font-size: 1.8rem; /* 3 */
 
     font-family: "Caveat", cursive; /* 4 */
     font-size: 2.6rem; /* 4 */
     color: var(--form-fg); /* 4 */
     caret-color: var(--form-fg); /* 4 */
 }
 
 /* Add disabled variant for input fields */
 input[type="email"]:disabled,
 input[type="number"]:disabled,
 input[type="password"]:disabled,
 input[type="search"]:disabled,
 input[type="tel"]:disabled,
 input[type="text"]:disabled,
 input[type="url"]:disabled,
 textarea:disabled,
 select:disabled {
     border-bottom: 1px dashed var(--fg); /* 3 */
     text-decoration: line-through;
 }
 
 /* A slightly more transparent color for the placeholder text. */
 input::placeholder,
 textarea::placeholder {
     color: var(--form-fg-placeholder);
 }
 
 /*
     1) Remove the default focus outline.
     2) Add a thicker bottom border to the input when focused, reducing margin to prevent layout shifting.
 */
 input[type="email"]:focus,
 input[type="number"]:focus,
 input[type="password"]:focus,
 input[type="search"]:focus,
 input[type="tel"]:focus,
 input[type="text"]:focus,
 input[type="url"]:focus,
 textarea:focus,
 select:focus {
     outline: none; /* 1 */
     border-bottom: 2px solid var(--fg); /* 2 */
     margin-bottom: calc(1.6rem - 1px); /* 2 */
 }
 
 
 /*
     1) Match the theme colors.
     2) Add padding.
     3) Add a top margin to visually separate the buttons for the rest of the form.
     4) Add a thin border.
     5) Add a pointer cursor on hover.
 */
 button,
 input[type="button"],
 input[type="reset"],
 input[type="submit"] {
     background-color: transparent; /* 1 */
     color: var(--fg); /* 1 */
     padding: 10px; /* 2 */
     margin-top: 1.6rem; /* 3 */
     border: 1px solid var(--fg); /* 4 */
     cursor: pointer; /* 5 */
 }
 
 /*
     1) A thin border around the fieldset.
     2) Set the width of the fieldset to fit around the content.
 */
 fieldset {
     border: 1px solid var(--fg); /* 1 */
     width: fit-content; /* 2 */
 }
 
 /* In a form, the fieldset takes up 100% of the width. */
 form fieldset {
     width: 100%;
 }
 
 /*
     1) Set the border of a disabled button to be dashed.
     2) Add the not-allowed cursor when hovering a disabled button.
 */
 button:disabled,
 input[type="button"]:disabled,
 input[type="reset"]:disabled,
 input[type="submit"]:disabled {
     opacity: 0.5;
     border-style: dashed; /* 1 */
     cursor: not-allowed; /* 2 */
     text-decoration: line-through;
 }
 
 /* ===================================================================================================================== */
 /*   Tables   ========================================================================================================== */
 /* ===================================================================================================================== */
 
 /* Remove the distance between adjacent cells, since we don't have vertical border lines. */
 table {
     border-spacing: 0;
 }
 
 /* Add some padding around table cells. */
 td,
 th {
     padding: 0.4rem 1.0rem;
 }
 
 /* Remove left padding for first cell in a row. */
 td:first-child,
 th:first-child {
     padding-left: 0;
 }
 
 /* Remove right padding for last cell in a row. */
 td:last-child,
 th:last-child {
     padding-right: 0;
 }
 
 /*
     1) Add a border under the table header.
     2) Align the text to the left in the table header.
 */
 th {
     border-bottom: 2px solid var(--fg); /* 1 */
     text-align: left; /* 2 */
 }
 
 /* ============================================================================================================================ */
 /*   Preformatting, Quotes & Code  ============================================================================================ */
 /* ============================================================================================================================ */
 
 /*
     1) Set custom padding and margins.
     2) Hide the vertical scroll bar.
     3) Set the width to fit just around the content, but limit it to 80% of the main column.
     4) Center the blockquote horizontally and add some vertical margins.
 */
 blockquote {
     padding: 1rem 1.6rem; /* 1 */
     overflow-y: hidden; /* 2 */
     width: fit-content; /* 3 */
     max-width: 80%; /* 3 */
     margin: 2rem auto; /* 4 */
 }
 
 /*
     1) Set a custom font for blockquote text paragraphs.
     2) Add some space between the paragraphs.
 */
 blockquote p {
     font-family: "Spectral", serif; /* 1 */
     font-style: italic; /* 1 */
     font-size: 2.1rem; /* 1 */
     font-weight: 300; /* 1 */
     line-height: 2.4rem; /* 1 */
     margin: 2.1rem 0; /* 2 */
 }
 
 /* Add a footer to the blockquote for citations. */
 /*
     1) Reset any margins and padding from the main footer.
     2) Set the footer to float and align to the right.
     3) Limit the width of the footer to 55% of the main column.
     4) Set a custom font for the footer.
 */
 blockquote footer {
     margin: 0; /* 1 */
     padding: 0; /* 1 */
     float: right; /* 2 */
     text-align: right; /* 2 */
     width: 55%; /* 3 */
     font-family: "Spectral", serif; /* 4 */
     font-style: normal; /* 4 */
     font-size: 1.4rem; /* 4 */
 }
 
 /* Make the actual citation italic */
 blockquote footer cite {
     font-style: italic;
 }
 
 /*
     1) Set custom padding.
     2) Hide the vertical scroll bar.
     3) Set a custom monospace font.
     4) Add a top and bottom border line.
 */
 pre:has(code) {
     padding: 1rem 1.6rem; /* 1 */
     margin: 1.6rem 0; /* 1 */
     overflow-y: hidden; /* 2 */
     font-family: "Courier Prime", monospace; /* 3 */
     font-size: 1.6rem; /* 3 */
     border-top: 2px solid var(--fg); /* 4 */
     border-bottom: 2px solid var(--fg); /* 4 */
 }
 
 /* Set a custom monospace font */
 code {
     font-family: "Courier Prime", monospace;
     font-size: 1.6rem;
 }
  
 /*
     1) Float the line numbers to the left, next to the code.
     2) Make some distance between the line numbers and the code, and pull it all to the left with a negative margin.
     3) Add a vertical line to separate the line numbers from the code.
     4) Align the line numbers against the separator.
 */
 pre .line-number {
     float: left; /* 1 */
     margin: 0 1.5rem 0 -1.5rem; /* 2 */
     border-right: 1px solid; /* 3 */
     text-align: right; /* 4 */
 }
 
 /*
     1) Display as block, so we get a new line for each line number.
     2) Add some padding to the line numbers.
 */
 pre .line-number span {
     display: block; /* 1 */
     padding: 0 0.8rem 0 1.6rem; /* 2 */
 }
 
 /* ============================================================================================================================ */
 /*   Sidenotes & Asides  ====================================================================================================== */
 /* ============================================================================================================================ */
 
 /*
     1) On mobile devices, sidenotes behave the same as asides.
     2) Float the sidenotes to the right.
     3) Make sure the sidenotes don't clash.
     4) Set the width of the sidenotes to 40% of the main column.
     5) Add padding, margins and a border for better visual separation.
     6) Adjust typography to be more compact.
 */
 .sidenote, /* 1 */
 aside {
     float: right; /* 2 */
     clear: right; /* 3 */
     width: 40%; /* 4 */
     margin: 1rem 1rem 1rem 3rem; /* 5 */
     padding: 0.5rem 0.5rem 0.5rem 2rem; /* 5 */
     border-left: 3px solid var(--fg); /* 5 */
     font-size: 1.4rem; /* 6 */
     line-height: 1.3; /* 6 */
 }
 
 /* Prevent double top margins */
 aside h1, aside h2, aside h3, aside h4 {
     margin-top: 0;
 }
 
 /*
     1) Set the distance from the main column.
     2) Set the width of the element to a little less than the remaining space on one side, limited to 40% of the main column.
     3) Remove any values set for the mobile version of the sidenotes.
     4) Set the width to the variable defined above.
     5) Set a negative right margin to (self-width + distance-from-main) to pull the sidenote to the right.
 */
 /* Sadly, CSS does not support var() and rem in media queries, so we have to hardcode pixels. */
 /* This will break if the main column width is changed without adjusting this media query.    */
 @media (min-width: calc(760px + 400px)) {
     .sidenote {
         --distance-from-main: 3rem; /* 1 */
         --self-width: min(calc((100vw - 760px)/2 - (var(--distance-from-main))), 40%); /* 2 */
         margin: 0; /* 3 */
         padding: 0; /* 3 */
         border: none; /* 3 */
         width: var(--self-width);
         margin-right: calc(calc(var(--self-width) + var(--distance-from-main)) * -1);
     }
 }
 
 /* Each time a sidenote anchor is encountered, increment the counter */
 .sidenote-anchor {
     counter-increment: sidenote-counter;
 }
 
 /* Use a custom font for the sidenote numbers */
 .sidenote-anchor:after,
 .sidenote:before {
     font-size: 1.3rem;
     position: relative;
     font-family: "Spectral", serif;
 }
 
 /* Fine-adjust the number position for the sidenote anchor */
 .sidenote-anchor:after {
     content: counter(sidenote-counter);
     top: -0.5rem;
     left: 0.1rem;
 }
 
 /* Fine-adjust the number position for the sidenote */
 .sidenote:before {
     content: counter(sidenote-counter) " ";
     top: -0.5rem;
 }
 </style>
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
