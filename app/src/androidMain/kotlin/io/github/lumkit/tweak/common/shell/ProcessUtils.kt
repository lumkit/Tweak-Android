package io.github.lumkit.tweak.common.shell

import android.content.Context
import android.util.Log
import io.github.lumkit.tweak.common.shell.provide.ReusableShells
import io.github.lumkit.tweak.common.shell.util.ToyboxInstaller
import io.github.lumkit.tweak.model.ProcessInfo

/*
 * 进程管理相关
 */
class ProcessUtils {

    /*
    VSS- Virtual Set Size 虚拟耗用内存（包含共享库占用的内存）
    RSS- Resident Set Size 实际使用物理内存（包含共享库占用的内存）
    PSS- Proportional Set Size 实际使用的物理内存（比例分配共享库占用的内存）
    USS- Unique Set Size 进程独自占用的物理内存（不包含共享库占用的内存）
    一般来说内存占用大小有如下规律：VSS >= RSS >= PSS >= USS
    ————————————————
    版权声明：本文为CSDN博主「火山石」的原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接及本声明。
    原文链接：https://blog.csdn.net/zhangcanyan/java/article/details/84556808
    */

    // pageSize 获取 : getconf PAGESIZE
    companion object {
        private var LIST_COMMAND: String? = null
        private var DETAIL_COMMAND: String? = null
    }

    /**
     * 兼容性检查，初始化 LIST_COMMAND 和 DETAIL_COMMAND
     *
     * @param context 上下文
     * @return 是否支持
     */
    suspend fun supported(context: Context): Boolean {
        if (LIST_COMMAND == null || DETAIL_COMMAND == null) {
            LIST_COMMAND = ""
            DETAIL_COMMAND = ""

            val outsideToybox = ToyboxInstaller(context).install()

            val perfectCmd = "top -o %CPU,RES,SWAP,NAME,PID,USER,COMMAND,CMDLINE -q -b -n 1 -m 65535"
            val outsidePerfectCmd = "$outsideToybox $perfectCmd"
            // val insideCmd1 = "ps -e -o %CPU,RSS,SHR,NAME,PID,USER,COMMAND,CMDLINE"
            // val insideCmd2 = "ps -e -o %CPU,RES,SHR,RSS,NAME,PID,S,USER,COMMAND,CMDLINE"
            val insideCmd = "ps -e -o %CPU,RES,SWAP,NAME,PID,USER,COMMAND,CMDLINE"
            val outsideCmd = "$outsideToybox $insideCmd"

            val commandsToCheckFirst = listOf(outsidePerfectCmd, perfectCmd, outsideCmd, insideCmd)
            for (cmd in commandsToCheckFirst) {
                val rows = ReusableShells.execSync("$cmd 2>&1").split("\n")
                val result = rows.firstOrNull() ?: ""
                if (rows.size > 10 && !(result.contains("bad -o") || result.contains("Unknown option") || result.contains("bad"))) {
                    LIST_COMMAND = cmd
                    break
                }
            }

            val commandsToCheckSecond = listOf(outsideCmd, insideCmd)
            for (cmd in commandsToCheckSecond) {
                val rows = ReusableShells.execSync("$cmd 2>&1").split("\n")
                val result = rows.firstOrNull() ?: ""
                if (rows.size > 10 && !(result.contains("bad -o") || result.contains("Unknown option") || result.contains("bad"))) {
                    DETAIL_COMMAND = "$cmd --pid "
                    break
                }
            }
        }

        return !(LIST_COMMAND.isNullOrEmpty() || DETAIL_COMMAND.isNullOrEmpty())
    }

    /**
     * 将字符串转换为 Long 类型，处理带单位的内存表示
     *
     * @param str 内存字符串（如 "10K", "5M", "2G"）
     * @return 转换后的 Long 值
     */
    private fun str2Long(str: String): Long {
        return when {
            str.contains("K", ignoreCase = true) -> (str.substringBefore("K").toDouble()).toLong()
            str.contains("M", ignoreCase = true) -> (str.substringBefore("M").toDouble() * 1024).toLong()
            str.contains("G", ignoreCase = true) -> (str.substringBefore("G").toDouble() * 1048576).toLong()
            else -> (str.toLongOrNull() ?: 0L) / 1024
        }
    }

