package io.github.lumkit.tweak.common.util

import android.os.Environment

object CommonCmds {
    val SDCardDir: String = Environment.getExternalStorageDirectory().absolutePath

    val AbsBackUpDir = "$SDCardDir/backups/apps/"

    const val MountSystemRW =
            "busybox mount -o rw,remount / 2>/dev/null\n" +
                    "mount -o rw,remount /system 2>/dev/null\n" +
                    "busybox mount -o rw,remount / 2>/dev/null\n" +
                    "mount -o rw,remount /system 2>/dev/null\n" +
                    "busybox mount -o remount,rw /dev/block/bootdevice/by-name/system /system 2>/dev/null\n" +
                    "mount -o remount,rw /dev/block/bootdevice/by-name/system /system 2>/dev/null\n" +
                    "busybox mount -o rw,remount /vendor 2>/dev/null\n" +
                    "mount -o rw,remount /vendor 2>/dev/null\n"

    const val DisableSELinux = "setenforce 0;\n"
    const val ResumeSELinux = "setenforce 1;\n"
}