package com.i.miniread.ui.adaptive

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.i.miniread.BuildConfig
import com.i.miniread.Screen

/**
 * 平板横屏专用侧边导航栏
 * Material Design 3 NavigationRail 组件
 */
@Composable
fun AppNavigationRail(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val items = listOf(
        NavigationItem(Screen.Feeds.route, "Feeds", Icons.Default.List),
        NavigationItem(Screen.TodayEntryList.route, "Today", Icons.Default.DateRange),
        NavigationItem(Screen.Categories.route, "Categories", Icons.Default.Menu)
    )

    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = if (BuildConfig.IS_EINK)
            androidx.compose.ui.graphics.Color.White
        else
            MaterialTheme.colorScheme.surface
    ) {
        items.forEach { item ->
            NavigationRailItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = if (BuildConfig.IS_EINK)
                            Modifier.size(20.dp)
                        else
                            Modifier
                    )
                },
                label = {
                    if (!BuildConfig.IS_EINK) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            )
        }
    }
}

private data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

