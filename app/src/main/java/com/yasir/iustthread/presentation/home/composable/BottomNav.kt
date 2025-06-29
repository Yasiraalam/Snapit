package com.yasir.iustthread.presentation.home.composable

import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yasir.iustthread.domain.model.BottomNavItem
import com.yasir.iustthread.navigation.Routes
import com.yasir.iustthread.presentation.addpost.composable.AddThreads
import com.yasir.iustthread.presentation.profile.composable.Profile
import com.yasir.iustthread.ui.theme.PinkColor
import com.yasir.iustthread.utils.NavigationUtils
import com.yasir.iustthread.utils.rememberBottomPadding

@Composable
fun BottomNav(navController: NavHostController) {
    val bottomNavController = rememberNavController()
    val context = LocalContext.current
    val bottomPadding = rememberBottomPadding()

    Scaffold(
        bottomBar = {
            ModernBottomBar(
                navController = bottomNavController,
                bottomPadding = bottomPadding,
                context = context
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = Routes.Home.routes,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = Routes.Home.routes) {
                HomeScreen(navController)
            }
            composable(Routes.Notification.routes) {
                Notification()
            }
            composable(Routes.Search.routes) {
                Search(navController)
            }
            composable(Routes.AddThread.routes) {
                AddThreads(bottomNavController)
            }
            composable(Routes.Profile.routes) {
                Profile(navController)
            }
        }
    }
}

@Composable
fun ModernBottomBar(
    navController: NavHostController,
    bottomPadding: Int,
    context: Context
) {
    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route

    val navItems = listOf(
        BottomNavItem("Home", Routes.Home.routes, Icons.Filled.Home),
        BottomNavItem("Search", Routes.Search.routes, Icons.Filled.Search),
        BottomNavItem("Add", Routes.AddThread.routes, Icons.Filled.Add),
        BottomNavItem("Notifications", Routes.Notification.routes, Icons.Filled.Notifications),
        BottomNavItem("Profile", Routes.Profile.routes, Icons.Filled.Person)
    )

    // Calculate dynamic height and padding based on navigation type
    val isGestureNavigation = NavigationUtils.isGestureNavigation(context)

    val bottomBarHeight = if (isGestureNavigation) {
        60.dp
    } else {
        105.dp // Increased height for three-button navigation
    }

    // Only add bottom padding for three-button navigation
    val additionalPadding = if (isGestureNavigation) {
        0.dp
    } else {
        bottomPadding.dp
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(bottomBarHeight)
            .padding(bottom = additionalPadding),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 12.dp), // Increased vertical padding
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEachIndexed { _, item ->
                val isSelected = currentRoute == item.route
                val isAddButton = item.title == "Add"
                val iconSize = if (isAddButton) 34.dp else 28.dp // Increased icon sizes
                val iconTint = when {
                    isAddButton && isSelected -> Color.White
                    isAddButton -> PinkColor
                    isSelected -> PinkColor
                    else -> Color(0xFF666666)
                }
                val bgColor = when {
                    isAddButton && isSelected -> PinkColor
                    isAddButton -> Color.White
                    else -> Color.Transparent
                }
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "scale"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier
                            .size(if (isAddButton) 64.dp else 56.dp) // Increased button sizes
                            .scale(scale)
                            .clip(CircleShape)
                            .background(bgColor)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(iconSize),
                            tint = iconTint
                        )
                    }
                }
            }
        }
    }
}
