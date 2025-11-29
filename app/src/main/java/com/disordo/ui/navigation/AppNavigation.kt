package com.disordo.ui.navigation

import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.disordo.DisordoApplication
import com.disordo.ml.DetectionResult
import com.disordo.ui.screens.ARScreen
import com.disordo.ui.screens.CameraScreen
import com.disordo.ui.screens.HomeScreen
import com.disordo.ui.screens.ResultsScreen
import com.disordo.ui.screens.SettingsScreen
import com.disordo.viewmodel.CameraViewModel
import com.disordo.viewmodel.HomeViewModel
import com.disordo.viewmodel.ViewModelFactory

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
                },
                onNavigateToResults = { riskScore, bitmap, detections ->
                    // Bitmap ve detections HomeViewModel'de saklanıyor, ResultsScreen'de alınacak
                    navController.navigate(Screen.Results.createRoute(riskScore))
                }
            )
        }
        composable(Screen.Camera.route) {
            CameraScreen(
                onNavigateToResults = { riskScore, bitmap, detections ->
                    // Bitmap ve detections ViewModel'de saklanıyor, ResultsScreen'de alınacak
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
            // ViewModel'den bitmap ve detections'ı al (hem CameraViewModel hem HomeViewModel'den)
            val context = LocalContext.current
            val application = context.applicationContext as? DisordoApplication
            
            val cameraViewModel: CameraViewModel? = if (application != null) {
                viewModel(factory = ViewModelFactory(application))
            } else {
                null
            }
            
            val homeViewModel: HomeViewModel? = if (application != null) {
                viewModel(factory = ViewModelFactory(application))
            } else {
                null
            }
            
            // Önce Application'dan geçici saklanan verileri al
            // Eğer yoksa ViewModel'lerden al
            val tempBitmap = application?.tempAnalyzedBitmap
            val tempDetections = application?.tempDetections ?: emptyList()
            
            // ViewModel'lerden de kontrol et (fallback)
            val cameraBitmap by cameraViewModel?.analyzedBitmap?.collectAsState() 
                ?: remember { mutableStateOf<Bitmap?>(null) }
            val cameraResult by cameraViewModel?.analysisResult?.collectAsState()
                ?: remember { mutableStateOf<com.disordo.ml.DyslexiaResult?>(null) }
            
            val homeBitmap by homeViewModel?.analyzedBitmap?.collectAsState() 
                ?: remember { mutableStateOf<Bitmap?>(null) }
            val homeResult by homeViewModel?.analysisResult?.collectAsState()
                ?: remember { mutableStateOf<com.disordo.ml.DyslexiaResult?>(null) }
            
            // Önce Application'dan, yoksa ViewModel'lerden al
            val analyzedBitmap = tempBitmap ?: (cameraBitmap ?: homeBitmap)
            val analysisResult = if (tempDetections.isNotEmpty()) {
                // Application'dan detections var, DyslexiaResult oluştur
                com.disordo.ml.DyslexiaResult(
                    riskScore = riskScore,
                    confidence = tempDetections.maxOfOrNull { it.score } ?: 0f,
                    isDyslexiaDetected = riskScore > 0.5f,
                    detections = tempDetections
                )
            } else {
                cameraResult ?: homeResult
            }
            
            ResultsScreen(
                riskScore = riskScore,
                bitmap = analyzedBitmap,
                detections = analysisResult?.detections ?: emptyList(),
                onBackToHome = {
                    cameraViewModel?.clearAnalysisResult()
                    homeViewModel?.clearAnalysisResult()
                    application?.clearTempAnalysisData()
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