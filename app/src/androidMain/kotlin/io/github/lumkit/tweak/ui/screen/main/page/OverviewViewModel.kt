package io.github.lumkit.tweak.ui.screen.main.page

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import io.github.lumkit.tweak.common.BaseViewModel
import io.github.lumkit.tweak.common.shell.CpuLoad
import io.github.lumkit.tweak.common.shell.ExternalStorageUtils
import io.github.lumkit.tweak.common.shell.GpuUtils
import io.github.lumkit.tweak.common.shell.MemoryUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

@SuppressLint("StaticFieldLeak")
class OverviewViewModel(
    private val context: Context,
): BaseViewModel() {

    @Immutable
    data class MemoryBean(
        val ramTotal: Long = 100_000,
        val ramFree: Long = 100_000,
        @FloatRange(from = 0.0, to = 1.0) val ramUsed: Float = 0f,
        val romTotal: Long = 100_000,
        val romFree: Long = 100_000,
        @FloatRange(from = 0.0, to = 1.0) val rowUsed: Float = 0f,
        val swapTotal: Long = 100_000,
        val swapFree: Long = 100_000,
        @FloatRange(from = 0.0, to = 1.0) val swapUsed: Float = 0f,
        val totalUsed: Float = 0f,
        val swapCache: Long = 100_000,
        val dirty: Long = 0,
    )

    @Immutable
    data class GpuBean(
        @FloatRange(from = 0.0, to = 1.0) val used: Float = 0f,
        val currentFreq: String = "N/A",
        val describe: String = "N/A",
    )

    private val _memoryBeanState = MutableStateFlow(MemoryBean())
    val memoryBeanState = _memoryBeanState.asStateFlow()

    private val _gpuBeanState = MutableStateFlow(GpuBean())
    val gpuBeanState = _gpuBeanState.asStateFlow()

    suspend fun loadMemoryBeanState(): Unit = withContext(Dispatchers.IO) {
        val storageInfo = ExternalStorageUtils.getExternalStorageInfo(context)
        val memoryInfo = MemoryUtils.getMemoryInfo()
        storageInfo?.apply {
            val mu = memoryInfo.memTotal - memoryInfo.memAvailable.toFloat()
            val su = memoryInfo.swapTotal - memoryInfo.swapFree.toFloat()
            _memoryBeanState.value = MemoryBean(
                ramTotal = memoryInfo.memTotal,
                ramFree = memoryInfo.memAvailable,
                ramUsed = mu / memoryInfo.memTotal.toFloat(),
                romTotal = totalSpace,
                romFree = freeSpace,
                rowUsed = this.usedSpace.toFloat() / totalSpace.toFloat(),
                swapTotal = memoryInfo.swapTotal,
                swapFree = memoryInfo.swapFree,
                swapUsed = su / memoryInfo.swapTotal.toFloat(),
                totalUsed = (mu + su) / memoryInfo.memTotal,
                swapCache = memoryInfo.swapCached,
                dirty = memoryInfo.dirty
            )
        }
    }

    suspend fun loadGpuBeanState() = withContext(Dispatchers.IO) {
        _gpuBeanState.value = GpuBean(
            used = GpuUtils.getGpuLoad().toFloat() / 100f,
            currentFreq = GpuUtils.getGpuFreq(),
            describe = GpuUtils.gles()
        )
    }
}