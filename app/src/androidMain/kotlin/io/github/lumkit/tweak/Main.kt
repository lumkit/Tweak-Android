package io.github.lumkit.tweak

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import io.github.lumkit.tweak.data.DarkModeState
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.local.CustomColorScheme
import io.github.lumkit.tweak.ui.local.LocalStorageStore
import io.github.lumkit.tweak.ui.local.LocalThemeStore
import io.github.lumkit.tweak.ui.local.Material3
import io.github.lumkit.tweak.ui.local.StorageStore
import io.github.lumkit.tweak.ui.local.ThemeStore
import io.github.lumkit.tweak.ui.local.json
import io.github.lumkit.tweak.ui.local.toColor
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun Main(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val storageStore = remember { StorageStore() }
    val themeStore = remember { ThemeStore() }

    LaunchedEffect(Unit) {
        themeStore.darkModeState = try {
            DarkModeState.valueOf(storageStore.getString(Const.APP_DARK_MODE_STATE)!!)
        } catch (_: Exception) {
            DarkModeState.System
        }
        themeStore.isDynamicColor = storageStore.getBoolean(Const.APP_THEME_DYNAMIC_COLOR)
        val customColorScheme = themeStore.customColorScheme

        customColorScheme.isCustomColorScheme = if (!themeStore.isDynamicColor) {
            storageStore.getBoolean(Const.APP_THEME_CUSTOM_COLOR_SCHEME)
        } else {
            false
        }

        try {
            // 自定义主题颜色
            val installFile = CustomColorScheme.CUSTOM_MATERIAL3_JSON_INSTALL_File
            if (!installFile.exists()) {
                context.assets.open("material-theme.json").use {  fis ->
                    installFile.outputStream().use {  fos ->
                        fis.copyTo(fos)
                    }
                }
            }
            val m3 = installFile.inputStream().use {
                json.decodeFromString<Material3>(String(it.readBytes()))
            }
            customColorScheme.lightColorScheme = m3.schemes.light.toColorScheme()
            customColorScheme.darkColorScheme = m3.schemes.dark.toColorScheme()
            customColorScheme.material3 = m3
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(themeStore) {
        snapshotFlow {
            themeStore.darkModeState
        }.distinctUntilChanged()
            .collect {
                storageStore.putString(Const.APP_DARK_MODE_STATE, it.name)
            }
    }

    LaunchedEffect(themeStore) {
        snapshotFlow {
            themeStore.isDynamicColor
        }.distinctUntilChanged()
            .collect {
                storageStore.putBoolean(Const.APP_THEME_DYNAMIC_COLOR, it)
                if (it) {
                    themeStore.customColorScheme.isCustomColorScheme = false
                }
            }
    }

    LaunchedEffect(themeStore) {
        snapshotFlow {
            themeStore.customColorScheme.isCustomColorScheme
        }.distinctUntilChanged()
            .collect {
                storageStore.putBoolean(Const.APP_THEME_CUSTOM_COLOR_SCHEME, it)
                if (it) {
                    themeStore.isDynamicColor = false
                }
            }
    }

    CompositionLocalProvider(
        LocalStorageStore provides storageStore,
        LocalThemeStore provides themeStore,
        content = content,
    )
}