package io.github.lumkit.tweak

import android.app.Application
import android.content.SharedPreferences
import android.os.Process
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import io.github.lumkit.tweak.data.RuntimeStatus
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.local.StorageStore
import io.github.lumkit.tweak.ui.screen.runtime.RuntimeModeViewModel
import rikka.shizuku.Shizuku

class TweakApplication : Application() {

    companion object {
        lateinit var application: Application
        lateinit var shared: SharedPreferences
        var rootUserState = false

        var runtimeStatus by mutableStateOf(RuntimeStatus.Normal)

        fun setRuntimeStatusState(status: RuntimeStatus) {
            runtimeStatus = status
            shared.edit(commit = true) { putInt(Const.APP_SHARED_RUNTIME_STATUS, status.ordinal) }
        }
    }

    private lateinit var runtimeModeViewModel: RuntimeModeViewModel

    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        application = this
        shared = this.getSharedPreferences(Const.APP_SHARED_PREFERENCE_ID, MODE_PRIVATE)

        val storageStore = StorageStore()

        runtimeModeViewModel = RuntimeModeViewModel(
            context = application,
            storageStore
        )

        setRuntimeStatusState(
            RuntimeStatus.entries[shared.getInt(
                Const.APP_SHARED_RUNTIME_STATUS,
                RuntimeStatus.Normal.ordinal
            )]
        )

        if (storageStore.getBoolean(Const.APP_SHARED_RUNTIME_MODE_STATE)) {
            when (runtimeStatus) {
                RuntimeStatus.Normal -> {

                }

                RuntimeStatus.Shizuku -> {
                    runtimeModeViewModel.installBusybox()

                    Shizuku.addBinderDeadListener {
                        Toast.makeText(
                            this,
                            R.string.text_shizuku_service_is_nonactivated,
                            Toast.LENGTH_SHORT
                        ).show()
                        Process.killProcess(Process.myPid())
                    }
                }

                RuntimeStatus.Root -> {
                    runtimeModeViewModel.installBusybox()
                }
            }
        }
    }
}