package io.github.lumkit.tweak.ui.local

import androidx.compose.material3.ColorScheme
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.data.DarkModeState
import io.github.lumkit.tweak.ui.theme.darkScheme
import io.github.lumkit.tweak.ui.theme.lightScheme
import kotlinx.serialization.json.JsonElement
import java.io.File

val LocalThemeStore =
    staticCompositionLocalOf<ThemeStore> { error("LocalThemeStore is not provided.") }

class ThemeStore {
    // 当前的是否处于深色
    var realDark: Boolean by mutableStateOf(false)

    // 深色模式
    var darkModeState: DarkModeState by mutableStateOf(DarkModeState.System)

    // 是否启用动态取色
    var isDynamicColor: Boolean by mutableStateOf(true)

    // 自定义颜色
    val customColorScheme = CustomColorScheme()
}

class CustomColorScheme {
    companion object {
        val CUSTOM_MATERIAL3_JSON_INSTALL_File: File
            get() {
                val dir = File(TweakApplication.application.filesDir, "theme")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                return File(dir, "material-theme.json")
            }

        val schemeNames = listOf(
            "primary",
            "onPrimary",
            "primaryContainer",
            "onPrimaryContainer",
            "inversePrimary",
            "secondary",
            "onSecondary",
            "secondaryContainer",
            "onSecondaryContainer",
            "tertiary",
            "onTertiary",
            "tertiaryContainer",
            "onTertiaryContainer",
            "background",
            "onBackground",
            "surface",
            "onSurface",
            "surfaceVariant",
            "onSurfaceVariant",
            "surfaceTint",
            "inverseSurface",
            "inverseOnSurface",
            "error",
            "onError",
            "errorContainer",
            "onErrorContainer",
            "outline",
            "outlineVariant",
            "scrim",
            "surfaceBright",
            "surfaceDim",
            "surfaceContainer",
            "surfaceContainerHigh",
            "surfaceContainerHighest",
            "surfaceContainerLow",
            "surfaceContainerLowest",
        )
    }

    var isCustomColorScheme by mutableStateOf(false)
    var lightColorScheme by mutableStateOf(lightScheme)
    var darkColorScheme by mutableStateOf(darkScheme)
}

@Serializable
data class Material3(
    val description: String,
    val seed: String,
    val coreColors: CoreColors,
    val extendedColors: List<JsonElement>,
    val schemes: Schemes,
    val palettes: Palettes
)

@Serializable
data class CoreColors(
    val primary: String
)

@Serializable
data class Schemes(
    val light: Scheme,

    @SerialName("light-medium-contrast")
    val lightMediumContrast: Scheme,

    @SerialName("light-high-contrast")
    val lightHighContrast: Scheme,

    val dark: Scheme,

    @SerialName("dark-medium-contrast")
    val darkMediumContrast: Scheme,

    @SerialName("dark-high-contrast")
    val darkHighContrast: Scheme
)

