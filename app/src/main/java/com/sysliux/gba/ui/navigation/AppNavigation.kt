package com.sysliux.gba.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sysliux.gba.ui.screens.EmulatorScreen
import com.sysliux.gba.ui.screens.GameLibraryScreen
import com.sysliux.gba.ui.screens.SettingsScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val LIBRARY = "/"
    const val PLAY = "/play/{romPath}"
    const val SETTINGS = "/settings"

    fun play(romPath: String): String {
        val encoded = URLEncoder.encode(romPath, StandardCharsets.UTF_8.toString())
        return "/play/$encoded"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LIBRARY
    ) {
        composable(Routes.LIBRARY) {
            GameLibraryScreen(
                onNavigateToGame = { romPath ->
                    navController.navigate(Routes.play(romPath))
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }
        composable(
            route = Routes.PLAY,
            arguments = listOf(
                androidx.navigation.navArgument("romPath") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("romPath") ?: ""
            val romPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())
            EmulatorScreen(
                romPath = romPath,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
