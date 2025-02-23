package io.github.lumkit.tweak.common.shell

import io.github.lumkit.tweak.common.TweakNative
import io.github.lumkit.tweak.common.shell.provide.ReusableShells
import io.github.lumkit.tweak.model.CpuStatus
import java.io.File

object CpuFrequencyUtils {

    private val platform: String by lazy { PlatformUtils.getCPUName() }
    private const val CPU_DIR = "/sys/devices/system/cpu/cpu0/"
    private const val CPUFREQ_SYS_DIR = "/sys/devices/system/cpu/cpu0/cpufreq/"
    private const val SCALING_MIN_FREQ = "${CPUFREQ_SYS_DIR}scaling_min_freq"
    private const val SCALING_CUR_FREQ = "${CPUFREQ_SYS_DIR}scaling_cur_freq"
    private const val SCALING_MAX_FREQ = "${CPUFREQ_SYS_DIR}scaling_max_freq"
    private const val SCALING_GOVERNOR = "${CPUFREQ_SYS_DIR}scaling_governor"

    private val cpuClusterInfoLoading = Any()
    private var cpuClusterInfo: List<Array<String>> = emptyList()
    private var coreCount: Int = -1

    private fun isMTK(): Boolean {
        return platform.startsWith("mt", ignoreCase = true)
    }

    private fun getCpuFreqValue(path: String): String {
        val freqValue = TweakNative.getKernelPropLong(path)
        return if (freqValue > -1) freqValue.toString() else ""
    }

    suspend fun getAvailableFrequencies(cluster: Int): Array<String> {
        val clusters = getClusterInfo()
        if (cluster >= clusters.size) return emptyArray()

        val cpu = "cpu${clusters[cluster][0]}"
        val scalingAvailableFreq = "${CPUFREQ_SYS_DIR}scaling_available_frequencies"
        val alternativePath =
            "/sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster${cluster}_freq_table"

        val scalingAvailableFreqPath = scalingAvailableFreq.replace("cpu0", cpu)
        val frequencies = when {
            File(scalingAvailableFreqPath).exists() -> {
                KernelProp.getProp(scalingAvailableFreqPath).split("\\s+".toRegex()).toTypedArray()
            }

            File(alternativePath).exists() -> {
                KernelProp.getProp(alternativePath).split("\\s+".toRegex()).toTypedArray()
            }

            else -> emptyArray()
        }
        return frequencies
    }

    suspend fun getCurrentMaxFrequency(cluster: Int): String {
        val clusters = getClusterInfo()
        if (cluster >= clusters.size) return ""
        val cpu = "cpu${clusters[cluster][0]}"
        return KernelProp.getProp(SCALING_MAX_FREQ.replace("cpu0", cpu))
    }

    suspend fun getCurrentMaxFrequency(core: String): String {
        return KernelProp.getProp(SCALING_MAX_FREQ.replace("cpu0", core))
    }

    suspend fun getCurrentFrequency(cluster: Int): String {
        val clusters = getClusterInfo()
        if (cluster >= clusters.size) return ""
        val cpu = "cpu${clusters[cluster][0]}"
        return getCpuFreqValue(SCALING_CUR_FREQ.replace("cpu0", cpu))
    }

    suspend fun getCurrentFrequency(cpu: String): String {
        return getCpuFreqValue(SCALING_CUR_FREQ.replace("cpu0", cpu))
    }

    suspend fun getCurrentMinFrequency(cluster: Int): String {
        val clusters = getClusterInfo()
        if (cluster >= clusters.size) return ""
        val cpu = "cpu${clusters[cluster][0]}"
        return KernelProp.getProp(SCALING_MIN_FREQ.replace("cpu0", cpu))
    }

    suspend fun getCurrentMinFrequency(core: String): String {
        return KernelProp.getProp(SCALING_MIN_FREQ.replace("cpu0", core))
    }

    suspend fun getAvailableGovernors(cluster: Int): Array<String> {
        val clusters = getClusterInfo()
        if (cluster >= clusters.size) return emptyArray()
        val cpu = "cpu${clusters[cluster][0]}"
        val scalingAvailableGovernors = "${CPUFREQ_SYS_DIR}scaling_available_governors"
        val governors = KernelProp.getProp(scalingAvailableGovernors.replace("cpu0", cpu))
        return if (governors.isEmpty()) emptyArray() else governors.split("\\s+".toRegex())
            .toTypedArray()
    }