@Serializable
data class Scheme(
    val primary: String,
    val surfaceTint: String,

    @SerialName("onPrimary")
    val onPrimary: String,

    @SerialName("primaryContainer")
    val primaryContainer: String,

    @SerialName("onPrimaryContainer")
    val onPrimaryContainer: String,

    val secondary: String,

    @SerialName("onSecondary")
    val onSecondary: String,

    @SerialName("secondaryContainer")
    val secondaryContainer: String,

    @SerialName("onSecondaryContainer")
    val onSecondaryContainer: String,

    val tertiary: String,

    @SerialName("onTertiary")
    val onTertiary: String,

    @SerialName("tertiaryContainer")
    val tertiaryContainer: String,

    @SerialName("onTertiaryContainer")
    val onTertiaryContainer: String,

    val error: String,

    @SerialName("onError")
    val onError: String,

    @SerialName("errorContainer")
    val errorContainer: String,

    @SerialName("onErrorContainer")
    val onErrorContainer: String,

    val background: String,

    @SerialName("onBackground")
    val onBackground: String,

    val surface: String,

    @SerialName("onSurface")
    val onSurface: String,

    @SerialName("surfaceVariant")
    val surfaceVariant: String,

    @SerialName("onSurfaceVariant")
    val onSurfaceVariant: String,

    val outline: String,

    @SerialName("outlineVariant")
    val outlineVariant: String,

    val shadow: String,
    val scrim: String,

    @SerialName("inverseSurface")
    val inverseSurface: String,

    @SerialName("inverseOnSurface")
    val inverseOnSurface: String,

    @SerialName("inversePrimary")
    val inversePrimary: String,

    @SerialName("primaryFixed")
    val primaryFixed: String,

    @SerialName("onPrimaryFixed")
    val onPrimaryFixed: String,

    @SerialName("primaryFixedDim")
    val primaryFixedDim: String,

    @SerialName("onPrimaryFixedVariant")
    val onPrimaryFixedVariant: String,

    @SerialName("secondaryFixed")
    val secondaryFixed: String,

    @SerialName("onSecondaryFixed")
    val onSecondaryFixed: String,

    @SerialName("secondaryFixedDim")
    val secondaryFixedDim: String,

    @SerialName("onSecondaryFixedVariant")
    val onSecondaryFixedVariant: String,

    @SerialName("tertiaryFixed")
    val tertiaryFixed: String,

    @SerialName("onTertiaryFixed")
    val onTertiaryFixed: String,

    @SerialName("tertiaryFixedDim")
    val tertiaryFixedDim: String,

    @SerialName("onTertiaryFixedVariant")
    val onTertiaryFixedVariant: String,

    @SerialName("surfaceDim")
    val surfaceDim: String,

    @SerialName("surfaceBright")
    val surfaceBright: String,

    @SerialName("surfaceContainerLowest")
    val surfaceContainerLowest: String,

    @SerialName("surfaceContainerLow")
    val surfaceContainerLow: String,

    @SerialName("surfaceContainer")
    val surfaceContainer: String,

    @SerialName("surfaceContainerHigh")
    val surfaceContainerHigh: String,

    @SerialName("surfaceContainerHighest")
    val surfaceContainerHighest: String
) {
    fun toColorScheme(): ColorScheme = ColorScheme(
        primary = this.primary.toColor(),
        onPrimary = this.onPrimary.toColor(),
        primaryContainer = this.primaryContainer.toColor(),
        onPrimaryContainer = this.onPrimaryContainer.toColor(),
        inversePrimary = this.inversePrimary.toColor(),
        secondary = this.secondary.toColor(),
        onSecondary = this.onSecondary.toColor(),
        secondaryContainer = this.secondaryContainer.toColor(),
        onSecondaryContainer = this.onSecondaryContainer.toColor(),
        tertiary = this.tertiary.toColor(),
        onTertiary = this.onTertiary.toColor(),
        tertiaryContainer = this.tertiaryContainer.toColor(),
        onTertiaryContainer = this.onTertiaryContainer.toColor(),
        background = this.background.toColor(),
        onBackground = this.onBackground.toColor(),
        surface = this.surface.toColor(),
        onSurface = this.onSurface.toColor(),
        surfaceVariant = this.surfaceVariant.toColor(),
        onSurfaceVariant = this.onSurfaceVariant.toColor(),
        surfaceTint = this.surfaceTint.toColor(),
        inverseSurface = this.inverseSurface.toColor(),
        inverseOnSurface = this.inverseOnSurface.toColor(),
        error = this.error.toColor(),
        onError = this.onError.toColor(),
        errorContainer = this.errorContainer.toColor(),
        onErrorContainer = this.onErrorContainer.toColor(),
        outline = this.outline.toColor(),
        outlineVariant = this.outlineVariant.toColor(),
        scrim = this.scrim.toColor(),
        surfaceBright = this.surfaceBright.toColor(),
        surfaceDim = this.surfaceDim.toColor(),
        surfaceContainer = this.surfaceContainer.toColor(),
        surfaceContainerHigh = this.surfaceContainerHigh.toColor(),
        surfaceContainerHighest = this.surfaceContainerHighest.toColor(),
        surfaceContainerLow = this.surfaceContainerLow.toColor(),
        surfaceContainerLowest = this.surfaceContainerLowest.toColor(),
    )
}

