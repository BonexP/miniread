package com.i.miniread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.i.miniread.ui.FeedListScreen
import com.i.miniread.ui.LoginScreen
import com.i.miniread.viewmodel.MinifluxViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MinifluxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainContent(viewModel)
        }
    }
}

@Composable
fun MainContent(viewModel: MinifluxViewModel) {
    if (viewModel.authToken.value == null) {
        LoginScreen(viewModel) {
            viewModel.fetchFeeds()
        }
    } else {
        FeedListScreen(viewModel)
    }
}
