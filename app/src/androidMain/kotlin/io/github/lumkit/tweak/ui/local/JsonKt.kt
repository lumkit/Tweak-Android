package io.github.lumkit.tweak.ui.local

import kotlinx.serialization.json.Json

val json by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    Json {
        isLenient = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
}