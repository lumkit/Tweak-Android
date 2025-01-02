package io.github.lumkit.tweak.model

import io.github.lumkit.tweak.TweakApplication
import java.io.File

object Config {
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
    }
}