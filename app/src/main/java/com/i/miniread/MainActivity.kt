package com.i.miniread

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.i.miniread.ui.ArticleDetailScreen
import com.i.miniread.ui.CategoryListScreen
import com.i.miniread.ui.FeedListScreen
import com.i.miniread.ui.LoginScreen
import com.i.miniread.viewmodel.MinifluxViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MinifluxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("miniread_prefs", Context.MODE_PRIVATE)
        val savedToken = sharedPreferences.getString("auth_token", null)
        if (savedToken != null) {
            viewModel.setAuthToken(savedToken)
            viewModel.fetchFeeds()
        }

        setContent {
            MainContent(viewModel, sharedPreferences)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(viewModel: MinifluxViewModel, sharedPreferences: android.content.SharedPreferences) {
    val authToken by viewModel.authToken.observeAsState()
    val navController = rememberNavController()
    val screens = listOf("Feeds", "Categories", "Account")

    if (authToken == null) {
        LoginScreen(viewModel) { token ->
            try {
                Log.d("MainContent", "Saving token: $token")
                sharedPreferences.edit().putString("auth_token", token).apply()
                viewModel.setAuthToken(token)
            } catch (e: Exception) {
                Log.e("MainContent", "Error saving token", e)
            }
        }
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(id = R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            // Handle menu click (e.g., open a drawer if needed)
                        }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                        }
                    }
                )
            },
            content = { innerPadding ->
                Surface(modifier = Modifier.padding(innerPadding)) {
                    NavHost(navController = navController, startDestination = "feeds") {
                        composable("feeds") { FeedListScreen(viewModel, navController) }
                        composable("categories") { CategoryListScreen(viewModel) }
                        composable("articleDetail") { ArticleDetailScreen(viewModel) }
                        // Add more composable routes as needed
                    }
                }
            }
        )
    }
}
