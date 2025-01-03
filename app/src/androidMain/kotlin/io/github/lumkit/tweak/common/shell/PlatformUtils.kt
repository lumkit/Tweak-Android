package io.github.lumkit.tweak.common.shell

import kotlinx.coroutines.runBlocking

object PlatformUtils {
    //获取CPU型号，如msm8996
    fun getCPUName(): String = runBlocking {
        PropsUtils.getProp("ro.board.platform")
    }
}