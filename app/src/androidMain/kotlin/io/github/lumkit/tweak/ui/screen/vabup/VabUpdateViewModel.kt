package io.github.lumkit.tweak.ui.screen.vabup

import androidx.compose.ui.text.input.TextFieldValue
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.BaseViewModel
import io.github.lumkit.tweak.common.shell.module.UpdateEngineClient
import io.github.lumkit.tweak.common.shell.provide.ReusableShells
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class VabUpdateViewModel: BaseViewModel() {

    private val _romPathState = MutableStateFlow(TextFieldValue())
    val romPathState = _romPathState.asStateFlow()

    private val _installEnabledState = MutableStateFlow(true)
    val installEnabled = _installEnabledState.asStateFlow()

    private val _startButtonTextState = MutableStateFlow(TweakApplication.application.getString(R.string.text_start_vab_update))
    val startButtonTextState = _startButtonTextState.asStateFlow()

    fun setPath(path: TextFieldValue) {
        _romPathState.value = path
    }

    fun setStartButtonTextState(text: String) {
        _startButtonTextState.value = text
    }

    fun install() = suspendLaunch("install") {
        _installEnabledState.value = false
        _startButtonTextState.value = TweakApplication.application.getString(R.string.text_start_unzip)
        val unzipRomPath = UpdateEngineClient.unzipRom(_romPathState.value.text)
        _startButtonTextState.value = TweakApplication.application.getString(R.string.text_installing)
        UpdateEngineClient.installRom(unzipRomPath)
        _installEnabledState.value = true
    }

    fun cancel() = suspendLaunch("cancel") {
        _installEnabledState.value = false
        ReusableShells.execSync("update_engine_client --cancel")
        _installEnabledState.value = true
    }

    fun merge() = suspendLaunch("merge") {
        _installEnabledState.value = false
        ReusableShells.execSync("update_engine_client --merge")
        _installEnabledState.value = true
    }

    fun resetStatus() = suspendLaunch("resetStatus") {
        _installEnabledState.value = false
        ReusableShells.execSync("update_engine_client --reset_status")
        _installEnabledState.value = true
    }
}