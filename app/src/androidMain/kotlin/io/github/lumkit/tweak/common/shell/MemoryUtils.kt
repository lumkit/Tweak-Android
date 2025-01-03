package io.github.lumkit.tweak.common.shell

import android.util.Log
import java.io.File
import java.io.IOException

object MemoryUtils {

    data class MemoryInfo(
        var memTotal: Long = 0,
        var memAvailable: Long = 0,
        var swapCached: Long = 0,
        var swapTotal: Long = 0,
        var swapFree: Long = 0,
        var buffers: Long = 0,
        var dirty: Long = 0
    )

    private const val MEMINFO_PATH = "/proc/meminfo"
    private const val CACHE_DURATION_MS = 500L

    @Volatile
    private var lastMemoryInfo: MemoryInfo? = null

    @Volatile
    private var lastMemoryInfoTime: Long = 0

    /**
     * 从 /proc/meminfo 中提取内存信息
     */
    @Synchronized
    fun getMemoryInfo(): MemoryInfo {
        val currentTime = System.currentTimeMillis()
        // 使用缓存，如果上次读取时间在 CACHE_DURATION_MS 内，则返回缓存结果
        if (lastMemoryInfo != null && (currentTime - lastMemoryInfoTime) < CACHE_DURATION_MS) {
            return lastMemoryInfo!!
        }

        val memInfo = MemoryInfo()
        try {
            File(MEMINFO_PATH).useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split(":")
                    if (parts.size < 2) return@forEach
                    val key = parts[0].trim()
                    val valuePart = parts[1].trim().split(" ").firstOrNull()?.toLongOrNull()?.let { it * 1024 } ?: return@forEach

                    when (key) {
                        "MemTotal" -> memInfo.memTotal = valuePart
                        "MemAvailable" -> memInfo.memAvailable = valuePart
                        "SwapCached" -> memInfo.swapCached = valuePart
                        "SwapTotal" -> memInfo.swapTotal = valuePart
                        "SwapFree" -> memInfo.swapFree = valuePart
                        "Buffers" -> memInfo.buffers = valuePart
                        "Dirty" -> memInfo.dirty = valuePart
                    }
                }
            }
            lastMemoryInfo = memInfo
            lastMemoryInfoTime = currentTime
        } catch (e: IOException) {
             Log.e("MemoryUtils", "Failed to read $MEMINFO_PATH", e)
        } catch (e: Exception) {
             Log.e("MemoryUtils", "Unexpected error", e)
        }

        return lastMemoryInfo ?: MemoryInfo()
    }
}
