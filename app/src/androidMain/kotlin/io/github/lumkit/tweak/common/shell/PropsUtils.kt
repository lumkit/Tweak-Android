package io.github.lumkit.tweak.common.shell

import io.github.lumkit.tweak.common.shell.provide.ReusableShells

object PropsUtils {
    /**
     * 获取属性
     *
     * @param propName 属性名称
     * @return 内容
     */
    suspend fun getProp(propName: String): String {
        return ReusableShells.execSync("getprop \"$propName\"")
    }

    suspend fun setProp(propName: String, value: String): Boolean {
        return ReusableShells.execSync("setprop \"$propName\" \"$value\"") != "error"
    }
}