    suspend fun getCurrentScalingGovernor(cluster: Int): String {
        val clusters = getClusterInfo()
        if (cluster >= clusters.size) return ""
        val cpu = "cpu${clusters[cluster][0]}"
        return KernelProp.getProp(SCALING_GOVERNOR.replace("cpu0", cpu))
    }

    suspend fun getCurrentScalingGovernor(core: String): String {
        return KernelProp.getProp(SCALING_GOVERNOR.replace("cpu0", core))
    }

    suspend fun getCurrentScalingGovernorParams(cluster: Int): Map<String, String>? {
        val clusters = getClusterInfo()
        if (cluster >= clusters.size) return null
        val cpu = "cpu${clusters[cluster][0]}"
        val governor = getCurrentScalingGovernor(cpu)
        return FileValueMap.mapFileValue("${CPU_DIR.replace("cpu0", cpu)}cpufreq/$governor")
    }

    suspend fun getCoreGovernorParams(cluster: Int): Map<String, String> {
        val cpu = "cpu$cluster"
        val governor = getCurrentScalingGovernor(cpu)
        return FileValueMap.mapFileValue("${CPU_DIR.replace("cpu0", cpu)}cpufreq/$governor")
    }

    suspend fun setMinFrequency(minFrequency: String, cluster: Int) {
        val clusters = getClusterInfo()
        if (cluster >= clusters.size) return

        if (isMTK()) {
            val command =
                "echo $cluster $minFrequency > /proc/ppm/policy/hard_userlimit_min_cpu_freq"
            ReusableShells.execSync(command)
        } else {
            val cores = clusters[cluster]
            if (minFrequency.isNotEmpty()) {
                val commands = ArrayList<String>()
                for (core in cores) {
                    commands.add("chmod 0664 " + SCALING_MIN_FREQ.replace("cpu0", "cpu$core"))
                    commands.add(
                        "echo $minFrequency > " + SCALING_MIN_FREQ.replace(
                            "cpu0",
                            "cpu$core"
                        )
                    )
                }
                ReusableShells.execSync(commands)
            }
        }
    }

