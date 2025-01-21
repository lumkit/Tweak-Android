package io.github.lumkit.tweak.ui.screen.settings

import io.github.lumkit.tweak.common.BaseViewModel
import io.github.lumkit.tweak.net.Apis

class SettingsViewModel: BaseViewModel() {


    fun checkVersion(versionCode: Int) = suspendLaunch("checkVersion") {
        loading()
        val all = Apis.Version.all()
        println(all)
        success("")
    }

}