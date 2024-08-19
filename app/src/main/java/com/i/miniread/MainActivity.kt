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
            LoginScreen(viewModel = viewModel) {
                // Handle successful login, such as navigating to another screen
                // You can use viewModel.fetchFeeds(viewModel.authToken.value) to fetch feeds after successful login
            }
        }
    }
}
