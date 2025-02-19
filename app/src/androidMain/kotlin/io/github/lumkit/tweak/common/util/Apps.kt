package io.github.lumkit.tweak.common.util

import android.content.Context
import android.content.Intent
import android.os.Process

fun Context.restartApp() {
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    val componentName = intent?.component
    val mainIntent = Intent.makeRestartActivityTask(componentName)
    startActivity(mainIntent)

    // 结束当前进程
    Process.killProcess(Process.myPid())
}