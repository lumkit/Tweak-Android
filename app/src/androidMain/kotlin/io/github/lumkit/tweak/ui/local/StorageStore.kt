package io.github.lumkit.tweak.ui.local

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.lumkit.tweak.TweakApplication

val LocalStorageStore = staticCompositionLocalOf<StorageStore> { error("LocalStorageStore is not provided.") }

class StorageStore {
    private val shared = TweakApplication.shared

    infix fun getString(name: String): String? = shared.getString(name, null)
    infix fun getBoolean(name: String): Boolean = shared.getBoolean(name, false)
    infix fun getInt(name: String): Int = shared.getInt(name, 0)
    infix fun getFloat(name: String): Float = shared.getFloat(name, 0f)
    infix fun getStringSet(name: String): Set<String>? = shared.getStringSet(name, emptySet())

    fun putString(name: String, value: String) {
        shared.edit().putString(name, value).apply()
    }

    fun putBoolean(name: String, value: Boolean) {
        shared.edit().putBoolean(name, value).apply()
    }

    fun putInt(name: String, value: Int) {
        shared.edit().putInt(name, value).apply()
    }

    fun putFloat(name: String, value: Float) {
        shared.edit().putFloat(name, value).apply()
    }

    fun putStringSet(name: String, value: Set<String>) {
        shared.edit().putStringSet(name, value).apply()
    }
}