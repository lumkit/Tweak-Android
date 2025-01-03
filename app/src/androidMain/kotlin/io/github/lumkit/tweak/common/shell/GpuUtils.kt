package io.github.lumkit.tweak.common.shell

import io.github.lumkit.tweak.common.shell.io.impl.RootFile
import io.github.lumkit.tweak.common.shell.provide.ReusableShells
import io.github.lumkit.tweak.common.util.autoUnit
import io.github.lumkit.tweak.common.util.firstLine
import io.github.lumkit.tweak.model.CpuStatus
import kotlinx.coroutines.runBlocking
import java.io.File

object GpuUtils {

    private const val GPU_MEMORY_CMD1 =
        "cat /proc/mali/memory_usage | grep \"Total\" | cut -f2 -d \"(\" | cut -f1 -d \" \""
    private const val GPU_PARAMS_DIR_ADRENO = "/sys/class/kgsl/kgsl-3d0"
    private const val GPU_PARAMS_DIR_MALI = "/sys/class/devfreq/gpufreq"

    private var platform: String? = null
    private var kgsGM: Boolean = true

    @Volatile
    private var isAdrenoGPU: Boolean? = null

    @Volatile
    private var isMaliGPU: Boolean? = null

    @Volatile
    private var gpuParamsDir: String? = null

    @Volatile
    private var gpuLoadPath: String? = null

    @Volatile
    private var gpuFreqCmd: String? = null

    /**
     * 判断是否为 MTK 平台
     */
    private fun isMTK(): Boolean {
        if (platform == null) {
            platform = PlatformUtils.getCPUName()
        }
        return platform?.startsWith("mt", ignoreCase = true) ?: false
    }

    /**
     * 获取 GPU 内存使用情况
     */
    suspend fun getMemoryUsage(): String? {
        return if (isMTK()) {
            val bytes = ReusableShells.execSync(GPU_MEMORY_CMD1)
            try {
                bytes.toLong().autoUnit()
            } catch (ex: Exception) {
                "N/A"
            }
        } else if (kgsGM) {
            val bytes = ReusableShells.execSync("cat /sys/devices/virtual/kgsl/kgsl-3d0/page_alloc")
            try {
                bytes.toLong().autoUnit()
            } catch (ex: Exception) {
                kgsGM = false
                null
            }
        } else {
            null
        }
    }

    /**
     * 获取 GPU 频率
     */
    suspend fun getGpuFreq(): String {
        if (gpuFreqCmd == null) {
            gpuFreqCmd = when {
                RootFile("$GPU_PARAMS_DIR_ADRENO/cur_freq").exists() ->
                    "cat $GPU_PARAMS_DIR_ADRENO/cur_freq"

                RootFile("/sys/kernel/gpu/gpu_clock").exists() ->
                    "cat /sys/kernel/gpu/gpu_clock"

                RootFile("/sys/kernel/debug/ged/hal/current_freqency").exists() ->
                    "echo \$((`cat /sys/kernel/debug/ged/hal/current_freqency | cut -f2 -d ' '` / 1000))"

                RootFile("/sys/kernel/ged/hal/current_freqency").exists() ->
                    "echo \$((`cat /sys/kernel/ged/hal/current_freqency | cut -f2 -d ' '` / 1000))"
                else -> ""
            }
        }

        return if (gpuFreqCmd.isNullOrEmpty()) {
            ""
        } else {
            val freq = ReusableShells.execSync(gpuFreqCmd!!).firstLine()
            if (freq.length > 6) freq.substring(0, freq.length - 6) else freq
        }
    }

    suspend fun gles(): String {
        val gLES = ReusableShells.execSync("dumpsys SurfaceFlinger | grep -i GLES")
        return if (gLES.contains("GLES:")) {
            gLES.substring(gLES.lastIndexOf("GLES:") + 5).trim()
        } else {
            gLES
        }
    }

    /**
     * 获取 GPU 负载
     */
    suspend fun getGpuLoad(): Int {
        if (gpuLoadPath == null) {
            val paths = listOf(
                "/sys/kernel/gpu/gpu_busy",
                "/sys/class/kgsl/kgsl-3d0/devfreq/gpu_load",
                "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
                "/sys/class/kgsl/kgsl-3d0/gpuload",
                "/sys/class/devfreq/gpufreq/mali_ondemand/utilisation",
                "/sys/kernel/debug/ged/hal/gpu_utilization",
                "/sys/kernel/ged/hal/gpu_utilization",
                "/sys/module/ged/parameters/gpu_loading"
            )
            gpuLoadPath = paths.find { RootFile(it).exists() } ?: ""
        }

        return if (gpuLoadPath.isNullOrEmpty()) {
            -1
        } else {
            val load = KernelProp.getProp(gpuLoadPath!!)
            try {
                load.replace("%", "").trim().split(" ").firstOrNull()?.toInt() ?: -1
            } catch (ex: Exception) {
                -1
            }
        }
    }

