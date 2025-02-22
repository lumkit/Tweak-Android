package io.github.lumkit.tweak

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
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

val LocalScreenNavigationController = staticCompositionLocalOf<NavHostController> {
    error("LocalScreenNavigationController is not provided.")
}

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope> {
    error("SharedTransitionScope is not provided.")
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun App() {
    SharedTransitionLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val navHostController = rememberNavController()
        CompositionLocalProvider(
            LocalScreenNavigationController provides navHostController,
            LocalSharedTransitionScope provides this@SharedTransitionLayout,
        ) {
            Routing()
        }
    }
}

val LocalAnimateContentScope = compositionLocalOf<AnimatedContentScope> {
    error("AnimatedContentScope is not provided.")
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
            slideInHorizontally(animationSpec = tween(450)) { it }
        },
        popEnterTransition = {
            scaleIn(initialScale = .5f,animationSpec = tween(450)) + fadeIn(animationSpec = tween(450))
        },
        exitTransition = {
            scaleOut(animationSpec = tween(450)) + fadeOut(animationSpec = tween(450))
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = tween(450)) { it }
        }
    ) {
        composable(
            route = ScreenRoute.PROTOCOL,
        ) {
            ProtocolScreen()
        }

        composable(
            route = ScreenRoute.RUNTIME_MODE
        ) {
            RuntimeModeScreen()
        }

        composable(
            route = ScreenRoute.MAIN,
        ) {
            CompositionLocalProvider(
                LocalAnimateContentScope provides this
            ) {
                MainScreen()
            }
        }

        composable(
            route = ScreenRoute.SETTINGS,
        ) {
            CompositionLocalProvider(
                LocalAnimateContentScope provides this
            ) {
                SettingsScreen()
            }
        }

        platesState.forEach { modules ->
            modules.modules.forEach { module ->
                composable(
                    route = module.route,
                    arguments = module.arguments,
                    deepLinks = module.deepLinks
                ) {
                    CompositionLocalProvider(
                        LocalAnimateContentScope provides this
                    ) {
                        module.screen(it)
                    }
                }
            }
        }

        composable(
            route = ScreenRoute.OPEN_SOURCE
        ) {
            CompositionLocalProvider(
                LocalAnimateContentScope provides this
            ) {
                OpenSourceScreen()
            }
        }
    }
}