fun ColorScheme.toScheme(): Scheme = Scheme(
    primary = this.primary.toHex(),
    surfaceTint = this.surfaceTint.toHex(),
    onPrimary = this.onPrimary.toHex(),
    primaryContainer = this.primaryContainer.toHex(),
    onPrimaryContainer = this.onPrimaryContainer.toHex(),
    secondary = this.secondary.toHex(),
    onSecondary = this.onSecondary.toHex(),
    secondaryContainer = this.secondaryContainer.toHex(),
    onSecondaryContainer = this.onSecondaryContainer.toHex(),
    tertiary = this.tertiary.toHex(),
    onTertiary = this.onTertiary.toHex(),
    tertiaryContainer = this.tertiaryContainer.toHex(),
    onTertiaryContainer = this.onTertiaryContainer.toHex(),
    error = this.error.toHex(),
    onError = this.onError.toHex(),
    errorContainer = this.errorContainer.toHex(),
    onErrorContainer = this.onErrorContainer.toHex(),
    background = this.background.toHex(),
    onBackground = this.onBackground.toHex(),
    surface = this.surface.toHex(),
    onSurface = this.onSurface.toHex(),
    surfaceVariant = this.surfaceVariant.toHex(),
    onSurfaceVariant = this.onSurfaceVariant.toHex(),
    outline = this.outline.toHex(),
    outlineVariant = this.outlineVariant.toHex(),
    shadow = Color.Transparent.toHex(),
    scrim = this.scrim.toHex(),
    inverseSurface = this.inverseSurface.toHex(),
    inverseOnSurface = this.inverseOnSurface.toHex(),
    inversePrimary = this.inversePrimary.toHex(),
    primaryFixed = Color.Transparent.toHex(),
    onPrimaryFixed = Color.Transparent.toHex(),
    primaryFixedDim = Color.Transparent.toHex(),
    onPrimaryFixedVariant = Color.Transparent.toHex(),
    secondaryFixed = Color.Transparent.toHex(),
    onSecondaryFixed = Color.Transparent.toHex(),
    secondaryFixedDim = Color.Transparent.toHex(),
    onSecondaryFixedVariant = Color.Transparent.toHex(),
    tertiaryFixed = Color.Transparent.toHex(),
    onTertiaryFixed = Color.Transparent.toHex(),
    tertiaryFixedDim = Color.Transparent.toHex(),
    onTertiaryFixedVariant = Color.Transparent.toHex(),
    surfaceDim = this.surfaceDim.toHex(),
    surfaceBright = this.surfaceBright.toHex(),
    surfaceContainerLowest = this.surfaceContainerLowest.toHex(),
    surfaceContainerLow = this.surfaceContainerLow.toHex(),
    surfaceContainer = this.surfaceContainer.toHex(),
    surfaceContainerHigh =this.surfaceContainerHigh.toHex(),
    surfaceContainerHighest =this.surfaceContainerHighest.toHex(),
)

@Serializable
data class Palettes(
    val primary: Palette,
    val secondary: Palette,
    val tertiary: Palette,

    @SerialName("neutral")
    val neutral: Palette,

    @SerialName("neutral-variant")
    val neutralVariant: Palette
)

@Serializable
data class Palette(
    @SerialName("0")
    val zero: String,

    @SerialName("5")
    val five: String,

    @SerialName("10")
    val ten: String,

    @SerialName("15")
    val fifteen: String,

    @SerialName("20")
    val twenty: String,

    @SerialName("25")
    val twentyFive: String,

    @SerialName("30")
    val thirty: String,

    @SerialName("35")
    val thirtyFive: String,

    @SerialName("40")
    val forty: String,

    @SerialName("50")
    val fifty: String,

    @SerialName("60")
    val sixty: String,

    @SerialName("70")
    val seventy: String,

    @SerialName("80")
    val eighty: String,

    @SerialName("90")
    val ninety: String,

    @SerialName("95")
    val ninetyFive: String,

    @SerialName("98")
    val ninetyEight: String,

    @SerialName("99")
    val ninetyNine: String,

    @SerialName("100")
    val oneHundred: String
)

/**
 * 将16进制颜色码字符串转换为Color对象的扩展函数。
 *
 * 支持的格式：
 * - RGB: "#RRGGBB" 或 "RRGGBB"
 * - ARGB: "#AARRGGBB" 或 "AARRGGBB"
 *
 * @return 转换后的Color对象
 * @throws IllegalArgumentException 如果颜色字符串格式不正确
 */
fun String.toColor(): Color {
    var colorString = this.trim()

    if (colorString.startsWith("#")) {
        colorString = colorString.substring(1)
    }

    return when (colorString.length) {
        6 -> { // RGB格式
            val r = colorString.substring(0, 2).toInt(16) / 255f
            val g = colorString.substring(2, 4).toInt(16) / 255f
            val b = colorString.substring(4, 6).toInt(16) / 255f
            Color(red = r, green = g, blue = b)
        }

        8 -> { // ARGB格式
            val a = colorString.substring(0, 2).toInt(16) / 255f
            val r = colorString.substring(2, 4).toInt(16) / 255f
            val g = colorString.substring(4, 6).toInt(16) / 255f
            val b = colorString.substring(6, 8).toInt(16) / 255f
            Color(red = r, green = g, blue = b, alpha = a)
        }

        else -> throw IllegalArgumentException("无效的颜色字符串: \"$this\"")
    }
}

/**
 * 将Color对象转换为8位16进制颜色码（#AARRGGBB）的扩展函数。
 *
 * @return 转换后的16进制颜色字符串
 */
fun Color.toHex(): String {
    // 将颜色分量从0f-1f转换为0-255的整数
    val alpha = (this.alpha * 255).toInt().coerceIn(0, 255)
    val red = (this.red * 255).toInt().coerceIn(0, 255)
    val green = (this.green * 255).toInt().coerceIn(0, 255)
    val blue = (this.blue * 255).toInt().coerceIn(0, 255)

    // 格式化为两位16进制数，并拼接为#AARRGGBB格式
    return String.format("#%02X%02X%02X%02X", alpha, red, green, blue)
}