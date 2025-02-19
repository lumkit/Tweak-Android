package io.github.lumkit.tweak.common.shell

import android.content.Context
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.shell.io.impl.RootFile
import io.github.lumkit.tweak.common.shell.provide.ReusableShells
import io.github.lumkit.tweak.model.Config
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.math.pow

data class BatteryStatus(
    var statusText: String = "",
    var level: Int = 0,
    var temperature: Float = 0.0f
)

object BatteryUtils {
    private var fastChargeScript: String = ""

    @Volatile
    private var changeLimitRunning: Boolean = false
    private var isFirstRun: Boolean = true
    private var useMainConstant: Boolean? = null
    private var kernelCapacitySupported: Boolean? = null

    /**
     * 获取电池温度及其他基本信息
     */
    suspend fun getBatteryStatus(): BatteryStatus {
        val batteryInfo = ReusableShells.execSync("dumpsys battery") ?: ""
        val batteryStatus = BatteryStatus()
        val seenKeys = mutableSetOf<String>()

        batteryInfo.lines().forEach { line ->
            val (key, value) = line.split(":", limit = 2).map { it.trim() }.takeIf { it.size == 2 }
                ?: return@forEach
            if (key in seenKeys) return@forEach

            when (key) {
                "status" -> {
                    batteryStatus.statusText = value
                    seenKeys.add(key)
                }

                "level" -> {
                    batteryStatus.level = value.toIntOrNull() ?: 0
                    seenKeys.add(key)
                }

                "temperature" -> {
                    batteryStatus.temperature = (value.toFloatOrNull() ?: 0.0f) / 10.0f
                    seenKeys.add(key)
                }
            }
        }
        return batteryStatus
    }

    /**
     * 将字符串转换为电压格式
     */
    private fun strToVoltage(str: String): String {
        val numericValue = str.take(4).toDoubleOrNull() ?: return "?v"
        val voltage = when {
            numericValue > 3000 -> numericValue / 1000
            numericValue > 300 -> numericValue / 100
            numericValue > 30 -> numericValue / 10
            else -> numericValue
        }
        return "${voltage}v"
    }

