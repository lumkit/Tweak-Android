package io.github.lumkit.tweak.ui.local

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.lumkit.tweak.TweakApplication
import androidx.core.content.edit

val LocalStorageStore =
    staticCompositionLocalOf<StorageStore> { error("LocalStorageStore is not provided.") }

class StorageStore {
    private val shared = TweakApplication.shared

    infix fun getString(name: String): String? = shared.getString(name, null)
    fun getBoolean(name: String, default: Boolean = false): Boolean =
        shared.getBoolean(name, default)

    fun getInt(name: String, default: Int = 0): Int = shared.getInt(name, default)
    fun getLong(name: String, default: Long = 0L): Long = shared.getLong(name, default)
    fun getFloat(name: String, defValue: Float = 0f): Float = shared.getFloat(name, defValue)
    infix fun getStringSet(name: String): Set<String>? = shared.getStringSet(name, emptySet())

    fun putString(name: String, value: String) {
        shared.edit { putString(name, value) }
    }

    fun putBoolean(name: String, value: Boolean) {
        shared.edit { putBoolean(name, value) }
    }

    fun putInt(name: String, value: Int) {
        shared.edit { putInt(name, value) }
    }

    fun putLong(name: String, value: Long) {
        shared.edit { putLong(name, value) }
    }

    fun putFloat(name: String, value: Float) {
        shared.edit { putFloat(name, value) }
    }

    fun putStringSet(name: String, value: Set<String>) {
        shared.edit { putStringSet(name, value) }
    }
}