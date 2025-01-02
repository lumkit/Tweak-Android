package io.github.lumkit.tweak.common.permissions

import java.io.File

object Busybox {
    fun systemBusyboxInstalled(): Boolean {
        if (
            File("/sbin/busybox").exists() ||
            File("/system/xbin/busybox").exists() ||
            File("/system/sbin/busybox").exists() ||
            File("/system/bin/busybox").exists() ||
            File("/vendor/bin/busybox").exists() ||
            File("/vendor/xbin/busybox").exists() ||
            File("/odm/bin/busybox").exists()) {
            return true
        }
        return try {
            Runtime.getRuntime().exec("busybox --help").destroy()
            true
        } catch (ex: Exception) {
            false
        }
    }
}