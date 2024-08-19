package com.i.miniread

import android.content.Context
import android.os.Bundle
import android.util.Log
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

        Log.d("MainActivity", "onCreate: Activity started")

        // Load saved auth token
        val sharedPreferences = getSharedPreferences("miniread_prefs", Context.MODE_PRIVATE)
        val savedToken = sharedPreferences.getString("auth_token", null)
        if (savedToken != null) {
            Log.d("MainActivity", "onCreate: Found saved token")
            viewModel.setAuthToken(savedToken)
            viewModel.fetchFeeds()
        } else {
            Log.d("MainActivity", "onCreate: No saved token found")
        }

        setContent {
            MainContent(viewModel, sharedPreferences)
        }
    }
}

@Composable
fun MainContent(viewModel: MinifluxViewModel, sharedPreferences: android.content.SharedPreferences) {
    if (viewModel.authToken.value == null) {
        Log.d("MainContent", "Displaying LoginScreen")
        LoginScreen(viewModel) { token ->
            Log.d("MainContent", "Login successful, token saved")
            sharedPreferences.edit().putString("auth_token", token).apply()
            viewModel.fetchFeeds()
        }
    } else {
        Log.d("MainContent", "Displaying FeedListScreen")
        FeedListScreen(viewModel)
    }
}
