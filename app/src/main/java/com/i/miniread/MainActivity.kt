package com.i.miniread

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.i.miniread.network.RetrofitInstance
import com.i.miniread.ui.ArticleDetailScreen
import com.i.miniread.ui.CategoryListScreen
import com.i.miniread.ui.EntryListScreen
import com.i.miniread.ui.FeedListScreen
import com.i.miniread.ui.LoginScreen
import com.i.miniread.ui.SubFeedScreen
import com.i.miniread.ui.TodayEntryListScreen
import com.i.miniread.ui.theme.MinireadTheme
import com.i.miniread.util.PreferenceManager
import com.i.miniread.viewmodel.MinifluxViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MinifluxViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.init(this) // 初始化 SharedPreferences 工具类

        val savedBaseUrl = PreferenceManager.baseUrl
        val savedAuthToken = PreferenceManager.apiToken

        if (savedBaseUrl.isNotEmpty() && savedAuthToken.isNotEmpty()) {
            // 如果存在已保存的 baseUrl 和 authToken，则初始化 Retrofit 和 ViewModel
            RetrofitInstance.initialize(savedBaseUrl)
            viewModel.setAuthToken(savedAuthToken)
            viewModel.fetchFeeds()
            viewModel.fetchCategories()
            viewModel.fetchUserInfo()
        }

        setContent {
            MinireadTheme {
                MainContent(viewModel = viewModel)
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
    data object SubFeedScreen : Screen("subFeed", "Category Feeds")
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    viewModel: MinifluxViewModel,
) {
    val navController = rememberNavController()
    var selectedScreen by remember { mutableStateOf(Screen.Feeds.route) }

    var currentFeedId by remember { mutableStateOf("") }
    var currentCategoryId by remember { mutableStateOf("") }
    var isLoggedIn by remember {
        mutableStateOf(
            PreferenceManager.baseUrl.isNotEmpty() && PreferenceManager.apiToken.isNotEmpty()
        )
    }
    var baseUrl by remember { mutableStateOf(PreferenceManager.baseUrl) }
    var authToken by remember { mutableStateOf(PreferenceManager.apiToken) }

    if (!isLoggedIn) {
        LoginScreen(viewModel = viewModel) { url, token ->
            // 登录成功时保存 baseUrl 和 authToken
            baseUrl = url
            authToken = token
            PreferenceManager.baseUrl = url
            PreferenceManager.apiToken = token
            // 初始化 Retrofit 和 ViewModel
            RetrofitInstance.initialize(baseUrl)
            viewModel.setAuthToken(authToken)
            viewModel.fetchFeeds()
            viewModel.fetchCategories()
            viewModel.fetchUserInfo()

            isLoggedIn = true
        }
    } else {

        RetrofitInstance.initialize(baseUrl)

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // 判断是否是 ArticleDetailScreen 的路由
        val shouldShowBottomBar = currentRoute?.startsWith(Screen.ArticleDetail.route) == false
        val shouldRefreshTodayEntries =
            currentRoute?.startsWith(Screen.TodayEntryList.route) == true
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(id = R.string.app_name)) },
                    actions = {
                        IconButton(onClick = {
                            Log.d("IconButton", "Refresh data")
                            viewModel.fetchFeeds()
                            viewModel.fetchCategories()
                            if (shouldRefreshTodayEntries) {
                                viewModel.fetchTodayEntries()
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        }
                    }
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
                            currentFeedId = feedId.toString()
                            currentCategoryId = ""
                            navController.navigate(Screen.EntryList.route + "?feedId=$feedId")
                        }

                    }
                    composable(Screen.Categories.route) {
                        CategoryListScreen(viewModel = viewModel, { categoryId ->
                            currentCategoryId = categoryId.toString()
                            currentFeedId = ""
                            navController.navigate(Screen.EntryList.route + "?categoryId=$categoryId")
                        }, { categoryId ->
                            navController.navigate(Screen.SubFeedScreen.route + "?categoryId=$categoryId")
                        })
                    }
                    composable(
                        route = Screen.EntryList.route + "?feedId={feedId}&categoryId={categoryId}",
                        arguments = listOf(
                            navArgument("feedId") { type = NavType.StringType; nullable = true },
                            navArgument("categoryId") { type = NavType.StringType; nullable = true }

                        )) { backStackEntry ->
                        currentFeedId = backStackEntry.arguments?.getString("feedId").toString()
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
                    composable(
                        route = "subFeed?categoryId={categoryId}",
                        arguments = listOf(navArgument("categoryId") {
                            type = NavType.IntType
                        }) // 将 categoryId 设置为 Int 类型
                    ) { backStackEntry ->
                        val categoryId =
                            backStackEntry.arguments?.getInt("categoryId") // 直接获取 Int 类型的 categoryId
                        categoryId?.let {
                            SubFeedScreen(
                                viewModel = viewModel,
                                categoryId = it,
                                onFeedSelected = { feedId ->
                                    currentFeedId = feedId.toString()
                                    currentCategoryId = ""
                                    navController.navigate(
                                        Screen.EntryList.route + "?feedId=$feedId"
                                    )
                                }
                            )
                        }
                    }


                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        val items = listOf(Screen.Feeds, Screen.TodayEntryList, Screen.Categories)

        items.forEach { screen ->
            NavigationBarItem(
                label = { Text(screen.label) },
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

                icon = { Icon(imageVector = Icons.Default.Menu, "somthing") }

            )
        }
    }
}



