package com.rishavmishra010.railcam.navigation

sealed class Screen(val route: String) {
    object Home    : Screen("home")
    object Capture : Screen("capture")
    object Gallery : Screen("gallery")
    object Preview : Screen("preview/{photoId}") {
        fun createRoute(photoId: String) = "preview/$photoId"
    }
}