    /**
     * 获取可用频率
     */
    suspend fun getAvailableFreqs(): Array<String> {
        val freqs = KernelProp.getProp("${getGpuParamsDir()}/available_frequencies")
        return if (freqs.isEmpty()) emptyArray() else freqs.split("\\s+".toRegex()).toTypedArray()
    }

    /**
     * 获取频率表（仅 Adreno）
     */
    suspend fun getFreqTableMhz(): Array<String> {
        return if (isAdrenoGPU()) {
            val freqs = KernelProp.getProp("$GPU_PARAMS_DIR_ADRENO/freq_table_mhz")
            if (freqs.isNotEmpty()) freqs.split("\\s+".toRegex()).toTypedArray() else emptyArray()
        } else {
            emptyArray()
        }
    }

    /**
     * 检查是否支持 GPU 操作
     */
    suspend fun supported(): Boolean = isAdrenoGPU() || isMaliGPU()

    /**
     * 判断是否为 Adreno GPU
     */
    suspend fun isAdrenoGPU(): Boolean {
        if (isAdrenoGPU == null) {
            isAdrenoGPU = File(GPU_PARAMS_DIR_ADRENO).exists() || RootFile(GPU_PARAMS_DIR_ADRENO).exists()
        }
        return isAdrenoGPU ?: false
    }

    /**
     * 判断是否为 Mali GPU
     */
    suspend fun isMaliGPU(): Boolean {
        if (isMaliGPU == null) {
            isMaliGPU = File(GPU_PARAMS_DIR_MALI).exists() || RootFile(GPU_PARAMS_DIR_MALI).exists()
        }
        return isMaliGPU ?: false
    }

    /**
     * 获取 GPU 参数目录
     */
    private suspend fun getGpuParamsDir(): String {
        if (gpuParamsDir == null) {
            gpuParamsDir = when {
                isAdrenoGPU() -> "$GPU_PARAMS_DIR_ADRENO/devfreq"
                isMaliGPU() -> GPU_PARAMS_DIR_MALI
                else -> ""
            }
        }
        return gpuParamsDir ?: ""
    }

    /**
     * 获取可用治理器（Governors）
     */
    suspend fun getGovernors(): Array<String> {
        val governors = KernelProp.getProp("${getGpuParamsDir()}/available_governors")
        return if (governors.isEmpty()) emptyArray() else governors.split("\\s+".toRegex()).toTypedArray()
    }

    /**
     * 获取最小频率
     */
    suspend fun getMinFreq(): String = KernelProp.getProp("${getGpuParamsDir()}/min_freq")

    /**
     * 设置最小频率
     */
    suspend fun setMinFreq(value: String) {
        val commands = listOf(
            "chmod 0664 ${getGpuParamsDir()}/min_freq",
            "echo $value > ${getGpuParamsDir()}/min_freq"
        )
        ReusableShells.execSync(commands)
    }

    /**
     * 获取最大频率
     */
    suspend fun getMaxFreq(): String = KernelProp.getProp("${getGpuParamsDir()}/max_freq")

    /**
     * 设置最大频率
     */
    suspend fun setMaxFreq(value: String) {
        val commands = listOf(
            "chmod 0664 ${getGpuParamsDir()}/max_freq",
            "echo $value > ${getGpuParamsDir()}/max_freq"
        )
        ReusableShells.execSync(commands)
    }

    /**
     * 获取当前治理器
     */
    suspend fun getGovernor(): String = KernelProp.getProp("${getGpuParamsDir()}/governor")

    /**
     * 设置治理器
     */
    suspend fun setGovernor(value: String) {
        val commands = listOf(
            "chmod 0664 ${getGpuParamsDir()}/governor",
            "echo $value > ${getGpuParamsDir()}/governor"
        )
        ReusableShells.execSync(commands)
    }

    // #region Adreno GPU Power Level
    /**
     * 获取 Adreno GPU 最小功率级别
     */
    suspend fun getAdrenoGPUMinPowerLevel(): String =
        KernelProp.getProp("$GPU_PARAMS_DIR_ADRENO/min_pwrlevel")

