package io.github.lumkit.tweak.ui.screen.notice.model

import androidx.compose.runtime.Composable
import androidx.core.content.edit
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.ui.view.SmartNoticeFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class SmartNoticeNotificationPlugin(
    open val factory: SmartNoticeFactory,
    val sharedKey: String,
) {
    private val sharedPreferences = TweakApplication.shared

    private val _enabled = MutableStateFlow(false)
    val enableState = _enabled.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
        onEnableChanged(enabled)
    }

    fun loadEnabled(defaultValue: Boolean = true) {
        _enabled.value = sharedPreferences.getBoolean(sharedKey, defaultValue)
        onEnableChanged(_enabled.value)
    }

    fun saveEnabled(key: String) {
        sharedPreferences.edit {
            putBoolean(key, _enabled.value)
        }
    }

    @Composable
    abstract fun PreferenceContent(plugin: SmartNoticeNotificationPlugin)

    open fun onDestroy(){
        onEnableChanged(false)
    }

    open fun onEnableChanged(enabled: Boolean) {}

    open fun display(state: Any) {}
}