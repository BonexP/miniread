package com.i.miniread

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
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
import com.i.miniread.ui.adaptive.AdaptiveLayoutHelper
import com.i.miniread.ui.adaptive.AppNavigationRail
import com.i.miniread.ui.theme.MinireadTheme
import com.i.miniread.util.DataStoreManager
import com.i.miniread.viewmodel.MinifluxViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MinifluxViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            DataStoreManager.init(this) // 初始化 DataStore
            Log.d("MainActivity", "DataStoreManager initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing DataStoreManager", e)
        }

        // 使用协程读取存储的数据
        lifecycleScope.launch {
            try {
                val savedBaseUrl = DataStoreManager.getBaseUrl()
                val savedAuthToken = DataStoreManager.getApiToken()
                Log.d("MainActivity", "Loaded: baseUrl=$savedBaseUrl, token=${if(savedAuthToken.isNotEmpty()) "exists" else "empty"}")

                if (savedBaseUrl.isNotEmpty() && savedAuthToken.isNotEmpty()) {
                    // 如果存在已保存的 baseUrl 和 authToken，则初始化 Retrofit 和 ViewModel
                    RetrofitInstance.initialize(savedBaseUrl)
                    viewModel.setAuthToken(savedAuthToken)
                    viewModel.fetchFeeds()
                    viewModel.fetchCategories()
                    viewModel.fetchUserInfo()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading saved credentials", e)
            }
        }

        setContent {
            MinireadTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                MainContent(viewModel = viewModel, windowSizeClass = windowSizeClass.widthSizeClass)
            }
        }
    }

    /**
     * 处理按键事件 - E-Ink 版本专用功能
     * 
     * ⚠️ 注意事项：
     * - 此功能仅在 E-Ink 版本中启用
     * - 需要 WebView 获得焦点才能工作
     * - 如果焦点不在 WebView 上，音量键将保持默认行为
     * - 建议在设置中提供开关选项让用户选择是否启用此功能
     * 
     * 实现细节：
     * - 音量上键：向上滚动 500px
     * - 音量下键：向下滚动 800px（考虑阅读习惯，向下滚动距离更大）
     * - 其他按键：保持默认行为
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 仅在 E-Ink 版本启用音量键翻页
        if (BuildConfig.IS_EINK) {
            Log.d("MainActivity", "Key pressed: $keyCode")

            return when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    // 尝试获取当前焦点的 WebView
                    val webView = currentFocus as? WebView
                    if (webView != null) {
                        // WebView 有焦点，执行向上滚动
                        webView.scrollBy(0, -500)
                        Log.d("MainActivity", "Volume up - scrolled up 500px")
                        true  // 消费此事件，不再传递
                    } else {
                        // WebView 没有焦点，记录警告并保持默认行为
                        Log.w("MainActivity", "Volume up pressed but WebView not focused")
                        super.onKeyDown(keyCode, event)
                    }
                }

                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    // 尝试获取当前焦点的 WebView
                    val webView = currentFocus as? WebView
                    if (webView != null) {
                        // WebView 有焦点，执行向下滚动
                        webView.scrollBy(0, 800)
                        Log.d("MainActivity", "Volume down - scrolled down 800px")
                        true  // 消费此事件，不再传递
                    } else {
                        // WebView 没有焦点，记录警告并保持默认行为
                        Log.w("MainActivity", "Volume down pressed but WebView not focused")
                        super.onKeyDown(keyCode, event)
                    }
                }

                else -> {
                    // 其他按键保持默认行为
                    super.onKeyDown(keyCode, event)
                }
            }
        }

        // 标准版本保持默认按键行为
        return super.onKeyDown(keyCode, event)
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
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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

        // 判断是否应该使用侧边导航栏（平板横屏模式）
        val useNavigationRail = AdaptiveLayoutHelper.shouldUseNavigationRail(windowSizeClass)

        LaunchedEffect(windowSizeClass) {
            Log.d("MainActivity", "WindowSizeClass changed: $windowSizeClass")
            Log.d("MainActivity", "Should use NavigationRail: $useNavigationRail")
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(

                    title = {
                        Text(
                            stringResource(id = R.string.app_name),
                            // E-Ink 版本使用更小的字体
                            style = if (BuildConfig.IS_EINK)
                                androidx.compose.material3.MaterialTheme.typography.titleMedium
                            else
                                androidx.compose.material3.MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(onClick = {
                            Log.d(
                                "IconButton",
                                "Refresh button clicked, current route: $currentRoute"
                            )

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
                                    val categoryId =
                                        navBackStackEntry?.arguments?.getInt("categoryId")
                                    Log.d(
                                        "RefreshAction",
                                        "Refreshing SubFeed for category: $categoryId"
                                    )
                                    categoryId?.let {
                                        viewModel.fetchCategoryFeeds(it)
                                        viewModel.fetchFeedsUnreadCount()
                                    }
                                }

                                // EntryList 页面：根据 feedId 或 categoryId 刷新条目列表
                                currentRoute?.startsWith(Screen.EntryList.route) == true -> {
                                    val feedId = navBackStackEntry?.arguments?.getString("feedId")
                                        ?.toIntOrNull()
                                    val categoryId =
                                        navBackStackEntry?.arguments?.getString("categoryId")
                                            ?.toIntOrNull()

                                    when {
                                        feedId != null -> {
                                            Log.d(
                                                "RefreshAction",
                                                "Refreshing entries for feed: $feedId"
                                            )
                                            viewModel.refreshEntriesByFeed(feedId)
                                        }

                                        categoryId != null -> {
                                            Log.d(
                                                "RefreshAction",
                                                "Refreshing entries for category: $categoryId"
                                            )
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
                                    Log.d(
                                        "RefreshAction",
                                        "No refresh action for route: $currentRoute"
                                    )
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新",
                                // E-Ink 版本使用更小的图标
                                modifier = if (BuildConfig.IS_EINK)
                                    Modifier.size(18.dp)
                                else
                                    Modifier
                            )
                        }
                    },
                    // E-Ink 版本使用更小的高度
                    modifier = if (BuildConfig.IS_EINK) {
                        Modifier.height(40.dp)
//                        Modifier.padding(paddingValues = PaddingValues(bottom = 0.dp))
//                        Modifier.padding(horizontal = 0.dp)

                    }
                    else
                        Modifier,
                )

            },
            bottomBar = {
                // 只在手机模式下显示底部导航栏
                if (shouldShowBottomBar && !useNavigationRail) {
                    BottomNavigationBar(navController = navController)
                }
            },


        ) { innerPadding ->
            // 平板横屏模式：使用侧边导航栏 + 内容区域
            if (useNavigationRail) {
                Row(modifier = Modifier.padding(innerPadding)) {
                    AppNavigationRail(navController = navController)
                    Surface(modifier = Modifier.weight(1f)) {
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
                                    navArgument("feedId") {
                                        type = NavType.StringType; nullable = true
                                    },
                                    navArgument("categoryId") {
                                        type = NavType.StringType; nullable = true
                                    }
                                )) { backStackEntry ->
                                currentFeedId =
                                    backStackEntry.arguments?.getString("feedId").toString()
                                val feedId =
                                    backStackEntry.arguments?.getString("feedId")?.toIntOrNull()
                                val categoryId =
                                    backStackEntry.arguments?.getString("categoryId")?.toIntOrNull()
                                EntryListScreen(viewModel, navController, feedId, categoryId)
                            }
                            composable(
                                route = Screen.ArticleDetail.route + "?entryId={entryId}",
                                arguments = listOf(navArgument("entryId") {
                                    type = NavType.IntType
                                })
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
                                })
                            ) { backStackEntry ->
                                val categoryId = backStackEntry.arguments?.getInt("categoryId")
                                categoryId?.let {
                                    SubFeedScreen(
                                        viewModel = viewModel,
                                        categoryId = it,
                                        onFeedSelected = { feedId ->
                                            currentFeedId = feedId.toString()
                                            currentCategoryId = ""
                                            navController.navigate(Screen.EntryList.route + "?feedId=$feedId")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // 手机模式：传统布局
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
                                navArgument("feedId") {
                                    type = NavType.StringType; nullable = true
                                },
                                navArgument("categoryId") {
                                    type = NavType.StringType; nullable = true
                                }
                            )) { backStackEntry ->
                            currentFeedId = backStackEntry.arguments?.getString("feedId").toString()
                            val feedId =
                                backStackEntry.arguments?.getString("feedId")?.toIntOrNull()
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
                                Log.d("MainActivity", "MainContent: Error while getting entryId")
                            }
                        }
                        composable(Screen.TodayEntryList.route) {
                            TodayEntryListScreen(viewModel, navController)
                        }
                        composable(
                            route = "subFeed?categoryId={categoryId}",
                            arguments = listOf(navArgument("categoryId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val categoryId = backStackEntry.arguments?.getInt("categoryId")
                            categoryId?.let {
                                SubFeedScreen(
                                    viewModel = viewModel,
                                    categoryId = it,
                                    onFeedSelected = { feedId ->
                                        currentFeedId = feedId.toString()
                                        currentCategoryId = ""
                                        navController.navigate(Screen.EntryList.route + "?feedId=$feedId")
                                    }
                                )
                            }

                    }
                }
            }
        }
    }
}

/**
 * 底部导航栏
 *
 * E-Ink 版本优化：
 * - 减小高度（48dp vs 80dp）
 * - 移除文本标签以减少刷新区域和视觉复杂度
 * - 使用更小的图标
 * - 使用纯白背景避免残影
 */
