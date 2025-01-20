package io.github.lumkit.tweak.common.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.model.Const

fun autoStartService(service: Class<out Service>) {
    if (TweakApplication.shared.getBoolean(Const.APP_AUTO_START_SERVICE, false)) {
        TweakApplication.application.startService(service)
    }
}

fun Context.startService(service: Class<out Service>) {
    val intent = Intent(this, service)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}