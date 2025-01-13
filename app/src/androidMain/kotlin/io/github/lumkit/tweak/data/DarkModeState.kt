package io.github.lumkit.tweak.data

import io.github.lumkit.tweak.R

enum class DarkModeState {
    System, Light, Dark
}

fun DarkModeState.asStringId(): Int = when (this) {
    DarkModeState.System -> R.string.text_dark_mode_system
    DarkModeState.Light -> R.string.text_dark_mode_light
    DarkModeState.Dark -> R.string.text_dark_mode_dark
}