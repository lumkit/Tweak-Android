package io.github.lumkit.tweak.ui.view.model

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.core.bundle.Bundle
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.model.AppInfo
import io.github.lumkit.tweak.ui.view.SmartNoticeWindow

abstract class NotificationImpl(
    open val statusBarNotification: StatusBarNotification?,
    open val view: SmartNoticeWindow,
    open val scope: SmartNoticeWindow.SmartNoticeWindowScope,
    open val density: Density,
) {
    protected val notification: Notification?
        get() = statusBarNotification?.notification
    protected val info: AppInfo?
        get() = TweakApplication.userApps.firstOrNull {
            it.packageName == statusBarNotification?.packageName
        }
    protected val extras: Bundle?
        get() = notification?.extras
    protected val context: Context get() = view.context

    abstract fun componentSize(): DpSize

    @Composable
    abstract fun Content()
}