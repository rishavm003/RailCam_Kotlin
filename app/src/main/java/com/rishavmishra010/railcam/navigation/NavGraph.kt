package com.rishavmishra010.railcam.navigation

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rishavmishra010.railcam.ui.screens.CaptureScreen
import com.rishavmishra010.railcam.ui.screens.GalleryScreen
import com.rishavmishra010.railcam.ui.screens.HomeScreen
import com.rishavmishra010.railcam.ui.screens.PreviewScreen

private const val NAV_ANIM_MS = 180

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    LaunchedEffect(currentRoute) {
        val activity = context as? Activity ?: return@LaunchedEffect
        activity.requestedOrientation = if (currentRoute == Screen.Capture.route) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        // Default Navigation-Compose transitions are 700 ms fades, which make every
        // screen change feel sluggish. Short fades keep it smooth but snappy.
        enterTransition = { fadeIn(tween(NAV_ANIM_MS)) },
        exitTransition = { fadeOut(tween(NAV_ANIM_MS)) },
        popEnterTransition = { fadeIn(tween(NAV_ANIM_MS)) },
        popExitTransition = { fadeOut(tween(NAV_ANIM_MS)) }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Capture.route) {
            CaptureScreen(navController = navController)
        }
        composable(Screen.Gallery.route) {
            GalleryScreen(navController = navController)
        }
        composable(Screen.Preview.route) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId") ?: return@composable
            PreviewScreen(
                photoId = photoId,
                navController = navController
            )
        }
    }
}
