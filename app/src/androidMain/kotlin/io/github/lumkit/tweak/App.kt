package io.github.lumkit.tweak

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.local.LocalStorageStore
import io.github.lumkit.tweak.ui.screen.ScreenRoute
import io.github.lumkit.tweak.ui.screen.main.MainScreen
import io.github.lumkit.tweak.ui.screen.protocol.ProtocolScreen
import io.github.lumkit.tweak.ui.screen.runtime.RuntimeModeScreen

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
    val navHostController = LocalScreenNavigationController.current
    val storageStore = LocalStorageStore.current

    val startDestination = if (storageStore.getBoolean(Const.APP_SHARED_PROTOCOL_AGREE_STATE)) {
        ScreenRoute.RUNTIME_MODE
    } else if (storageStore.getBoolean(Const.APP_SHARED_RUNTIME_MODE_STATE)) {
        ScreenRoute.MAIN
    } else {
        ScreenRoute.PROTOCOL
    }

    NavHost(
        navController = navHostController,
        startDestination = ScreenRoute.RUNTIME_MODE,
    ) {
        composable(route = ScreenRoute.PROTOCOL) {
            ProtocolScreen()
        }

        composable(route = ScreenRoute.RUNTIME_MODE) {
            RuntimeModeScreen()
        }

        composable(route = ScreenRoute.MAIN) {
            MainScreen()
        }
    }
}