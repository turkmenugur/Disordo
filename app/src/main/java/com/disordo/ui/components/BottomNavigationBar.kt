package com.disordo.ui.components

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.disordo.ui.navigation.Screen
import com.disordo.ui.theme.disordo_coral
import com.disordo.ui.theme.disordo_cream

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        Screen.Home,
        Screen.AR,
        Screen.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Log.d("BottomNav", "Current route in BottomNav: $currentRoute")

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
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
                    
                    // Bottom bar'daki ana ekranlara geçiş
                    if (currentRoute != screen.route) {
                        Log.d("BottomNav", "NAVIGATING to ${screen.route}")
                        
                        // Tek bir navigate işlemi ile güvenli geçiş
                        navController.navigate(screen.route) {
                            // Ana sayfaya (start destination) kadar tüm back stack'i temizle
                            // Camera gibi ara ekranlar da otomatik temizlenir
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Çoklu instance'ları engelle
                            launchSingleTop = true
                            // Ekran state'ini geri yükle
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