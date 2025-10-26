package com.disordo.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.disordo.ui.screens.ARScreen
import com.disordo.ui.screens.CameraScreen
import com.disordo.ui.screens.HomeScreen
import com.disordo.ui.screens.ResultsScreen
import com.disordo.ui.screens.SettingsScreen

@Composable
fun AppNavigation(navController: NavHostController, paddingValues: PaddingValues) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCamera = {
                    navController.navigate(Screen.Camera.route)
                },
                onNavigateToGallery = {
                    navController.navigate(Screen.Camera.route)
                }
            )
        }
        composable(Screen.Camera.route) {
            CameraScreen(
                onNavigateToResults = { riskScore ->
                    navController.navigate(Screen.Results.createRoute(riskScore))
                }
            )
        }
        composable(
            route = Screen.Results.route,
            arguments = listOf(
                navArgument("riskScore") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val riskScore = backStackEntry.arguments?.getFloat("riskScore") ?: 0f
            ResultsScreen(
                riskScore = riskScore,
                onBackToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.AR.route) {
            ARScreen()
        }
        composable(Screen.Profile.route) {
            SettingsScreen()
        }
    }
}