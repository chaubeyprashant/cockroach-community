package com.cockroachcommunity.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cockroachcommunity.app.core.designsystem.theme.Gold
import com.cockroachcommunity.app.core.designsystem.theme.Ink
import com.cockroachcommunity.app.feature.blog.ArticleDetailScreen
import com.cockroachcommunity.app.feature.blog.BlogScreen
import com.cockroachcommunity.app.feature.complaints.ComplaintDetailScreen
import com.cockroachcommunity.app.feature.complaints.ComplaintListScreen
import com.cockroachcommunity.app.feature.complaints.ComplaintWizardScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cockroachcommunity.app.feature.home.HomeScreen
import com.cockroachcommunity.app.feature.notifications.NotificationsScreen
import com.cockroachcommunity.app.feature.notifications.NotificationsViewModel
import com.cockroachcommunity.app.feature.placeholder.PlaceholderScreen
import com.cockroachcommunity.app.feature.profile.ProfileScreen
import com.cockroachcommunity.app.feature.protest.ProtestScreen

sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Tab("home", "Home", Icons.Default.Home)
    data object Complaints : Tab("complaints", "Complaints", Icons.Default.Report)
    data object Community : Tab("community", "Community", Icons.Default.Forum)
    data object Blog : Tab("blog", "Blog", Icons.Default.Article)
    data object Profile : Tab("profile", "Profile", Icons.Default.Person)
}

object Routes {
    const val PROTEST = "protest"
    const val NOTIFICATIONS = "notifications"
    const val ARTICLE = "blog/{id}"
    const val COMPLAINT = "complaint/{id}"
    const val COMPLAINT_NEW = "complaint_new?police={police}"
    fun complaint(id: String) = "complaint/$id"
    fun complaintNew(police: Boolean) = "complaint_new?police=$police"
    fun article(id: String) = "blog/$id"
}

private val tabs = listOf(Tab.Home, Tab.Complaints, Tab.Community, Tab.Blog, Tab.Profile)

private fun NavHostController.switchTab(tab: Tab) = navigate(tab.route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun MainNavigation(notificationsViewModel: NotificationsViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route.orEmpty()
    val borderColor = MaterialTheme.colorScheme.outline
    val unread by notificationsViewModel.unreadCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .drawBehind {
                        drawLine(borderColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 4f)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🪳 COCKROACH JANTA PARTY",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { navController.navigate(Routes.NOTIFICATIONS) { launchSingleTop = true } }) {
                    BadgedBox(badge = { if (unread > 0) Badge { Text(unread.toString()) } }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.drawBehind {
                    drawLine(borderColor, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 4f)
                }
            ) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route || currentRoute.startsWith(tab.route + "/") ||
                            (tab == Tab.Complaints && currentRoute.startsWith("complaint")),
                        onClick = { navController.switchTab(tab) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label.uppercase(), maxLines = 1, softWrap = false, fontSize = 9.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Gold,
                            selectedIconColor = Ink,
                            selectedTextColor = MaterialTheme.colorScheme.onBackground,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Tab.Home.route) {
                HomeScreen(
                    onJoinProtest = { navController.navigate(Routes.PROTEST) },
                    onReportPolice = { navController.navigate(Routes.complaintNew(true)) }
                )
            }
            composable(Routes.PROTEST) { ProtestScreen(onReportPolice = { navController.navigate(Routes.complaintNew(true)) }) }
            composable(Tab.Complaints.route) {
                ComplaintListScreen(
                    onOpen = { navController.navigate(Routes.complaint(it)) },
                    onNew = { navController.navigate(Routes.complaintNew(false)) }
                )
            }
            composable(Routes.COMPLAINT) { ComplaintDetailScreen() }
            composable(
                Routes.COMPLAINT_NEW,
                arguments = listOf(navArgument("police") { type = NavType.BoolType; defaultValue = false })
            ) { entry ->
                ComplaintWizardScreen(
                    policeFocus = entry.arguments?.getBoolean("police") ?: false,
                    onDone = { navController.popBackStack() }
                )
            }
            composable(Tab.Community.route) { PlaceholderScreen("Community") }
            composable(Tab.Blog.route) { BlogScreen(onOpen = { navController.navigate(Routes.article(it)) }) }
            composable(Routes.ARTICLE) { ArticleDetailScreen() }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(onOpen = { n ->
                    // Website hrefs look like /complaints/{id}
                    val id = n.complaintId ?: n.href.substringAfter("/complaints/", "").takeIf { it.isNotBlank() }
                    if (id != null) navController.navigate(Routes.complaint(id))
                })
            }
            composable(Tab.Profile.route) { ProfileScreen() }
        }
    }
}