    /**
     * 读取并解析电池信息
     */
    val batteryInfo: String
        get() {
            val paths = listOf(
                "/sys/class/power_supply/bms/uevent",
                "/sys/class/power_supply/battery/uevent"
            )
            val path = paths.firstOrNull { runBlocking { RootFile(it).exists() } } ?: return ""

            val batteryInfos = runBlocking { KernelProp.getProp(path) }
            val infos = batteryInfos.lines()
            val stringBuilder = StringBuilder()
            var io = ""
            var mahLength = 0

            infos.forEach { info ->
                try {
                    when {
                        info.startsWith("POWER_SUPPLY_CHARGE_FULL=") -> {
                            val value = info.substringAfter('=').take(4)
                            stringBuilder.append("充满容量 = ${value}mAh\n")
                            mahLength = info.substringAfter('=').length
                        }

                        info.startsWith("POWER_SUPPLY_CHARGE_FULL_DESIGN=") -> {
                            val value = info.substringAfter('=').take(4)
                            stringBuilder.append("设计容量 = ${value}mAh\n")
                            mahLength = info.substringAfter('=').length
                        }

                        info.startsWith("POWER_SUPPLY_TEMP=") -> {
                            val tempStr = info.substringAfter('=').toIntOrNull() ?: 0
                            val temp = tempStr / 10f
                            stringBuilder.append("电池温度 = ${temp}°C\n")
                        }

                        info.startsWith("POWER_SUPPLY_TEMP_WARM=") -> {
                            val temp = info.substringAfter('=').toIntOrNull() ?: 0
                            stringBuilder.append("警戒温度 = ${temp / 10}°C\n")
                        }

                        info.startsWith("POWER_SUPPLY_TEMP_COOL=") -> {
                            val temp = info.substringAfter('=').toIntOrNull() ?: 0
                            stringBuilder.append("低温温度 = ${temp / 10}°C\n")
                        }

                        info.startsWith("POWER_SUPPLY_VOLTAGE_NOW=") -> {
                            val voltage = strToVoltage(info.substringAfter('='))
                            stringBuilder.append("当前电压 = ${voltage}\n")
                        }

                        info.startsWith("POWER_SUPPLY_VOLTAGE_MAX_DESIGN=") -> {
                            val voltage = strToVoltage(info.substringAfter('='))
                            stringBuilder.append("设计电压 = ${voltage}\n")
                        }

                        info.startsWith("POWER_SUPPLY_VOLTAGE_MIN=") -> {
                            val voltage = strToVoltage(info.substringAfter('='))
                            stringBuilder.append("最小电压 = ${voltage}\n")
                        }

                        info.startsWith("POWER_SUPPLY_VOLTAGE_MAX=") -> {
                            val voltage = strToVoltage(info.substringAfter('='))
                            stringBuilder.append("最大电压 = ${voltage}\n")
                        }

                        info.startsWith("POWER_SUPPLY_BATTERY_TYPE=") -> {
                            val type = info.substringAfter('=')
                            stringBuilder.append("电池类型 = ${type}\n")
                        }

                        info.startsWith("POWER_SUPPLY_TECHNOLOGY=") -> {
                            val tech = info.substringAfter('=')
                            stringBuilder.append("电池技术 = ${tech}\n")
                        }

                        info.startsWith("POWER_SUPPLY_CYCLE_COUNT=") -> {
                            val count = info.substringAfter('=')
                            stringBuilder.append("循环次数 = ${count}\n")
                        }

                        info.startsWith("POWER_SUPPLY_CONSTANT_CHARGE_VOLTAGE=") -> {
                            val voltage = strToVoltage(info.substringAfter('='))
                            stringBuilder.append("充电电压 = ${voltage}\n")
                        }

                        info.startsWith("POWER_SUPPLY_CAPACITY=") -> {
                            val capacity = info.substringAfter('=')
                            stringBuilder.append("电池电量 = ${capacity}%\n")
                        }

                        info.startsWith("POWER_SUPPLY_MODEL_NAME=") -> {
                            val model = info.substringAfter('=')
                            stringBuilder.append("模块/型号 = ${model}\n")
                        }

                        info.startsWith("POWER_SUPPLY_CHARGE_TYPE=") -> {
                            val chargeType = info.substringAfter('=')
                            stringBuilder.append("充电类型 = ${chargeType}\n")
                        }

                        info.startsWith("POWER_SUPPLY_RESISTANCE_NOW=") -> {
                            val resistance = info.substringAfter('=')
                            stringBuilder.append("电阻/阻值 = ${resistance}\n")
                        }

                        info.startsWith("POWER_SUPPLY_CURRENT_NOW=") ||
                                info.startsWith("POWER_SUPPLY_CONSTANT_CHARGE_CURRENT=") -> {
                            io = info.substringAfter('=')
                        }

                        else -> { /* 忽略未处理的键 */
                        }
                    }
                } catch (e: NumberFormatException) {
                    // 记录异常日志（假设有日志系统）
                    e.printStackTrace()
                }
            }

            if (io.isNotEmpty() && mahLength != 0) {
                val limit = try {
                    val divisor =
                        if (mahLength < 5) 1 else 10.0.pow((mahLength - 4).toDouble()).toInt()
                    (io.toInt() / divisor)
                } catch (e: NumberFormatException) {
                    0
                }
                stringBuilder.insert(0, "放电速度 = ${limit}mA\n")
            }

            return stringBuilder.toString()
        }

