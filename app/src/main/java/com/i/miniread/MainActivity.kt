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
            LoginScreen(viewModel) { token ->
                // Handle successful login, e.g., navigate to another screen
            }
        }
    }
}
