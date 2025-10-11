package com.disordo.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.disordo.TextRecognitionScreen
import com.disordo.ui.screens.CameraScreen
import com.disordo.ui.screens.HomeScreen
import com.disordo.ui.screens.SettingsScreen

@Composable
fun AppNavigation(navController: NavHostController, paddingValues: PaddingValues) {
    NavHost(
        navController = navController, 
        startDestination = Screen.Home.route,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(Screen.Home.route) {
            HomeScreen()
        }
        composable(Screen.Camera.route) {
            CameraScreen()
        }
        composable(Screen.AR.route) {
            TextRecognitionScreen()
        }
        composable(Screen.Profile.route) {
            SettingsScreen()
        }
    }
}