    suspend fun setMaxFrequency(maxFrequency: String, cluster: Int) {
        val clusters = getClusterInfo()
        if (cluster >= clusters.size) return

        if (isMTK()) {
            val command =
                "echo $cluster $maxFrequency > /proc/ppm/policy/hard_userlimit_max_cpu_freq"
            ReusableShells.execSync(command)
        } else {
            val cores = clusters[cluster]
            if (maxFrequency.isNotEmpty()) {
                val commands = mutableListOf<String>(
                    "chmod 0664 /sys/module/msm_performance/parameters/cpu_max_freq"
                )
                val freqBuilder = StringBuilder()
                cores.forEach { core ->
                    val scalingMaxFreq = SCALING_MAX_FREQ.replace("cpu0", "cpu$core")
                    commands += listOf(
                        "chmod 0664 $scalingMaxFreq",
                        "echo $maxFrequency > $scalingMaxFreq"
                    )
                    freqBuilder.append("$core:$maxFrequency ")
                }
                commands += "echo ${
                    freqBuilder.toString().trim()
                } > /sys/module/msm_performance/parameters/cpu_max_freq"
                ReusableShells.execSync(commands)
            }
        }
    }

    suspend fun setGovernor(governor: String, cluster: Int) {
        val clusters = getClusterInfo()
        if (cluster >= clusters.size) return
        val cores = clusters[cluster]
        if (governor.isNotEmpty()) {
            val commands = cores.flatMap { core ->
                listOf(
                    "chmod 0755 ${SCALING_GOVERNOR.replace("cpu0", "cpu$core")}",
                    "echo $governor > ${SCALING_GOVERNOR.replace("cpu0", "cpu$core")}"
                )
            }
            ReusableShells.execSync(commands)
        }
    }

    suspend fun getCoreOnlineState(coreIndex: Int): Boolean {
        val path = "/sys/devices/system/cpu/cpu$coreIndex/online"
        return KernelProp.getProp(path).trim() == "1"
    }

    suspend fun setCoreOnlineState(coreIndex: Int, online: Boolean) {
        val commands = mutableListOf<String>()
        if (exynosCpuhotplugSupport() && getExynosHotplug()) {
            commands += "echo 0 > /sys/devices/system/cpu/cpuhotplug/enabled"
        }
        val onlinePath = "/sys/devices/system/cpu/cpu$coreIndex/online"
        commands += listOf(
            "chmod 0755 $onlinePath",
            "echo ${if (online) "1" else "0"} > $onlinePath"
        )
        ReusableShells.execSync(commands)
    }

    suspend fun getExynosHmpUP(): Int {
        val up = KernelProp.getProp("/sys/kernel/hmp/up_threshold").trim()
        return up.toIntOrNull() ?: 0
    }

    suspend fun setExynosHmpUP(up: Int) {
        val commands = listOf(
            "chmod 0664 /sys/kernel/hmp/up_threshold",
            "echo $up > /sys/kernel/hmp/up_threshold"
        )
        ReusableShells.execSync(commands)
    }

    suspend fun getExynosHmpDown(): Int {
        val down = KernelProp.getProp("/sys/kernel/hmp/down_threshold").trim()
        return down.toIntOrNull() ?: 0
    }

    suspend fun setExynosHmpDown(down: Int) {
        val commands = listOf(
            "chmod 0664 /sys/kernel/hmp/down_threshold",
            "echo $down > /sys/kernel/hmp/down_threshold"
        )
        ReusableShells.execSync(commands)
    }

    suspend fun getExynosBooster(): Boolean {
        val value = KernelProp.getProp("/sys/kernel/hmp/boost").trim().lowercase()
        return value == "1" || value == "true" || value == "enabled"
    }

    suspend fun setExynosBooster(booster: Boolean) {
        val commands = listOf(
            "chmod 0664 /sys/kernel/hmp/boost",
            "echo ${if (booster) "1" else "0"} > /sys/kernel/hmp/boost"
        )
        ReusableShells.execSync(commands)
    }

    suspend fun getExynosHotplug(): Boolean {
        val value =
            KernelProp.getProp("/sys/devices/system/cpu/cpuhotplug/enabled").trim().lowercase()
        return value == "1" || value == "true" || value == "enabled"
    }

    suspend fun setExynosHotplug(hotplug: Boolean) {
        val commands = listOf(
            "chmod 0664 /sys/devices/system/cpu/cpuhotplug/enabled",
            "echo ${if (hotplug) "1" else "0"} > /sys/devices/system/cpu/cpuhotplug/enabled"
        )
        ReusableShells.execSync(commands)
    }

    suspend fun getCoreCount(): Int {
        if (coreCount > -1) return coreCount
        var cores = 0
        while (File(CPU_DIR.replace("cpu0", "cpu$cores")).exists()) {
            cores++
        }
        coreCount = cores
        return coreCount
    }

    private suspend fun loadClusterInfo(): MutableList<Array<String>> {
        val clusters = mutableListOf<String>()
        var cores = 0
        while (true) {
            val path = "/sys/devices/system/cpu/cpu$cores/cpufreq/related_cpus"
            val file = File(path)
            if (file.exists()) {
                val relatedCpus = KernelProp.getProp(path).trim()
                if (relatedCpus.isNotEmpty() && !clusters.contains(relatedCpus)) {
                    clusters += relatedCpus
                }
            } else {
                break
            }
            cores++
        }
        return clusters.map { it.split("\\s+".toRegex()).toTypedArray() }.toMutableList()
    }

    suspend fun getClusterInfo(): List<Array<String>> {
        if (cpuClusterInfo.isNotEmpty()) {
            return cpuClusterInfo
        }
        var cores = 0
        val clusterInfo = mutableListOf<Array<String>>()
        val clusters = mutableListOf<String>()
        while (true) {
            val file = File("/sys/devices/system/cpu/cpu0/cpufreq/related_cpus".replace("cpu0", "cpu$cores"))
            if (file.exists()) {
                val relatedCpus: String = KernelProp.getProp("/sys/devices/system/cpu/cpu0/cpufreq/related_cpus".replace("cpu0", "cpu$cores")).trim()
                if (!clusters.contains(relatedCpus) && relatedCpus.isNotEmpty()) {
                    clusters.add(relatedCpus)
                }
            } else {
                break
            }
            cores++
        }
        for (i in clusters.indices) {
            clusterInfo.add(
                clusters[i].split("[ ]+".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray())
        }
        cpuClusterInfo = clusterInfo
        return cpuClusterInfo
    }

    fun exynosCpuhotplugSupport(): Boolean {
        return File("/sys/devices/system/cpu/cpuhotplug").exists()
    }

    fun exynosHMP(): Boolean {
        return listOf(
            "/sys/kernel/hmp/down_threshold",
            "/sys/kernel/hmp/up_threshold",
            "/sys/kernel/hmp/boost"
        ).all { File(it).exists() }
    }

    suspend fun buildShell(cpuStatus: CpuStatus): List<String> {
        val commands = mutableListOf<String>()
        cpuStatus.let {
            // Thermal Control
            commands += ThermalControlUtils.buildSetThermalParams(it)

            // Core Online
            it.coreOnline?.let { coreStates ->
                if (coreStates.isNotEmpty()) {
                    if (exynosCpuhotplugSupport() && getExynosHotplug()) {
                        commands += "echo 0 > /sys/devices/system/cpu/cpuhotplug/enabled"
                    }
                    coreStates.forEachIndexed { index, online ->
                        val path = "/sys/devices/system/cpu/cpu$index/online"
                        if (exynosCpuhotplugSupport() && getExynosHotplug()) {
                            commands += listOf(
                                "chmod 0755 $path",
                                "echo ${if (online) "1" else "0"} > $path"
                            )
                        }
                    }
                }
            }

            // CPU Cluster Statuses
            it.cpuClusterStatuses.takeIf { it.isNotEmpty() }?.let { params ->
                if (params.size <= getClusterInfo().size) {
                    if (isMTK()) {
                        params.forEachIndexed { cluster, config ->
                            commands += listOf(
                                "echo $cluster ${config.minFreq} > /proc/ppm/policy/hard_userlimit_min_cpu_freq",
                                "echo $cluster ${config.maxFreq} > /proc/ppm/policy/hard_userlimit_max_cpu_freq"
                            )
                        }
                    } else {
                        params.forEachIndexed { cluster, config ->
                            val cores = getClusterInfo()[cluster]
                            if (config.governor.isNotEmpty()) {
                                cores.forEach { core ->
                                    val governorPath = SCALING_GOVERNOR.replace("cpu0", "cpu$core")
                                    commands += listOf(
                                        "chmod 0755 $governorPath",
                                        "echo ${config.governor} > $governorPath"
                                    )
                                }
                            }
                            if (config.maxFreq.isNotEmpty()) {
                                commands += listOf(
                                    "chmod 0664 /sys/module/msm_performance/parameters/cpu_max_freq"
                                )
                                val freqBuilder = StringBuilder()
                                cores.forEach { core ->
                                    val maxFreqPath = SCALING_MAX_FREQ.replace("cpu0", "cpu$core")
                                    commands += listOf(
                                        "chmod 0664 $maxFreqPath",
                                        "echo ${config.maxFreq} > $maxFreqPath"
                                    )
                                    freqBuilder.append("$core:${config.maxFreq} ")
                                }
                                commands += "echo ${
                                    freqBuilder.toString().trim()
                                } > /sys/module/msm_performance/parameters/cpu_max_freq"
                            }
                            if (config.minFreq.isNotEmpty()) {
                                cores.forEach { core ->
                                    val minFreqPath = SCALING_MIN_FREQ.replace("cpu0", "cpu$core")
                                    commands += listOf(
                                        "chmod 0664 $minFreqPath",
                                        "echo ${config.minFreq} > $minFreqPath"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // GPU Settings
            commands += GpuUtils.buildSetAdrenoGPUParams(it)

            // Exynos Settings
            if (exynosHMP()) {
                commands += listOf(
                    "chmod 0664 /sys/devices/system/cpu/cpuhotplug/enabled",
                    "echo ${if (it.exynosHotplug) "1" else "0"} > /sys/devices/system/cpu/cpuhotplug/enabled",
                    "chmod 0664 /sys/kernel/hmp/down_threshold",
                    "echo ${it.exynosHmpDown} > /sys/kernel/hmp/down_threshold",
                    "chmod 0664 /sys/kernel/hmp/up_threshold",
                    "echo ${it.exynosHmpUP} > /sys/kernel/hmp/up_threshold",
                    "chmod 0664 /sys/kernel/hmp/boost",
                    "echo ${if (it.exynosHmpBooster) "1" else "0"} > /sys/kernel/hmp/boost"
                )
            }

            // Cpuset Settings
            listOf(
                it.cpusetBackground to "/dev/cpuset/background/cpus",
                it.cpusetSysBackground to "/dev/cpuset/system-background/cpus",
                it.cpusetForeground to "/dev/cpuset/foreground/cpus",
                it.cpusetRestricted to "/dev/cpuset/restricted/cpus",
                it.cpusetTopApp to "/dev/cpuset/top-app/cpus"
            ).forEach { (value, path) ->
                if (value.isNotEmpty()) {
                    commands += "echo $value > $path"
                }
            }

        }
        return commands
    }
}
