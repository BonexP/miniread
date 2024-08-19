package com.i.miniread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.i.miniread.ui.LoginScreen
import com.i.miniread.viewmodel.MinifluxViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MinifluxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen(viewModel) { authToken ->
                viewModel.fetchFeeds(authToken)
                // 继续执行其他操作，例如导航到下一个屏幕
            }
        }
    }
}
