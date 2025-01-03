package io.github.lumkit.tweak.common.shell

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.io.File

object ExternalStorageUtils {

    data class ExternalStorageInfo(
        val totalSpace: Long,      // 总空间，单位字节
        val freeSpace: Long,       // 剩余空间，单位字节
        val usedSpace: Long,       // 已用空间，单位字节
        val usedPercentage: Float // 使用比例，0.0 到 100.0
    )

    private const val CACHE_DURATION_MS = 5000L // 缓存持续时间，单位毫秒

    @Volatile
    private var cachedInfo: ExternalStorageInfo? = null

    @Volatile
    private var lastFetchTime: Long = 0

    /**
     * 获取外部存储的存储信息
     * @param context Android 上下文
     * @return ExternalStorageInfo 包含总空间、剩余空间、已用空间和使用比例
     */
    @Synchronized
    fun getExternalStorageInfo(context: Context): ExternalStorageInfo? {
        val currentTime = System.currentTimeMillis()
        // 如果缓存有效，返回缓存结果
        if (cachedInfo != null && (currentTime - lastFetchTime) < CACHE_DURATION_MS) {
            return cachedInfo
        }

        // 检查外部存储是否可用
        if (!isExternalStorageAvailable()) {
            return null
        }

        // 获取外部存储目录
        val externalStorageDir: File? = Environment.getExternalStorageDirectory()

        if (externalStorageDir == null || !externalStorageDir.exists()) {
            return null
        }

        try {
            val stat = StatFs(externalStorageDir.path)

            // 使用不同的 API 以兼容不同的 Android 版本
            val totalBytes: Long = stat.blockCountLong * stat.blockSizeLong
            val freeBytes: Long = stat.availableBlocksLong * stat.blockSizeLong

            val usedBytes = totalBytes - freeBytes
            val usedPercentage = if (totalBytes > 0f) {
                (usedBytes / totalBytes) * 100.0f
            } else {
                0.0f
            }

            val storageInfo = ExternalStorageInfo(
                totalSpace = totalBytes,
                freeSpace = freeBytes,
                usedSpace = usedBytes,
                usedPercentage = usedPercentage
            )

            // 更新缓存
            cachedInfo = storageInfo
            lastFetchTime = currentTime

            return storageInfo

        } catch (e: Exception) {
            Log.e("ExternalStorageUtils", "Error fetching external storage info", e)
            return null
        }
    }

    /**
     * 检查外部存储是否可用
     * @return Boolean 外部存储是否可用
     */
    private fun isExternalStorageAvailable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }
}