    /**
     * 读取并解析 USB 信息
     */
    val usbInfo: String
        get() {
            val path = "/sys/class/power_supply/usb/uevent"
            if (runBlocking { !RootFile(path).exists() }) return ""

            val usbInfos = runBlocking { KernelProp.getProp(path) }
            val infos = usbInfos.lines()
            val stringBuilder = StringBuilder()
            var voltage = 0f
            var electricity = 0f
            var pdAuth = false

            infos.forEach { info ->
                try {
                    when {
                        info.startsWith("POWER_SUPPLY_VOLTAGE_NOW=") -> {
                            val voltageStr = strToVoltage(info.substringAfter('='))
                            voltage = voltageStr.removeSuffix("v").toFloatOrNull() ?: 0f
                            stringBuilder.append("当前电压 = ${voltageStr}\n")
                        }

                        info.startsWith("POWER_SUPPLY_CURRENT_MAX=") -> {
                            val current = (info.substringAfter('=').toIntOrNull() ?: 0) / 1_000_000f
                            if (current > 0) {
                                stringBuilder.append("最大电流 = ${current}A\n")
                            }
                        }

                        info.startsWith("POWER_SUPPLY_PD_VOLTAGE_MAX=") -> {
                            val voltage = strToVoltage(info.substringAfter('='))
                            stringBuilder.append("最大电压(PD) = ${voltage}\n")
                        }

                        info.startsWith("POWER_SUPPLY_CONNECTOR_TEMP=") -> {
                            val temp = (info.substringAfter('=').toIntOrNull() ?: 0) / 10.0f
                            stringBuilder.append("接口温度 = ${temp}°C\n")
                        }

                        info.startsWith("POWER_SUPPLY_PD_VOLTAGE_MIN=") -> {
                            val voltage = strToVoltage(info.substringAfter('='))
                            stringBuilder.append("最小电压(PD) = ${voltage}\n")
                        }

                        info.startsWith("POWER_SUPPLY_PD_CURRENT_MAX=") -> {
                            val current = (info.substringAfter('=').toIntOrNull() ?: 0) / 1_000_000f
                            if (current > 0) {
                                stringBuilder.append("最大电流(PD) = ${current}A\n")
                            }
                        }

                        info.startsWith("POWER_SUPPLY_INPUT_CURRENT_NOW=") ||
                                info.startsWith("POWER_SUPPLY_CONSTANT_CHARGE_CURRENT=") -> {
                            electricity = (info.substringAfter('=').toIntOrNull() ?: 0) / 1_000_000f
                        }

                        info.startsWith("POWER_SUPPLY_QUICK_CHARGE_TYPE=") -> {
                            val type = info.substringAfter('=')
                            val chargeType = when (type) {
                                "0" -> "慢速充电"
                                else -> "类型$type"
                            }
                            stringBuilder.append("快充类型 = ${chargeType}\n")
                        }

                        info.startsWith("POWER_SUPPLY_REAL_TYPE=") -> {
                            val realType = info.substringAfter('=')
                            stringBuilder.append("输电协议 = ${realType}\n")
                        }

                        info.startsWith("POWER_SUPPLY_HVDCP3_TYPE=") -> {
                            val type = info.substringAfter('=')
                            val hvdcp3Type = if (type == "0") "否" else "类型$type"
                            stringBuilder.append("高压快充 = ${hvdcp3Type}\n")
                        }

                        info.startsWith("POWER_SUPPLY_PD_AUTHENTICATION=") -> {
                            pdAuth = info.substringAfter('=') == "1"
                            stringBuilder.append("PD认证 = ${if (pdAuth) "已认证" else "未认证"}\n")
                        }

                        else -> { /* 忽略未处理的键 */
                        }
                    }
                } catch (e: NumberFormatException) {
                    // 记录异常日志
                    e.printStackTrace()
                }
            }

            if (!pdAuth && voltage > 0 && electricity > 0) {
                val power = (voltage * electricity).let { String.format("%.2f", it) }
                stringBuilder.append("当前电流 = ${electricity}A\n")
                stringBuilder.append("参考功率 = ${power}W\n")
            }

            return stringBuilder.toString()
        }

    /**
     * 快充是否支持修改充电速度设置
     */
    suspend fun qcSettingSupport(): Boolean {
        return RootFile("/sys/class/power_supply/battery/constant_charge_current_max").exists()
    }

    /**
     * 步进充电是否支持
     */
    suspend fun stepChargeSupport(): Boolean {
        return RootFile("/sys/class/power_supply/battery/step_charging_enabled").exists()
    }

    /**
     * 获取步进充电状态
     */
    suspend fun getStepCharge(): Boolean {
        return KernelProp.getProp("/sys/class/power_supply/battery/step_charging_enabled") == "1"
    }

    /**
     * 设置步进充电状态
     */
    suspend fun setStepCharge(stepCharge: Boolean) {
        KernelProp.setProp(
            "/sys/class/power_supply/battery/step_charging_enabled",
            if (stepCharge) "1" else "0"
        )
    }

    /**
     * 获取快充限制
     */
    suspend fun getQcLimit(): String {
        if (useMainConstant == null) {
            useMainConstant =
                RootFile("/sys/class/power_supply/main/constant_charge_current_max").exists()
        }

        val path = if (useMainConstant == true) {
            "/sys/class/power_supply/main/constant_charge_current_max"
        } else {
            "/sys/class/power_supply/battery/constant_charge_current_max"
        }

        var limit = KernelProp.getProp(path) ?: ""

        limit = when {
            limit.length > 3 -> "${limit.dropLast(3)}mA"
            limit.isNotEmpty() -> {
                try {
                    if (limit.toInt() == 0) "0"
                    else limit
                } catch (e: NumberFormatException) {
                    limit
                }
            }

            else -> "?mA"
        }
        return limit
    }

    /**
     * 快充是否支持电池保护
     */
    suspend fun bpSettingSupport(): Boolean {
        return RootFile("/sys/class/power_supply/battery/battery_charging_enabled").exists() ||
                RootFile("/sys/class/power_supply/battery/input_suspend").exists()
    }

