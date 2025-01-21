package io.github.lumkit.tweak

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.lumkit.tweak.ui.screen.ScreenRoute
import io.github.lumkit.tweak.ui.screen.main.MainScreen
import io.github.lumkit.tweak.ui.screen.main.page.FunctionPageViewModel
import io.github.lumkit.tweak.ui.screen.protocol.ProtocolScreen
import io.github.lumkit.tweak.ui.screen.runtime.RuntimeModeScreen
import io.github.lumkit.tweak.ui.screen.settings.OpenSourceScreen
import io.github.lumkit.tweak.ui.screen.settings.SettingsScreen

val LocalScreenNavigationController = staticCompositionLocalOf<NavHostController> { error("LocalScreenNavigationController is not provided.") }

@Composable
fun App() {
    val navHostController = rememberNavController()

    CompositionLocalProvider(
        LocalScreenNavigationController provides navHostController,
    ) {
        Routing()
    }
}

@Composable
private fun Routing() {
    val context = LocalContext.current
    val navHostController = LocalScreenNavigationController.current

    val functionPageViewModel = viewModel { FunctionPageViewModel(context) }
    val platesState by functionPageViewModel.plateState.collectAsStateWithLifecycle()

    NavHost(
        navController = navHostController,
        startDestination = ScreenRoute.RUNTIME_MODE,
        enterTransition = {
            slideInHorizontally { it } + fadeIn()
        },
        exitTransition = {
            scaleOut(targetScale = .9f) + fadeOut()
        },
        popEnterTransition = {
            scaleIn(initialScale = .9f) + fadeIn()
        },
        popExitTransition = {
            slideOutHorizontally { it } + fadeOut()
        }
    ) {
        composable(route = ScreenRoute.PROTOCOL) {
            ProtocolScreen()
        }

        composable(route = ScreenRoute.RUNTIME_MODE) {
            RuntimeModeScreen()
        }

        composable(
            route = ScreenRoute.MAIN,
        ) {
            MainScreen()
        }

        composable(
            route = ScreenRoute.SETTINGS,
        ) {
            SettingsScreen()
        }

        platesState.forEach { modules ->
            modules.modules.forEach { module ->
                composable(
                    route = module.route,
                    deepLinks = module.deepLinks
                ) {
                    module.screen()
                }
            }
        }

        composable(
            route = ScreenRoute.OPEN_SOURCE
        ) {
            OpenSourceScreen()
        }
    }
}