    /**
     * 设置 Adreno GPU 最小功率级别
     */
    suspend fun setAdrenoGPUMinPowerLevel(value: String) {
        val commands = listOf(
            "chmod 0664 $GPU_PARAMS_DIR_ADRENO/min_pwrlevel",
            "echo $value > $GPU_PARAMS_DIR_ADRENO/min_pwrlevel"
        )
        ReusableShells.execSync(commands)
    }

    /**
     * 获取 Adreno GPU 最大功率级别
     */
    suspend fun getAdrenoGPUMaxPowerLevel(): String =
        KernelProp.getProp("$GPU_PARAMS_DIR_ADRENO/max_pwrlevel")

    /**
     * 设置 Adreno GPU 最大功率级别
     */
    suspend fun setAdrenoGPUMaxPowerLevel(value: String) {
        val commands = listOf(
            "chmod 0664 $GPU_PARAMS_DIR_ADRENO/max_pwrlevel",
            "echo $value > $GPU_PARAMS_DIR_ADRENO/max_pwrlevel"
        )
        ReusableShells.execSync(commands)
    }

    /**
     * 获取 Adreno GPU 默认功率级别
     */
    suspend fun getAdrenoGPUDefaultPowerLevel(): String =
        KernelProp.getProp("$GPU_PARAMS_DIR_ADRENO/default_pwrlevel")

    /**
     * 设置 Adreno GPU 默认功率级别
     */
    suspend fun setAdrenoGPUDefaultPowerLevel(value: String) {
        val commands = listOf(
            "chmod 0664 $GPU_PARAMS_DIR_ADRENO/default_pwrlevel",
            "echo $value > $GPU_PARAMS_DIR_ADRENO/default_pwrlevel"
        )
        ReusableShells.execSync(commands)
    }

    /**
     * 获取 Adreno GPU 支持的功率级别
     */
    suspend fun getAdrenoGPUPowerLevels(): Array<String> {
        val levels = KernelProp.getProp("$GPU_PARAMS_DIR_ADRENO/num_pwrlevels")
        return try {
            val max = levels.toInt()
            Array(max) { it.toString() }
        } catch (ignored: Exception) {
            emptyArray()
        }
    }

    // #endregion Adreno GPU Power Level

    /**
     * 构建设置 Adreno GPU 参数的命令列表
     */
    suspend fun buildSetAdrenoGPUParams(cpuState: CpuStatus): List<String> {
        val commands = mutableListOf<String>()

        // 设置治理器
        cpuState.adrenoGovernor.takeIf { it.isNotEmpty() }?.let {
            commands += "chmod 0664 ${getGpuParamsDir()}/governor"
            commands += "echo $it > ${getGpuParamsDir()}/governor"
        }

        // 设置最小频率
        cpuState.adrenoMinFreq.takeIf { it.isNotEmpty() }?.let {
            commands += "chmod 0664 ${getGpuParamsDir()}/min_freq"
            commands += "echo $it > ${getGpuParamsDir()}/min_freq"
        }

        // 设置最大频率
        cpuState.adrenoMaxFreq.takeIf { it.isNotEmpty() }?.let {
            commands += "chmod 0664 ${getGpuParamsDir()}/max_freq"
            commands += "echo $it > ${getGpuParamsDir()}/max_freq"
        }

        // 设置最小功率级别
        cpuState.adrenoMinPL.takeIf { it.isNotEmpty() }?.let {
            commands += "chmod 0664 $GPU_PARAMS_DIR_ADRENO/min_pwrlevel"
            commands += "echo $it > $GPU_PARAMS_DIR_ADRENO/min_pwrlevel"
        }

        // 设置最大功率级别
        cpuState.adrenoMaxPL.takeIf { it.isNotEmpty() }?.let {
            commands += "chmod 0664 $GPU_PARAMS_DIR_ADRENO/max_pwrlevel"
            commands += "echo $it > $GPU_PARAMS_DIR_ADRENO/max_pwrlevel"
        }

        // 设置默认功率级别
        cpuState.adrenoDefaultPL.takeIf { it.isNotEmpty() }?.let {
            commands += "chmod 0664 $GPU_PARAMS_DIR_ADRENO/default_pwrlevel"
            commands += "echo $it > $GPU_PARAMS_DIR_ADRENO/default_pwrlevel"
        }

        return commands
    }
}
