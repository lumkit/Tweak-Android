package io.github.lumkit.tweak.common.util

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import io.github.lumkit.tweak.TweakApplication
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener
import rikka.shizuku.shared.BuildConfig

object ShizukuStatic {

    private val REQUEST_PERMISSION_RESULT_LISTENER: OnRequestPermissionResultListener =
        OnRequestPermissionResultListener { requestCode, grantResult ->
            onRequestPermissionsResult(requestCode, grantResult)
        }

    private val _componentName = ComponentName(
        TweakApplication.application,
        ShizukuStatic::class.java.name
    )

    private val userServiceArgs = Shizuku.UserServiceArgs(_componentName)
        .daemon(true)
        .debuggable(BuildConfig.DEBUG)
        .processNameSuffix("tweak_aidl_service")
        .version(1)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            unbindService()
        }

    }

    fun checkPermission(code: Int): Boolean {
        if (Shizuku.isPreV11()) {
            // Pre-v11 is unsupported
            return false
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            // Granted
            return true
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            // Users choose "Deny and don't ask again"
            return false;
        } else {
            // Request the permission
            Shizuku.requestPermission(code);
            return false
        }
    }

    private fun onRequestPermissionsResult(requestCode: Int, grantResult: Int) {
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        // Do stuff based on the result and the request code

    }

    fun removeRequestPermissionResultListener() {
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)
    }

    fun unbindService() {
        Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
    }

    fun bindService() {
        Shizuku.bindUserService(userServiceArgs, serviceConnection)
    }

}