package io.github.lumkit.tweak.model

import androidx.compose.runtime.Immutable

@Immutable
data class ProcessInfo(
    var pid: Int = 0,
    var name: String = "",
    var cpu: Float = 0f,
    var res: Long = 0L,
    var rss: Long = 0L,
    var mem: Long = 0L,
    var swap: Long = 0L,
    var state: String = "",
    var user: String = "",
    var command: String = "",
    var cmdline: String = "",
    var friendlyName: String = "",
    var cpuSet: String = "",
    var cGroup: String = "",
    var oomAdj: String = "",
    var oomScore: String = "",
    var oomScoreAdj: String = ""
) {
    /**
     * 进程的友好状态描述
     */
    val stateDescription: String
        get() = when (state) {
            "R" -> "R (running)"
            "S" -> "S (sleeping)"
            "D" -> "D (device I/O)"
            "T" -> "T (stopped)"
            "t" -> "t (trace stop)"
            "X" -> "X (dead)"
            "Z" -> "Z (zombie)"
            "P" -> "P (parked)"
            "I" -> "I (idle)"
            "x" -> "x (dead)"
            "K" -> "K (wakekill)"
            "W" -> "W (waking)"
            else -> "Unknown"
        }

    /**
     * 进程的CPU使用率
     */
    val cpuUsage: Float
        get() = when (state) {
            "T", "t", "X", "P", "I", "x" -> 0f
            else -> cpu
        }
}
