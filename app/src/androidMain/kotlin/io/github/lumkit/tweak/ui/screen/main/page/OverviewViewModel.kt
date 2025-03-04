package io.github.lumkit.tweak.ui.screen.main.page

import android.annotation.SuppressLint
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import io.github.lumkit.tweak.common.BaseViewModel
import io.github.lumkit.tweak.common.shell.BatteryUtils
import io.github.lumkit.tweak.common.shell.CpuFrequencyUtils
import io.github.lumkit.tweak.common.shell.CpuLoad
import io.github.lumkit.tweak.common.shell.CpuTemperatureUtils
import io.github.lumkit.tweak.common.shell.ExternalStorageUtils
import io.github.lumkit.tweak.common.shell.GpuUtils
import io.github.lumkit.tweak.common.shell.MemoryUtils
import io.github.lumkit.tweak.common.shell.ProcessUtils
import io.github.lumkit.tweak.common.shell.provide.ReusableShells
import io.github.lumkit.tweak.common.util.CpuCodenameUtils
import io.github.lumkit.tweak.common.util.firstLine
import io.github.lumkit.tweak.common.util.formatUptimeHour
import io.github.lumkit.tweak.data.AndroidSoc
import io.github.lumkit.tweak.data.ChartState
import io.github.lumkit.tweak.model.Config
import io.github.lumkit.tweak.model.ProcessInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

@SuppressLint("StaticFieldLeak")
class OverviewViewModel(
    private val context: Context,
) : BaseViewModel() {

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

    @Immutable
    data class CpuDetail(
        val cpuTemperature: Float = 25f,
        val cpuTotalUsed: ChartState = ChartState(0f),
        val cpuName: String = "N/A",
        val composition: String = "N/A",
        val cores: List<CoreDetail> = emptyList(),
    )

    @Immutable
    data class CoreDetail(
        val minFreq: String = "N/A",
        val maxFreq: String = "N/A",
        val currentFreq: String = "N/A",
        val used: ChartState = ChartState(0f),
    )

    @Immutable
    data class OtherDetail(
        val batteryLevel: Int = 0,
        val electricCurrent: Float = 0f,
        val voltage: Float = 0f,
        val temperature: Float = 0f,
        val androidVersion: String = "N/A",
        val androidSDK: Int = 0,
        val runningDuration: String = "N/A",
    )

    private val _memoryBeanState = MutableStateFlow(MemoryBean())
    val memoryBeanState = _memoryBeanState.asStateFlow()

    private val _gpuBeanState = MutableStateFlow(GpuBean())
    val gpuBeanState = _gpuBeanState.asStateFlow()

    private val _cpuDetailState = MutableStateFlow(CpuDetail())
    val cpuDetailState = _cpuDetailState.asStateFlow()

    private val _otherDetailState = MutableStateFlow(OtherDetail())
    val otherDetailState = _otherDetailState.asStateFlow()

    private val _runningServicesDetailState = MutableStateFlow(emptyList<ProcessInfo>())
    val runningServicesDetailState = _runningServicesDetailState.asStateFlow()

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

    suspend fun loadGpuBeanState(): Unit = withContext(Dispatchers.IO) {
        _gpuBeanState.value = GpuBean(
            used = GpuUtils.getGpuLoad().toFloat() / 100f,
            currentFreq = GpuUtils.getGpuFreq(),
            describe = GpuUtils.gles()
        )
    }

    suspend fun loadCpuDetailState(): Unit = withContext(Dispatchers.IO) {
        try {
            val coreDetails = mutableListOf<CoreDetail>()

            val coreCount = CpuFrequencyUtils.getCoreCount()
            val cpuLoad = CpuLoad.getCpuLoad()
            val totalUsed = CpuLoad.getCpuLoadSum() / 100f

            for (i in 0 until coreCount) {
                val cpuId = "cpu$i"
                val minFreq = CpuFrequencyUtils.getCurrentMinFrequency(cpuId)

                val coreDetail = CoreDetail(
                    minFreq = minFreq,
                    maxFreq = CpuFrequencyUtils.getCurrentMaxFrequency(cpuId),
                    currentFreq = CpuFrequencyUtils.getCurrentFrequency(cpuId),
                    used = ChartState(
                        progress = (cpuLoad[i] ?: 0f) / 100f
                    )
                )
                coreDetails.add(coreDetail)
            }

            val composition = buildString {
                CpuFrequencyUtils.getClusterInfo().onEachIndexed { index, items ->
                    if (index > 0) append("+")
                    append("${items.size}")
                }
            }

            _cpuDetailState.value = CpuDetail(
                cpuTemperature = CpuTemperatureUtils.getCpuTemperature() ?: 25f,
                cpuTotalUsed = ChartState(
                    progress = totalUsed
                ),
                cpuName = AndroidSoc.getSocByCpuMode(CpuCodenameUtils.getCpuCodename())?.NAME ?: "CPU",
                composition = composition,
                cores = coreDetails,
            )
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    suspend fun loadOtherDetailState(): Unit = withContext(Dispatchers.IO) {

        val batteryStatus = BatteryUtils.getBatteryStatus()
        val batteryCurrentNow = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val voltage = try {
            ReusableShells.execSync("cat /sys/class/power_supply/battery/voltage_now").firstLine().toFloat()
        }catch (_: Exception) {
            -1f
        }

        _otherDetailState.value = OtherDetail(
            batteryLevel = batteryStatus.level,
            electricCurrent = if (batteryCurrentNow in Long.MIN_VALUE until Long.MAX_VALUE) {
                batteryCurrentNow.toFloat() / Config.BatteryElectricCurrent.toFloat()
            } else {
                Float.NaN
            },
            voltage = voltage / 1_000_000.0f,
            temperature = batteryStatus.temperature,
            androidVersion = Build.VERSION.RELEASE,
            androidSDK = Build.VERSION.SDK_INT,
            runningDuration = SystemClock.elapsedRealtime().formatUptimeHour(),
        )
    }

    private val processUtils = ProcessUtils()

    suspend fun loadRunningServicesDetailState(): Unit = withContext(Dispatchers.IO) {
        if (processUtils.supported(context)) {
            _runningServicesDetailState.value =
                processUtils.getAllProcess().sortedBy { it.cpu }.reversed()
        }
    }
}