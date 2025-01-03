package io.github.lumkit.tweak.common.shell

import android.util.Log
import io.github.lumkit.tweak.common.shell.provide.ReusableShells

/**
 * CpuTemperatureUtils 用于获取Android设备的CPU温度。
 */
object CpuTemperatureUtils {

    private const val TAG = "CpuTemperatureUtils"
    private const val CACHE_KEY = "cpu_temperature"

    /**
     * 获取CPU温度（°C）。
     *
     * @return CPU温度，如果无法获取则返回null。
     */
    suspend fun getCpuTemperature(): Float? {
        // 定义可能存放CPU温度的文件路径
        val thermalZonePaths = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/thermal/thermal_zone2/temp",
            "/sys/class/thermal/thermal_zone3/temp",
            "/sys/class/thermal/thermal_zone4/temp",
            "/sys/class/thermal/thermal_zone5/temp"
        )

        for (path in thermalZonePaths) {
            try {
                // 通过Shell命令读取温度值
                val command = "cat $path"
                val output = ReusableShells.execSync(command).trim()
                if (output.isNotEmpty()) {
                    val tempMilli = output.toFloatOrNull()
                    if (tempMilli != null) {
                        val tempCelsius = tempMilli / 1000
                        return tempCelsius
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading temperature from $path: ${e.message}")
            }
        }

        // 如果未找到合适的thermal_zone，尝试从/proc/cpuinfo或其他路径获取
        return getTemperatureFallback()
    }

    /**
     * 尝试从其他来源获取CPU温度作为备选方案。
     *
     * @return CPU温度，如果无法获取则返回null。
     */
    private suspend fun getTemperatureFallback(): Float? {
        // 这里可以添加其他获取温度的方法，例如通过dumpsys命令
        // 示例：使用dumpsys thermald
        try {
            val command = "dumpsys thermald | grep 'cpu'"
            val output = ReusableShells.execSync(command).trim()
            if (output.isNotEmpty()) {
                // 假设输出格式为 "cpu0: 45.0°C"
                val regex = Regex("""cpu\d+:\s+([\d.]+)°C""")
                val matchResult = regex.find(output)
                if (matchResult != null && matchResult.groupValues.size > 1) {
                    val tempStr = matchResult.groupValues[1]
                    return tempStr.toFloatOrNull()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing fallback temperature command: ${e.message}")
        }
        return null
    }
}
