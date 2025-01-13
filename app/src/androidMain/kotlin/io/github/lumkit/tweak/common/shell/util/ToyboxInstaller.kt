package io.github.lumkit.tweak.common.shell.util

import android.content.Context
import android.os.Build
import io.github.lumkit.tweak.model.Config
import java.io.File

class ToyboxInstaller(private val context: Context) {

    fun install() : String {
        val installPath: String = Config.Path.binDir.absolutePath

        val abi = Build.SUPPORTED_ABIS.joinToString(" ").lowercase()
        val fileName = if (abi.contains("arm64")) "toybox-outside64" else "toybox-outside";
        val assetsPath = if (abi.contains("arm64")) "bin/aarch64/$fileName" else "bin/armeabi-v7a/$fileName"

        val toyboxFile = File(installPath, fileName)

        if (!toyboxFile.exists()) {
            context.assets.open(assetsPath).use {
                it.buffered().use { bis ->
                    toyboxFile.outputStream().use { fos ->
                        fos.buffered().use { bos ->
                            bis.copyTo(bos)
                        }
                    }
                }
            }
        }

        return toyboxFile.absolutePath
    }
}