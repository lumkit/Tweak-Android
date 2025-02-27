package io.github.lumkit.tweak.common.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.ui.geometry.Size
import androidx.core.net.toUri
import java.math.BigDecimal
import kotlin.math.sqrt

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

fun Context.startBrowser(url: String?) {
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

@SuppressLint("InternalInsetResource")
fun getStatusBarHeight(): Int {
    val resources = Resources.getSystem()
    val resourceId = resources.getIdentifier(
        "status_bar_height",
        "dimen",
        "android"
    )
    return resources.getDimensionPixelSize(resourceId)
}

fun Context.getDiveSize(): Size {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = wm.defaultDisplay
    val size = android.graphics.Point()
    display.getSize(size)
    return Size(width = size.x.toFloat(), height = size.y.toFloat())
}
