package com.i.miniread

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.i.miniread.ui.FeedListScreen
import com.i.miniread.ui.LoginScreen
import com.i.miniread.ui.ArticleDetailScreen
import com.i.miniread.ui.CategoryListScreen
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(viewModel: MinifluxViewModel, sharedPreferences: android.content.SharedPreferences) {
    val authToken by viewModel.authToken.observeAsState()
    val scaffoldState = rememberScaffoldState()
    var selectedScreen by remember { mutableStateOf("feeds") }
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
            scaffoldState = scaffoldState,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scaffoldState.drawerState.open()
                        }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                        }
                    }
                )
            },
            drawerContent = {
                screens.forEach { screen ->
                    ListItem(
                        text = { Text(screen) },
                        modifier = Modifier.clickable {
                            selectedScreen = screen.lowercase()
                            scaffoldState.drawerState.close()
                        }
                    )
                }
            }
        ) {
            when (selectedScreen) {
                "feeds" -> FeedListScreen(viewModel)
                "categories" -> CategoryListScreen(viewModel)
                "account" -> AccountScreen(viewModel)
            }
        }
    }
}
