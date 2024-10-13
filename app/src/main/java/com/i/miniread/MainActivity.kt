package com.i.miniread

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.unit.dp
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
import com.i.miniread.util.DataStoreManager
import com.i.miniread.viewmodel.MinifluxViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MinifluxViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataStoreManager.init(this) // 初始化 DataStore

        // 使用协程读取存储的数据
        lifecycleScope.launch {
            val savedBaseUrl = DataStoreManager.getBaseUrl()
            val savedAuthToken = DataStoreManager.getApiToken()

            if (savedBaseUrl.isNotEmpty() && savedAuthToken.isNotEmpty()) {
                // 如果存在已保存的 baseUrl 和 authToken，则初始化 Retrofit 和 ViewModel
                RetrofitInstance.initialize(savedBaseUrl)
                viewModel.setAuthToken(savedAuthToken)
                viewModel.fetchFeeds()
                viewModel.fetchCategories()
                viewModel.fetchUserInfo()
            }
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
    val scope = rememberCoroutineScope()

    var currentFeedId by remember { mutableStateOf("") }
    var currentCategoryId by remember { mutableStateOf("") }
    var isLoggedIn by remember { mutableStateOf(false) }
    var baseUrl by remember { mutableStateOf("") }
    var authToken by remember { mutableStateOf("") }

    // 初始化时读取 DataStore 数据
    LaunchedEffect(Unit) {
        baseUrl = DataStoreManager.getBaseUrl()
        authToken = DataStoreManager.getApiToken()
        isLoggedIn = baseUrl.isNotEmpty() && authToken.isNotEmpty()
    }

    if (!isLoggedIn) {
        LoginScreen(viewModel = viewModel) { url, token ->
            // 登录成功时保存 baseUrl 和 authToken
            baseUrl = url
            authToken = token
            scope.launch {
                DataStoreManager.setBaseUrl(url)
                DataStoreManager.setApiToken(token)
            }
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
                            Log.d("IconButton", "Refresh button clicked, current route: $currentRoute")

                            // 根据当前路由执行不同的刷新逻辑
                            when {
                                // Today 页面：刷新今日条目
                                currentRoute?.startsWith(Screen.TodayEntryList.route) == true -> {
                                    Log.d("RefreshAction", "Refreshing Today entries")
                                    viewModel.fetchTodayEntries()
                                }

                                // Feeds 页面：刷新订阅源列表和未读计数
                                currentRoute?.startsWith(Screen.Feeds.route) == true -> {
                                    Log.d("RefreshAction", "Refreshing Feeds")
                                    viewModel.fetchFeeds()
                                    viewModel.fetchFeedsUnreadCount()
                                }

                                // Categories 页面：刷新分类列表和未读计数
                                currentRoute?.startsWith(Screen.Categories.route) == true -> {
                                    Log.d("RefreshAction", "Refreshing Categories")
                                    // fetchCategoriesUnreadCount 内部会调用 getCategories，
                                    // 所以只调用这一个方法即可同时获取分类和未读计数
                                    viewModel.fetchCategoriesUnreadCount()
                                    viewModel.fetchFeedsUnreadCount()
                                }

                                // SubFeed 页面（分类下的订阅源）：刷新该分类的订阅源
                                currentRoute?.startsWith(Screen.SubFeedScreen.route) == true -> {
                                    val categoryId = navBackStackEntry?.arguments?.getInt("categoryId")
                                    Log.d("RefreshAction", "Refreshing SubFeed for category: $categoryId")
                                    categoryId?.let {
                                        viewModel.fetchCategoryFeeds(it)
                                        viewModel.fetchFeedsUnreadCount()
                                    }
                                }

                                // EntryList 页面：根据 feedId 或 categoryId 刷新条目列表
                                currentRoute?.startsWith(Screen.EntryList.route) == true -> {
                                    val feedId = navBackStackEntry?.arguments?.getString("feedId")?.toIntOrNull()
                                    val categoryId = navBackStackEntry?.arguments?.getString("categoryId")?.toIntOrNull()

                                    when {
                                        feedId != null -> {
                                            Log.d("RefreshAction", "Refreshing entries for feed: $feedId")
                                            viewModel.refreshEntriesByFeed(feedId)
                                        }
                                        categoryId != null -> {
                                            Log.d("RefreshAction", "Refreshing entries for category: $categoryId")
                                            viewModel.refreshEntriesByCategory(categoryId)
                                        }
                                        else -> {
                                            Log.d("RefreshAction", "Refreshing default entries")
                                            viewModel.refreshEntries()
                                        }
                                    }
                                }

                                // ArticleDetail 页面：重新加载文章内容
                                currentRoute?.startsWith(Screen.ArticleDetail.route) == true -> {
                                    val entryId = navBackStackEntry?.arguments?.getInt("entryId")
                                    Log.d("RefreshAction", "Refreshing article: $entryId")
                                    entryId?.let {
                                        viewModel.reloadArticleContent(it)
                                        // TODO: 可以考虑添加以下功能：
                                        // - 清除 WebView 缓存
                                        // - 显示刷新加载动画
                                        // - 刷新后滚动到顶部
                                    }
                                }

                                // 默认情况：不执行任何操作
                                else -> {
                                    Log.d("RefreshAction", "No refresh action for route: $currentRoute")
                                }
                            }
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
                            ArticleDetailScreen(viewModel, entryId, navController)
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
