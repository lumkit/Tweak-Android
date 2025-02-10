package io.github.lumkit.tweak.ui.screen.settings

import io.github.lumkit.tweak.common.BaseViewModel
import io.github.lumkit.tweak.common.status.TweakException
import io.github.lumkit.tweak.common.util.makeText
import io.github.lumkit.tweak.net.Apis
import io.github.lumkit.tweak.net.pojo.resp.ClientVersionBean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel: BaseViewModel() {

    private val _clientVersionState = MutableStateFlow<ClientVersionBean?>(null)
    val clientVersionState = _clientVersionState.asStateFlow()

    private val _latestMustBeVersion = MutableStateFlow<ClientVersionBean?>(null)
    val latestMustBeVersion = _latestMustBeVersion.asStateFlow()

    private val _mustUpdateDialogState = MutableStateFlow(false)
    val mustUpdateDialogState = _mustUpdateDialogState.asStateFlow()

    fun checkVersion(versionCode: Int) = suspendLaunch(
        id = "checkVersion",
        onError = {
            it.printStackTrace()
            fail(it.makeText())
            _clientVersionState.value = null
        }
    ) {
        loading()
        val latest = Apis.Version.latest()
        val data = latest.data ?: throw TweakException("暂无版本信息")
        if (data.versionCode <= versionCode) {
            throw TweakException("已是最新版~")
        }
        _clientVersionState.value = data
        success("有更新啦~")
    }

    fun checkMustUpdate(versionCode: Int) = suspendLaunch(
        "checkMustUpdate",
        onError = {
            it.printStackTrace()
            fail(it.makeText())
            _latestMustBeVersion.value = null
            _mustUpdateDialogState.value = false
        }
    ) {
        loading()
        val mustBe = Apis.Version.latestMustBe()
        val latest = Apis.Version.latest()

        val mustData = mustBe.data
        val latestData = latest.data

        if (mustData == null || latestData == null) {
            throw TweakException("暂无版本信息")
        }

        if (mustData.versionCode <= versionCode) {
            throw TweakException("不用强制更新")
        }

        _latestMustBeVersion.value = latestData
        _mustUpdateDialogState.value = true
        success("有更新啦")
    }

    fun dismiss() {
        _mustUpdateDialogState.value = false
    }
} 