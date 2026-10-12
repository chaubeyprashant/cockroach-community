package com.cockroachcommunity.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cockroachcommunity.app.ui.main.MainScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel

sealed class Screen(
    val route: String, 
    val label: String? = null, 
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    object Auth : Screen("auth")
    object Chats : Screen("chats", "Chats", Icons.AutoMirrored.Filled.Message, Icons.AutoMirrored.Outlined.Message)
    object Calls : Screen("calls", "Calls", Icons.Filled.Call, Icons.Outlined.Call)
    object Nearby : Screen("nearby", "Nearby", Icons.Filled.Person, Icons.Outlined.Person)
    object Transfers : Screen("transfers", "Transfers", Icons.Filled.Share, Icons.Outlined.Share)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    
    object ChatDetail : Screen("chat/{peerId}") {
        fun createRoute(peerId: String) = "chat/$peerId"
    }
    
    object ContactDetails : Screen("contact_details/{peerId}") {
        fun createRoute(peerId: String) = "contact_details/$peerId"
    }
}

// Ensure Chats, Calls, Nearby, Settings are top level. We'll keep Transfers so as not to delete existing features.
val items = listOf(
    Screen.Chats,
    Screen.Calls,
    Screen.Nearby,
    Screen.Transfers,
    Screen.Settings
)

@Composable
fun MainNavigation(
    isSignedIn: Boolean = true,
    callViewModel: com.cockroachcommunity.app.feature.call.CallViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val callState by callViewModel.callState.collectAsStateWithLifecycle()
    
    androidx.compose.runtime.LaunchedEffect(callState) {
        if (callState == com.cockroachcommunity.app.core.network.repository.CallState.INCOMING) {
            val currentRoute = navController.currentDestination?.route
            // Navigate to Calls screen but stripping out query parameters in our comparison
            if (currentRoute?.startsWith(Screen.Calls.route) != true) {
                navController.navigate(Screen.Calls.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(isSignedIn) {
        if (!isSignedIn) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != Screen.Auth.route) {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute == Screen.Auth.route) {
                navController.navigate(Screen.Chats.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            }
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Only show bottom bar on top-level destinations
            val currentRouteBase = currentDestination?.route?.substringBefore("?")
            val isTopLevel = items.any { it.route == currentRouteBase } && currentRouteBase != Screen.Auth.route
            
            if (isTopLevel) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 6.dp,
                        shadowElevation = 12.dp,
                        border = BorderStroke(
                            1.dp, 
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items.forEach { screen ->
                                val selected = currentRouteBase == screen.route
                                val interactionSource = remember { MutableInteractionSource() }
                                
                                val activeBg = if (selected) {
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            Color(0xFF8B5CF6)
                                        )
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent, 
                                            Color.Transparent
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(activeBg)
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null
                                        ) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                        .padding(
                                            horizontal = if (selected) 14.dp else 10.dp,
                                            vertical = 10.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    val icon = if (selected) screen.selectedIcon else screen.unselectedIcon
                                    if (icon != null) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = screen.label,
                                            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    if (selected && screen.label != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = screen.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        val startDest = if (isSignedIn) Screen.Chats.route else Screen.Auth.route
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Auth.route) {
                com.cockroachcommunity.app.feature.auth.AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(Screen.Chats.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Chats.route) {
                MainScreen(
                    onItemClick = { peerId -> 
                        navController.navigate(Screen.ChatDetail.createRoute(peerId))
                    },
                    onFabClick = {
                        navController.navigate(Screen.Nearby.route)
                    }
                )
            }
            composable(Screen.Nearby.route) {
                com.cockroachcommunity.app.feature.nearby.NearbyScreen(
                    onDeviceConnected = { device -> 
                        navController.navigate(Screen.ChatDetail.createRoute(device.address))
                    }
                )
            }
            
            composable(
                route = Screen.ChatDetail.route,
                arguments = listOf(androidx.navigation.navArgument("peerId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val peerId = backStackEntry.arguments?.getString("peerId") ?: return@composable
                com.cockroachcommunity.app.feature.chat.ChatScreen(
                    peerId = peerId,
                    onNavigateToContactDetails = {
                        navController.navigate(Screen.ContactDetails.createRoute(it))
                    },
                    onNavigateToCall = { isVideoCall ->
                        navController.navigate("${Screen.Calls.route}?autoStartCall=true&isVideoCall=$isVideoCall")
                    }
                )
            }
            composable(
                route = "${Screen.Calls.route}?autoStartCall={autoStartCall}&isVideoCall={isVideoCall}",
                arguments = listOf(
                    androidx.navigation.navArgument("autoStartCall") { 
                        type = androidx.navigation.NavType.BoolType
                        defaultValue = false 
                    },
                    androidx.navigation.navArgument("isVideoCall") {
                        type = androidx.navigation.NavType.BoolType
                        defaultValue = true
                    }
                )
            ) { backStackEntry ->
                val autoStartCall = backStackEntry.arguments?.getBoolean("autoStartCall") ?: false
                val isVideoCall = backStackEntry.arguments?.getBoolean("isVideoCall") ?: true
                com.cockroachcommunity.app.feature.call.CallScreen(autoStartCall = autoStartCall, isVideoCall = isVideoCall)
            }
            composable(Screen.Transfers.route) {
                com.cockroachcommunity.app.feature.transfers.TransfersScreen()
            }
            composable(Screen.Settings.route) {
                com.cockroachcommunity.app.feature.settings.SettingsScreen(
                    onSignOut = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Screen.ContactDetails.route,
                arguments = listOf(androidx.navigation.navArgument("peerId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val peerId = backStackEntry.arguments?.getString("peerId") ?: return@composable
                com.cockroachcommunity.app.feature.contact.ContactDetailsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { id -> 
                        navController.navigate(Screen.ChatDetail.createRoute(id)) {
                            popUpTo(Screen.Chats.route) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