    /**
     * 设置充电速度限制
     */
    suspend fun setChargeInputLimit(limit: Int, context: Context, force: Boolean = false): Boolean {
        if (changeLimitRunning && !force) {
            return false
        }

        synchronized(this) {
            if (changeLimitRunning && !force) {
                return false
            }
            changeLimitRunning = true
        }

        return try {
            if (fastChargeScript.isEmpty()) {
                val scriptPath =
                    listOf("script/fast_charge.sh", "script/fast_charge_run_once.sh").map {
                        context.assets.open(it).use { `is` ->
                            `is`.buffered().use { bis ->
                                val file =
                                    File(Config.Path.ScriptDir, it.substring(it.indexOf("/") + 1))
                                file.outputStream().use { os ->
                                    os.buffered().use { bos ->
                                        bis.copyTo(bos)
                                    }
                                }
                                file.absolutePath
                            }
                        }
                    }

                if (scriptPath.all { it != null }) {
                    if (isFirstRun) {
                        ReusableShells.getInstance(
                            "setChargeInputLimit",
                            status = TweakApplication.runtimeStatus
                        ).commitCmdSync("sh ${scriptPath[1]}")
                        isFirstRun = false
                    }
                    fastChargeScript = "sh ${scriptPath[0]} "
                }
            }

            if (fastChargeScript.isNotEmpty()) {
                if (limit > 3000) {
                    var current = 3000
                    while (current < (limit - 300) && current < 5000) {
                        val result = ReusableShells.getInstance(
                            "setChargeInputLimit",
                            status = TweakApplication.runtimeStatus
                        ).commitCmdSync("$fastChargeScript$current 1")
                        if (result == "error") break
                        current += 300
                    }
                }
                ReusableShells.getInstance(
                    "setChargeInputLimit",
                    status = TweakApplication.runtimeStatus
                ).commitCmdSync("$fastChargeScript$limit 0")
                true
            } else {
                false
            }
        } finally {
            changeLimitRunning = false
        }
    }

    /**
     * 判断是否支持 PD
     */
    suspend fun pdSupported(): Boolean {
        return RootFile("/sys/class/power_supply/usb/pd_allowed").exists() ||
                RootFile("/sys/class/power_supply/usb/pd_active").exists()
    }

    /**
     * 判断 PD 是否被允许
     */
    suspend fun pdAllowed(): Boolean {
        return KernelProp.getProp("/sys/class/power_supply/usb/pd_allowed") == "1"
    }

    /**
     * 设置 PD 允许状态
     */
    suspend fun setAllowed(isAllowed: Boolean): Boolean {
        val commands = listOf(
            "chmod 777 /sys/class/power_supply/usb/pd_allowed",
            "echo ${if (isAllowed) "1" else "0"} > /sys/class/power_supply/usb/pd_allowed",
            "chmod 777 /sys/class/power_supply/usb/pd_active",
            "echo 1 > /sys/class/power_supply/usb/pd_active"
        )
        val script = commands.joinToString("\n")
        return ReusableShells.execSync(script) != "error"
    }

    /**
     * 判断 PD 是否激活
     */
    suspend fun pdActive(): Boolean {
        return KernelProp.getProp("/sys/class/power_supply/usb/pd_active") == "1"
    }

    /**
     * 获取充满电容量
     */
    suspend fun getChargeFull(): Int {
        val value = KernelProp.getProp("/sys/class/power_supply/bms/charge_full") ?: return 0
        return value.toIntOrNull()?.div(1000) ?: 0
    }

    /**
     * 设置充满电容量
     */
    suspend fun setChargeFull(mAh: Int) {
        KernelProp.setProp("/sys/class/power_supply/bms/charge_full", (mAh * 1000).toString())
    }

    /**
     * 获取电池容量
     */
    suspend fun getCapacity(): Int {
        val value = KernelProp.getProp("/sys/class/power_supply/battery/capacity") ?: return 0
        return value.toIntOrNull() ?: 0
    }

    /**
     * 设置电池容量
     */
    suspend fun setCapacity(capacity: Int) {
        KernelProp.setProp("/sys/class/power_supply/battery/capacity", capacity.toString())
    }

    /**
     * 从内核读取精确电量
     */
    suspend fun getKernelCapacity(approximate: Int): Float {
        if (kernelCapacitySupported == null) {
            kernelCapacitySupported = RootFile("/sys/class/power_supply/bms/capacity_raw").exists()
        }
        if (kernelCapacitySupported == true) {
            try {
                val raw =
                    KernelProp.getProp("/sys/class/power_supply/bms/capacity_raw") ?: return -1f
                val capacityValue = raw.toIntOrNull() ?: return -1f

                val valueMA = if (kotlin.math.abs(capacityValue - approximate) >
                    kotlin.math.abs((capacityValue / 100f) - approximate)
                ) {
                    capacityValue / 100f
                } else {
                    raw.toFloat()
                }

                // 如果和系统反馈的电量差距超过5%，则认为数值无效，不再读取
                if (kotlin.math.abs(valueMA - approximate) > 5) {
                    kernelCapacitySupported = false
                    return -1f
                }
                return valueMA
            } catch (e: NumberFormatException) {
                kernelCapacitySupported = false
            }
        }
        return -1f
    }
}
