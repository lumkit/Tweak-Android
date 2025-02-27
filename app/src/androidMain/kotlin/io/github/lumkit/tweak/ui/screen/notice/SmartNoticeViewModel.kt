package io.github.lumkit.tweak.ui.screen.notice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.core.app.ActivityCompat
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.BaseViewModel
import io.github.lumkit.tweak.common.shell.provide.ReusableShells
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@SuppressLint("StaticFieldLeak")
class SmartNoticeViewModel(
    private val activity: ComponentActivity
) : BaseViewModel() {

    companion object {
        private const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"

        fun Context.isNotificationListenersEnabled(): Boolean {
            val flat = Settings.Secure.getString(contentResolver, ENABLED_NOTIFICATION_LISTENERS)
            return flat.split(":")
                .map { it.substring(0, it.indexOf("/")) }
                .find { it == packageName } != null
        }

        fun Context.gotoNotificationAccessSetting() {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        suspend fun checkAccessibilityService(): Boolean = withContext(Dispatchers.IO) {
            ReusableShells.execSync("settings get secure enabled_accessibility_services").contains(TweakApplication.application.packageName)
        }
    }

    @Immutable
    data class PermissionState(
        val name: String,
        val permission: String,
        val grant: Boolean,
        val describe: String,
        @DrawableRes val icon: Int,
    )

    private val _permissionsState = MutableStateFlow(
        listOf(
            PermissionState(
                name = TweakApplication.application.getString(R.string.text_name_accessibility_service),
                permission = Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
                grant = false,
                describe = TweakApplication.application.getString(R.string.text_decscribe_accessibility_service),
                icon = R.drawable.ic_alert,
            ),
            PermissionState(
                name = TweakApplication.application.getString(R.string.text_name_alert),
                permission = Manifest.permission.SYSTEM_ALERT_WINDOW,
                grant = false,
                describe = TweakApplication.application.getString(R.string.text_decscribe_alert),
                icon = R.drawable.ic_alert,
            ),
            PermissionState(
                name = TweakApplication.application.getString(R.string.text_name_bind_notification_listener_service),
                permission = Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
                grant = false,
                describe = TweakApplication.application.getString(R.string.text_decscribe_bind_notification_listener_service),
                icon = R.drawable.ic_notification_outline,
            ),
            PermissionState(
                name = TweakApplication.application.getString(R.string.text_name_ignore_battery_optimizations),
                permission = Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                grant = false,
                describe = TweakApplication.application.getString(R.string.text_decscribe_ignore_battery_optimizations),
                icon = R.drawable.ic_ignore_battery_optimizations,
            ),
        )
    )

    val permissionsState = _permissionsState.asStateFlow()

    fun checkPermissions() {
        val list = ArrayList<PermissionState>()
        for (permissionState in _permissionsState.value) {
            if (permissionState.permission == Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE) {
                list.add(permissionState.copy(grant = TweakApplication.application.isNotificationListenersEnabled()))
            } else if (permissionState.permission == Manifest.permission.SYSTEM_ALERT_WINDOW){
                list.add(permissionState.copy(grant = Settings.canDrawOverlays(TweakApplication.application)))
            } else if (permissionState.permission == Manifest.permission.BIND_ACCESSIBILITY_SERVICE) {
                list.add(
                    permissionState.copy(grant = runBlocking { checkAccessibilityService() })
                )
            } else {
                val grant = ActivityCompat.checkSelfPermission(activity, permissionState.permission)
                list.add(permissionState.copy(grant = grant == PackageManager.PERMISSION_GRANTED))
            }
        }
        _permissionsState.value = list
    }

    fun requestPermission(
        permission: String,
        grant: Boolean
    ) {
        val list = ArrayList<PermissionState>()
        for (permissionState in _permissionsState.value) {
            list.add(
                permissionState.copy(
                    grant = if (permissionState.permission == permission) {
                        grant
                    } else {
                        permissionState.grant
                    }
                )
            )
        }
        _permissionsState.value = list
    }
}