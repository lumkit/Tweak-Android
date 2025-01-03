package io.github.lumkit.tweak.common.shell

import io.github.lumkit.tweak.common.shell.io.impl.RootFile
import io.github.lumkit.tweak.common.shell.provide.ReusableShells
import io.github.lumkit.tweak.model.CpuStatus

object ThermalControlUtils {

    // Thermal file paths
    private const val THERMAL_CORE_CONTROL = "/sys/module/msm_thermal/core_control/enabled" // "1" or "0"
    private const val THERMAL_VDD_RESTRICTION = "/sys/module/msm_thermal/vdd_restriction/enabled" // "1" or "0"
    private const val THERMAL_PARAMETERS = "/sys/module/msm_thermal/parameters/enabled" // "Y" or "N"

    /**
     * 检查设备是否支持热管理功能。
     */
    suspend fun isSupported(): Boolean {
        return RootFile(THERMAL_CORE_CONTROL).exists() ||
                RootFile(THERMAL_VDD_RESTRICTION).exists() ||
                RootFile(THERMAL_PARAMETERS).exists()
    }

    /**
     * 获取核心控制状态。
     */
    suspend fun getCoreControlState(): String {
        return KernelProp.getProp(THERMAL_CORE_CONTROL).trim()
    }

    /**
     * 设置核心控制状态。
     */
    suspend fun setCoreControlState(online: Boolean) {
        setThermalState(THERMAL_CORE_CONTROL, if (online) "1" else "0")
    }

    /**
     * 获取VDD限制状态。
     */
    suspend fun getVDDRestrictionState(): String {
        return KernelProp.getProp(THERMAL_VDD_RESTRICTION).trim()
    }

    /**
     * 设置VDD限制状态。
     */
    suspend fun setVDDRestrictionState(online: Boolean) {
        setThermalState(THERMAL_VDD_RESTRICTION, if (online) "1" else "0")
    }

    /**
     * 获取热管理参数状态。
     */
    suspend fun getThermalState(): String {
        return KernelProp.getProp(THERMAL_PARAMETERS).trim()
    }

    /**
     * 设置热管理参数状态。
     */
    suspend fun setThermalState(online: Boolean) {
        setThermalState(THERMAL_PARAMETERS, if (online) "Y" else "N")
    }

    /**
     * 构建设置热管理参数的命令列表。
     */
    fun buildSetThermalParams(cpuStatus: CpuStatus): List<String> {
        val commands = mutableListOf<String>()

        cpuStatus.coreControl.takeIf { it.isNotBlank() }?.let {
            commands += buildSetCommand(THERMAL_CORE_CONTROL, it)
        }

        cpuStatus.vdd.takeIf { it.isNotBlank() }?.let {
            commands += buildSetCommand(THERMAL_VDD_RESTRICTION, it)
        }

        cpuStatus.msmThermal.takeIf { it.isNotBlank() }?.let {
            commands += buildSetCommand(THERMAL_PARAMETERS, it)
        }

        return commands
    }

    /**
     * 私有辅助方法：设置热管理参数状态。
     */
    private suspend fun setThermalState(path: String, value: String) {
        val commands = listOf(
            "chmod 0664 $path",
            "echo $value > $path"
        )
        ReusableShells.execSync(commands)
    }

    /**
     * 私有辅助方法：构建设置命令。
     */
    private fun buildSetCommand(path: String, value: String): List<String> {
        return listOf(
            "chmod 0664 $path",
            "echo $value > $path"
        )
    }
}
