package io.github.lumkit.tweak

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.local.StorageStore
import io.github.lumkit.tweak.ui.screen.runtime.RuntimeModeViewModel

class TweakApplication: Application() {

    companion object {
        lateinit var application: Application
        lateinit var shared: SharedPreferences
        var rootUserState = false
    }

    private lateinit var runtimeModeViewModel: RuntimeModeViewModel

    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        application = this
        shared = this.getSharedPreferences(Const.APP_SHARED_PREFERENCE_ID, Context.MODE_PRIVATE)

        val storageStore = StorageStore()

        runtimeModeViewModel = RuntimeModeViewModel(
            context = application,
            storageStore
        )

        if (storageStore.getBoolean(Const.APP_ACCEPT_RISK)) {
            runtimeModeViewModel.installBusybox()
        }
    }

}