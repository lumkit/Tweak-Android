package io.github.lumkit.tweak

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.core.content.edit
import io.github.lumkit.tweak.common.util.AppListHelper
import io.github.lumkit.tweak.data.RuntimeStatus
import io.github.lumkit.tweak.model.AppInfo
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.services.AppChangeReceiver
import io.github.lumkit.tweak.ui.local.StorageStore
import io.github.lumkit.tweak.ui.screen.runtime.RuntimeModeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class TweakApplication : Application() {

    companion object {
        lateinit var application: Application
        lateinit var shared: SharedPreferences
        lateinit var density: Density
        var rootUserState = false

        var runtimeStatus by mutableStateOf(RuntimeStatus.Normal)

        fun setRuntimeStatusState(status: RuntimeStatus) {
            runtimeStatus = status
            shared.edit(commit = true) { putInt(Const.APP_SHARED_RUNTIME_STATUS, status.ordinal) }
        }
        var userApps by mutableStateOf(listOf<AppInfo>())
        var systemApps by mutableStateOf(listOf<AppInfo>())
        var backupApps by mutableStateOf(listOf<AppInfo>())
        var bootableApps by mutableStateOf(listOf<AppInfo>())
        lateinit var appListHelper: AppListHelper
        private val ioScope = CoroutineScope(Dispatchers.IO)

        fun updateApps() {
            ioScope.launch {
                try {
                    userApps = appListHelper.getUserAppList()
                    systemApps = appListHelper.getSystemAppList()
                    backupApps = appListHelper.getBackupedAppList()
                    bootableApps = appListHelper.getBootableApps()
                }catch (_: Exception) {}
            }
        }
    }

    private lateinit var runtimeModeViewModel: RuntimeModeViewModel
    private lateinit var appChangeReceiver: AppChangeReceiver

    override fun onCreate() {
        super.onCreate()
        init()

        // 更新应用列表
        appChangeReceiver = AppChangeReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(appChangeReceiver, intentFilter)
    }

    @SuppressLint("HardwareIds")
    private fun init() {
        application = this
        shared = this.getSharedPreferences(Const.APP_SHARED_PREFERENCE_ID, MODE_PRIVATE)
        density = Density(this)
        appListHelper = AppListHelper(this)

        // 验证运行的设备是否被更改
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        // 获取存储的设备ID
        val localDeviceId = shared.getString(Const.APP_DEVICE_ID, null)

        if (localDeviceId == null || androidId != localDeviceId) {
            shared.edit(commit = true) {
                // 清除所有数据，或者仅清除与设备ID相关的数据
                clear()  // 如果你只想清除特定的存储项，可以替换为 remove(key)

                // 更新设备ID
                putString(Const.APP_DEVICE_ID, androidId)
            }
        }

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