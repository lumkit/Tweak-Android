package io.github.lumkit.tweak.common.shell

import io.github.lumkit.tweak.common.shell.provide.ReusableShells

/**
 * 操作内核参数节点
 * Created by Hello on 2017/11/01.
 */
object KernelProp {
    /**
     * 获取属性
     * @param propName 属性名称
     * @return
     */
    suspend fun getProp(propName: String): String {
        return ReusableShells.execSync("if [[ -e \"$propName\" ]]; then cat \"$propName\"; fi;")
    }

    suspend fun getProp(propName: String, grep: String): String {
        return ReusableShells.execSync("if [[ -e \"$propName\" ]]; then cat \"$propName\" | grep \"$grep\"; fi;")
    }

    /**
     * 保存属性
     * @param propName 属性名称（要永久保存，请以persist.开头）
     * @param value    属性值,值尽量是简单的数字或字母，避免出现错误
     */
    suspend fun setProp(propName: String, value: String): Boolean {
        return ReusableShells.execSync(
                "chmod 664 \"$propName\" 2 > /dev/null\n" +
                "echo \"$value\" > \"$propName\""
        ) != "error"
    }
}