@Composable
fun BottomNavigationBar(navController: NavController) {
    // 定义 E-Ink 模式下的高度和修饰符
    val navModifier = if (BuildConfig.IS_EINK) {
        Modifier
            .height(56.dp) // 建议至少 56dp，48dp 对触摸不太友好，且容易切掉图标
            .drawBehind { // E-INK 专用：画一条顶部分割线，增加层次感
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx()
                )
            }
    } else {
        Modifier
    }

    NavigationBar(
        modifier = navModifier,
        // 1. 关键：设置纯白背景
        containerColor = if (BuildConfig.IS_EINK) Color.White else MaterialTheme.colorScheme.surface,
        // 2. 关键：移除 Elevation 带来的灰色遮罩
        tonalElevation = if (BuildConfig.IS_EINK) 0.dp else NavigationBarDefaults.Elevation
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val screens = listOf(Screen.Feeds, Screen.TodayEntryList, Screen.Categories)

        screens.forEach { screen ->
            val isSelected = currentRoute == screen.route
            NavigationBarItem(
                // E-Ink 模式下不显示 label，或者你可以选择始终不显示
                label = if (BuildConfig.IS_EINK) null else {
                    { Text(screen.label) }
                },
                selected = isSelected,
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
                    // Choose icon
                    val iconVector = when(screen) {
                            Screen.Feeds -> Icons.Default.Menu
                            Screen.TodayEntryList -> Icons.Default.DateRange
                            Screen.Categories -> Icons.Default.Menu
                            else -> Icons.Default.Menu
                    }

                    Icon(
                        imageVector = iconVector,// 注意：这里你应该换成 screen.icon
                        contentDescription = screen.label,
                        modifier = if (BuildConfig.IS_EINK) Modifier.size(24.dp) else Modifier,
                        // 注意：这里不需要手动设置 tint，下面 colors 属性会自动处理
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    // --- E-INK 配色核心逻辑 ---

                    // 3. 选中图标：纯黑
                    selectedIconColor = if (BuildConfig.IS_EINK) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer,

                    // 4. 未选中图标：深灰/浅黑（保证对比度）
                    unselectedIconColor = if (BuildConfig.IS_EINK) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant,

                    // 5. 关键优化：将指示器背景设为透明！
                    // 避免墨水屏出现一大块浅灰色的色块，那非常难看且容易留残影
                    indicatorColor = if (BuildConfig.IS_EINK) Color.Transparent else MaterialTheme.colorScheme.secondaryContainer,

                    selectedTextColor = if (BuildConfig.IS_EINK) Color.Black else MaterialTheme.colorScheme.onSurface,
                    unselectedTextColor = if (BuildConfig.IS_EINK) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
