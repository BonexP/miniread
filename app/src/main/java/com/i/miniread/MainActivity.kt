package com.i.miniread

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.i.miniread.ui.ArticleDetailScreen
import com.i.miniread.ui.CategoryListScreen
import com.i.miniread.ui.EntryListScreen
import com.i.miniread.ui.FeedListScreen
import com.i.miniread.ui.LoginScreen
import com.i.miniread.ui.TodayEntryListScreen
import com.i.miniread.ui.theme.MinireadTheme
import com.i.miniread.viewmodel.MinifluxViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MinifluxViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
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

sealed class Screen(val route: String, val label: String) {
    data object Feeds : Screen("feeds", "Feeds")
    data object Categories : Screen("categories", "Categories")
    data object EntryList : Screen("entryList", "Entry List")
    data object ArticleDetail : Screen("articleDetail", "Article Detail")
    data object TodayEntryList : Screen("todayEntryList", "Today")
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    viewModel: MinifluxViewModel,
    sharedPreferences: android.content.SharedPreferences
) {
    val authToken by viewModel.authToken.observeAsState()
    val navController = rememberNavController()
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
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // 判断是否是 ArticleDetailScreen 的路由
        val shouldShowBottomBar = currentRoute?.startsWith(Screen.ArticleDetail.route) == false

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleMedium // Use a smaller style
                        )
                    },
                    actions = {
                        IconButton(onClick = {
                            Log.d("IconButton", "Refresh data")
                            viewModel.fetchFeeds()
                            viewModel.fetchCategories()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.height(40.dp),

                    )
            },
            bottomBar = {
//                BottomNavigationBar(navController = navController)
                if (shouldShowBottomBar) {
                    BottomNavigationBar(navController = navController)
                }

            }
        ) { innerPadding ->
            Surface(modifier = Modifier.padding(innerPadding)) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.TodayEntryList.route
                ) {
                    composable(Screen.Feeds.route) {
                        FeedListScreen(viewModel) { feedId ->
                            currentFeedId = feedId
                            currentCategoryId = null
                            navController.navigate(Screen.EntryList.route + "?feedId=$feedId")
                        }
                    }
                    composable(Screen.Categories.route) {
                        CategoryListScreen(viewModel) { categoryId ->
                            currentCategoryId = categoryId
                            currentFeedId = null
                            navController.navigate(Screen.EntryList.route + "?categoryId=$categoryId")
                        }
                    }
                    composable(
                        route = Screen.EntryList.route + "?feedId={feedId}&categoryId={categoryId}",
                        arguments = listOf(
                            navArgument("feedId") { nullable = true; type = NavType.StringType },
                            navArgument("categoryId") { nullable = true; type = NavType.StringType }

                        )) { backStackEntry ->
                        currentFeedId = backStackEntry.arguments?.getInt("feedId")
                        val feedId = backStackEntry.arguments?.getString("feedId")?.toIntOrNull()
                        val categoryId =
                            backStackEntry.arguments?.getString("categoryId")?.toIntOrNull()
                        EntryListScreen(viewModel, navController, feedId, categoryId)
                    }
                    composable(
                        route = Screen.ArticleDetail.route + "?entryId={entryId}",
                        arguments = listOf(navArgument("entryId") { type = NavType.IntType })
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
                    composable(Screen.TodayEntryList.route) {
                        TodayEntryListScreen(viewModel, navController)
                    }

                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar(
        modifier = Modifier.height(48.dp), // 使导航栏高度更扁
        containerColor = MaterialTheme.colorScheme.surface

    ) {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        val items = listOf(Screen.Feeds, Screen.TodayEntryList, Screen.Categories)

        items.forEach { screen ->
            NavigationBarItem(
//                label = {
//                    Text(
//                        text = screen.label,
//                        fontSize = 10.sp, // 调整字体大小，确保不会与图标重叠
//                        style = MaterialTheme.typography.labelSmall, // 使用较小字体
//
//                    )
//                },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {

                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = screen.label, // 有意义的描述
                        modifier = Modifier.size(20.dp), // 调整图标大小，使其更小
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    }
}


