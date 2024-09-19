package com.i.miniread

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.i.miniread.ui.ArticleDetailScreen
import com.i.miniread.ui.ArticleWebView
import com.i.miniread.ui.CategoryListScreen
import com.i.miniread.ui.EntryListScreen
import com.i.miniread.ui.FeedListScreen
import com.i.miniread.ui.LoginScreen
import com.i.miniread.ui.theme.MinireadTheme
import com.i.miniread.viewmodel.MinifluxViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MinifluxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("miniread_prefs", Context.MODE_PRIVATE)
        val savedToken = sharedPreferences.getString("auth_token", null)
        if (savedToken != null) {
            viewModel.setAuthToken(savedToken)
            viewModel.fetchFeeds()
            viewModel.fetchCategories()
            viewModel.fetchUserInfo()
        }

        setContent {
            MinireadTheme {
                MainContent(viewModel, sharedPreferences)

            }
        }
    }
}

sealed class Screen(val route: String) {
    data object Feeds : Screen("feeds")
    data object Categories : Screen("categories")
    data object EntryList : Screen("entryList")
    data object ArticleDetail : Screen("articleDetail")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    viewModel: MinifluxViewModel,
    sharedPreferences: android.content.SharedPreferences
) {
    val authToken by viewModel.authToken.observeAsState()
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val screens = listOf(Screen.Feeds, Screen.Categories)
    var selectedScreen by remember { mutableStateOf(Screen.Feeds.route) }
    var currentFeedId by remember { mutableStateOf<Int?>(null) }
    var currentCategoryId by remember { mutableStateOf<Int?>(null) }

    if (authToken == null) {
        LoginScreen(viewModel) { token ->
            try {
                sharedPreferences.edit().putString("auth_token", token).apply()
                viewModel.setAuthToken(token)
            } catch (e: Exception) {
                Log.e("MainContent", "Error saving token", e)
            }
        }
    } else {
        val isArticleDetailScreen = selectedScreen == Screen.ArticleDetail.route

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !isArticleDetailScreen, // 禁用文章详情页的滑动手势
            drawerContent = {
                ModalDrawerSheet {
                    screens.forEach { screen ->
                        Text(
                            text = screen.route.capitalize(),
                            modifier = Modifier
                                .clickable {
                                    selectedScreen = screen.route
                                    scope.launch { drawerState.close() }
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                .padding(16.dp)
                        )
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(stringResource(id = R.string.app_name)) },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                Log.d("IconButton", "MainContent: ${navController.currentDestination?.route}")
                                val currentRoute = navController.currentDestination?.route
                                when {
                                    currentRoute?.startsWith(Screen.EntryList.route) == true -> {
                                        currentFeedId?.let { viewModel.refreshEntriesByFeed(it) }
                                        currentCategoryId?.let {
                                            Log.d("IconButton", "MainContent: refreshEntriesByCategory $it")
                                            viewModel.refreshEntriesByCategory(it)
                                        }
                                    }
                                    currentRoute == Screen.Feeds.route -> viewModel.refreshFeeds()
                                    currentRoute == Screen.Categories.route -> viewModel.refreshCategories()
                                    else -> {} // Handle other screens if needed
                                }
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }

                        }
                    )
                },
                content = { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding)) {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Categories.route
                        ) {
                            composable(Screen.Feeds.route) {
                                FeedListScreen(viewModel) { feedId ->
                                    currentFeedId = feedId
                                    currentCategoryId = null
                                    navController.navigate(Screen.EntryList.route + "?feedId=$feedId")
                                }
                            }
                            composable(Screen.Categories.route) {
                                CategoryListScreen(viewModel) {
                                        categoryId ->
                                    currentCategoryId = categoryId
                                    currentFeedId = null
                                    navController.navigate(Screen.EntryList.route+"?categoryId=$categoryId")                                }
                            }
                            composable(Screen.EntryList.route+ "?feedId={feedId}&categoryId={categoryId}",
                                arguments = listOf(
                                    navArgument("feedId") { nullable = true; type = NavType.StringType },
                                    navArgument("categoryId") { nullable = true; type = NavType.StringType }
                                )
                            ) {
                                    backStackEntry ->
                                val feedId = backStackEntry.arguments?.getString("feedId")?.toIntOrNull()
                                val categoryId = backStackEntry.arguments?.getString("categoryId")?.toIntOrNull()
                                EntryListScreen(viewModel, navController, feedId, categoryId)                            }
                            composable(
                                route = Screen.ArticleDetail.route + "?entryId={entryId}",
                                arguments = listOf(navArgument("entryId") {
                                    type = NavType.IntType
                                })
                            ) { backStackEntry ->
                                val entryId = backStackEntry.arguments?.getInt("entryId")
                                if (entryId != null) {
                                    selectedScreen = Screen.ArticleDetail.route
                                    ArticleDetailScreen(viewModel, entryId)
                                } else {
                                    Log.d(
                                        "MainActivity",
                                        "MainContent: Error while getting entryId"
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }


    }
}


