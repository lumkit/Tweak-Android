package io.github.lumkit.tweak.common.util

import android.annotation.SuppressLint
import android.os.Build

object SystemProperties {

    /**
     * 获取系统属性值。
     *
     * @param key 属性键名。
     * @return 属性值，如果无法获取则返回null。
     */
    @SuppressLint("PrivateApi")
    fun get(key: String): String {
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val getMethod = systemProperties.getMethod("get", String::class.java)
            getMethod.invoke(null, key) as String
        } catch (e: Exception) {
            ""
        }
    }
}
