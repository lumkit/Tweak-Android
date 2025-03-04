package io.github.lumkit.tweak.model

import io.github.lumkit.tweak.TweakApplication
import java.io.File
import androidx.core.content.edit

object Config {

    val DEBUG: Boolean
        get() = false

    object Path {
        val binDir: File
            get() {
                val dir = File(TweakApplication.application.filesDir, "bin")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                return dir
            }
        val BusyboxFile: File
            get() = File(binDir, "busybox")
        val ScriptDir: File
            get() {
                val dir = File(TweakApplication.application.filesDir, "scripts")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                return dir
            }
        val cacheDir: File = File("/data/ota_package")

        val appCacheDir: File
            get() {
                val dir = File(TweakApplication.application.filesDir, "cache")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                return dir
            }
    }

    val ROOT_USERS = listOf(
        "su",
        "suu"
    )

    /**
     * 刷新间隔
     */
    const val DEFAULT_REFRESH_TICK = 1250

    val REFRESH_TICK: Long
        get() = TweakApplication.shared.getInt(Const.APP_OVERVIEW_TICK, DEFAULT_REFRESH_TICK)
            .toLong()

    /**
     * 电流单位
     */
    var BatteryElectricCurrent: Long
        get() = TweakApplication.shared.getLong(Const.APP_BATTERY_CURRENT_NOW_UNIT, -1000)
        set(value) {
            TweakApplication.shared.edit {
                putLong(
                    Const.APP_BATTERY_CURRENT_NOW_UNIT,
                    value
                )
            }
        }
}