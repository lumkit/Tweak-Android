package io.github.lumkit.tweak.common.shell

import kotlinx.coroutines.runBlocking

object CpuLoad {

    private var lastCpuState: String = ""
    private var lastCpuStateMap: Map<Int, Float>? = null
    private var lastCpuStateTime: Long = 0

    init {
        lastCpuState = readCpuStat()
    }

    /**
     * 读取 /proc/stat 中以 "cpu" 开头的行
     */
    private fun readCpuStat(): String {
        return runBlocking {
            KernelProp.getProp("/proc/stat", "^cpu")
        }
    }

    /**
     * 根据 CPU 行的第一列返回对应的索引
     * "cpu" 返回 -1，"cpu0" 返回 0，"cpu1" 返回 1，依此类推
     */
    private fun getCpuIndex(cols: List<String>): Int {
        return when (cols[0]) {
            "cpu" -> -1
            else -> cols[0].substring(3).toIntOrNull() ?: -1
        }
    }

    /**
     * 计算 CPU 总时间
     */
    private fun cpuTotalTime(cols: List<String>): Long {
        return cols.drop(1).mapNotNull { it.toLongOrNull() }.sum()
    }

    /**
     * 获取 CPU 空闲时间
     */
    private fun cpuIdleTime(cols: List<String>): Long {
        return cols.getOrNull(4)?.toLongOrNull() ?: 0
    }

    /**
     * 获取每个 CPU 的负载
     */
    @Synchronized
    fun getCpuLoad(): Map<Int, Float> {
        val currentTime = System.currentTimeMillis()
        // 使用缓存，如果上次读取时间在 500ms 内，则返回缓存结果
        if (lastCpuStateMap != null && (currentTime - lastCpuStateTime) < 500) {
            return lastCpuStateMap!!
        }

        val loads = mutableMapOf<Int, Float>()
        val currentStat = readCpuStat()

        if (currentStat.isNotEmpty() && currentStat.startsWith("cpu")) {
            try {
                if (lastCpuState.isEmpty()) {
                    lastCpuState = currentStat
                    Thread.sleep(100) // 保持与原始逻辑一致，建议优化
                    return getCpuLoad()
                } else {
                    val currentLines = currentStat.lineSequence().filter { it.startsWith("cpu") }.toList()
                    val previousLines = lastCpuState.lineSequence().filter { it.startsWith("cpu") }.toList()

                    for (currentLine in currentLines) {
                        val cols1 = currentLine.replace(Regex(" +"), " ").split(" ")
                        val previousLine = previousLines.find { it.startsWith("${cols1[0]} ") }
                        val cols0 = previousLine?.replace(Regex(" +"), " ")?.split(" ")

                        if (cols0 != null && cols0.size > 4) {
                            val total1 = cpuTotalTime(cols1)
                            val idle1 = cpuIdleTime(cols1)
                            val total0 = cpuTotalTime(cols0)
                            val idle0 = cpuIdleTime(cols0)
                            val timeDiff = total1 - total0

                            if (timeDiff == 0L) {
                                loads[getCpuIndex(cols1)] = 0.0f
                            } else {
                                val idleDiff = idle1 - idle0
                                val load = if (idleDiff < 1L) {
                                    100.0f
                                } else {
                                    100.0f - (idleDiff * 100.0f / timeDiff)
                                }
                                loads[getCpuIndex(cols1)] = load
                            }
                        } else {
                            loads[getCpuIndex(cols1)] = 0.0f
                        }
                    }

                    lastCpuState = currentStat
                    lastCpuStateTime = currentTime
                    lastCpuStateMap = loads
                    return loads
                }
            } catch (ex: Exception) {
                // 这里可以添加日志记录 ex 信息
                return loads
            }
        }

        return loads
    }

    /**
     * 获取 CPU 总负载
     */
    @Synchronized
    fun getCpuLoadSum(): Float {
        val cpuLoadMap = getCpuLoad()
        return cpuLoadMap[-1] ?: -1.0f
    }
}
