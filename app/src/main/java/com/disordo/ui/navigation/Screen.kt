package com.disordo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    object Home : Screen("home", Icons.Default.Home, "Ana Sayfa")
    object Camera : Screen("camera", Icons.Default.CameraAlt, "Kamera")
    object AR : Screen("ar", Icons.Default.ViewInAr, "AR")
    object Profile : Screen("profile", Icons.Default.AccountCircle, "Profil")
}