    // 从进程列表排除的应用
    private val excludeProcess = listOf(
        "toybox-outside",
        "toybox-outside64",
        "ps",
        "top",
        "io.github.lumkit.tweak"
    )

    /**
     * 解析单行数据，转换为 ProcessInfo 对象
     *
     * @param row 单行进程信息
     * @return ProcessInfo 对象或 null
     */
    private fun readRow(row: String): ProcessInfo? {
        val columns = row.split(Regex("\\s+"))
        if (columns.size >= 7) { // 确保至少有7列
            return try {
                val processInfo = ProcessInfo().apply {
                    cpu = columns[0].toFloatOrNull() ?: 0f
                    res = str2Long(columns[1])
                    swap = str2Long(columns[2])
                    name = columns[3]

                    if (excludeProcess.contains(name)) {
                        return null
                    }

                    pid = columns[4].toIntOrNull() ?: 0
                    user = columns[5]
                    command = columns.getOrNull(6) ?: ""
                    cmdline = if (command.isNotEmpty()) {
                        row.substringAfter(command).trim()
                    } else {
                        ""
                    }
                }
                processInfo
            } catch (ex: Exception) {
                Log.e("ProcessUtils", "Error parsing row: $row", ex)
                null
            }
        } else {
            Log.e("ProcessUtils", "Invalid row format: $row")
            return null
        }
    }

    /**
     * 获取所有进程信息
     *
     * @return 进程信息列表
     */
    suspend fun getAllProcess(): List<ProcessInfo> {
        val processInfoList = mutableListOf<ProcessInfo>()
        var isFirstRow = true
        LIST_COMMAND?.let { cmd ->
            val rows = ReusableShells.execSync(cmd).split("\n")
            for (row in rows) {
                if (isFirstRow) {
                    isFirstRow = false // 跳过标题行
                    continue
                }

                val trimmedRow = row.trim()
                val processInfo = readRow(trimmedRow)
                if (processInfo != null) {
                    processInfoList.add(processInfo)
                }
            }
        }
        return processInfoList
    }

    /**
     * 获取指定 PID 的进程详情
     *
     * @param pid 进程 ID
     * @return ProcessInfo 对象或 null
     */
    suspend fun getProcessDetail(pid: Int): ProcessInfo? {
        DETAIL_COMMAND?.let { cmd ->
            val fullCmd = "$cmd$pid"
            val response = ReusableShells.execSync(fullCmd)
            Log.d("ProcessUtils", "Executing command: $fullCmd")
            Log.d("ProcessUtils", "Response: $response")
            val rows = response.split("\n")
            if (rows.size > 1) {
                val row = readRow(rows[1].trim())
                row?.apply {
                    cpuSet = KernelProp.getProp("/proc/$pid/cpuset")
                    cGroup = KernelProp.getProp("/proc/$pid/cgroup")
                    oomAdj = KernelProp.getProp("/proc/$pid/oom_adj")
                    oomScore = KernelProp.getProp("/proc/$pid/oom_score")
                    oomScoreAdj = KernelProp.getProp("/proc/$pid/oom_score_adj")
                }
                return row
            }
        }
        return null
    }

    /**
     * 强制结束指定 PID 的进程
     *
     * @param pid 进程 ID
     */
    suspend fun killProcess(pid: Int) {
        ReusableShells.execSync("kill -9 $pid")
    }

    /**
     * 检查进程是否为 Android 应用进程
     *
     * @param processInfo 进程信息
     * @return 是否为 Android 进程
     */
    private fun isAndroidProcess(processInfo: ProcessInfo): Boolean {
        return processInfo.command.contains("app_process") && processInfo.name.contains(".")
    }

    /**
     * 强制结束指定的进程
     *
     * @param processInfo 进程信息
     */
    suspend fun killProcess(processInfo: ProcessInfo) {
        if (isAndroidProcess(processInfo)) {
            val packageName = if (processInfo.name.contains(":")) {
                processInfo.name.substringBefore(":")
            } else {
                processInfo.name
            }
            ReusableShells.execSync("killall -9 $packageName; am force-stop $packageName; am kill $packageName")
        } else {
            killProcess(processInfo.pid)
        }
    }
}
