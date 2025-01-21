package io.github.lumkit.tweak.common.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri

fun Context.getVersionCode() = try {
    packageManager.getPackageInfo(packageName, 0).versionCode
} catch (e: PackageManager.NameNotFoundException) {
    e.printStackTrace()
    -1
}

fun Context.getVersionName() = try {
    packageManager.getPackageInfo(packageName, 0).versionName
} catch (e: PackageManager.NameNotFoundException) {
    e.printStackTrace()
    null
}

fun Context.startBrowser(url: String?){
    try {
        val intent = Intent()
        intent.action = "android.intent.action.VIEW"
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.data = url?.toUri()
        startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}