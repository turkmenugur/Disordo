package com.disordo.ui.components

import android.util.Log
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.xr.compose.material3.ExperimentalMaterial3XrApi
import androidx.xr.compose.material3.NavigationBar
import com.disordo.ui.navigation.Screen
import com.disordo.ui.theme.disordo_coral
import com.disordo.ui.theme.disordo_cream

@OptIn(ExperimentalMaterial3XrApi::class)
@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        Screen.Home,
        Screen.Camera,
        Screen.AR,
        Screen.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Log.d("BottomNav", "Current route in BottomNav: $currentRoute")

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label
                    )
                },
                label = { Text(screen.label) },
                selected = currentRoute == screen.route,
                onClick = {
                    Log.d("BottomNav", "=== CLICKED: ${screen.route} ===")
                    Log.d("BottomNav", "Current route: $currentRoute")
                    Log.d("BottomNav", "Are they equal? ${currentRoute == screen.route}")

                    // Aynı ekranda değilse navigate et
                    if (currentRoute != screen.route) {
                        Log.d("BottomNav", "NAVIGATING to ${screen.route}")
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        Log.d("BottomNav", "SKIPPED - Already on ${screen.route}")
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = disordo_coral,
                    selectedTextColor = disordo_coral,
                    indicatorColor = disordo_coral.copy(alpha = 0.2f),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}