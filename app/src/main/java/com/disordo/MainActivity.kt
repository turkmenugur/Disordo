package com.disordo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.disordo.ui.components.BottomNavigationBar
import com.disordo.ui.navigation.AppNavigation
import com.disordo.ui.screens.SplashScreen
import com.disordo.ui.theme.DisordoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DisordoApp()
        }
    }
}

@Composable
fun DisordoApp() {
    DisordoTheme {
        var showSplash by remember { mutableStateOf(true) }
        
        if (showSplash) {
            SplashScreen(
                onSplashFinished = { showSplash = false }
            )
        } else {
            val navController = rememberNavController()
            Scaffold(
                bottomBar = { BottomNavigationBar(navController = navController) }
            ) { innerPadding ->
                AppNavigation(navController = navController, paddingValues = innerPadding)
            }
        }
    }
}
