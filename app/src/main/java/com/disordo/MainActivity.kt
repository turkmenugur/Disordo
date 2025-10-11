package com.disordo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.disordo.ui.components.BottomNavigationBar
import com.disordo.ui.navigation.AppNavigation
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
        val navController = rememberNavController()
        Scaffold(
            bottomBar = { BottomNavigationBar(navController = navController) }
        ) { innerPadding ->
            AppNavigation(navController = navController, paddingValues = innerPadding)
        }
    }
}
