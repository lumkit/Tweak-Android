package io.github.lumkit.tweak.common.util

import android.os.Build
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.shell.provide.ReusableShells
import kotlinx.coroutines.runBlocking

fun deviceModeName(): String {
    val shell = ReusableShells.getInstance("sh", "sh", status = TweakApplication.runtimeStatus)
    val marketname = runBlocking { shell.commitCmdSync("getprop ro.product.marketname") }
    return marketname.ifBlank { Build.MODEL }
}