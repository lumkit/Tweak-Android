package io.github.lumkit.tweak.data

import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.ui.local.json
import kotlinx.serialization.Serializable

@Serializable
data class AndroidSoc(
    val VENDOR: String? = null,
    val NAME: String? = null,
    val FAB: String? = null,
    val CPU: String? = null,
    val MEMORY: String? = null,
    val BANDWIDTH: String? = null,
    val CHANNELS: String? = null,
) {
    override fun toString(): String = buildString {
        append("处理器厂商: ")
        append(VENDOR)
        append("\n")
        append("制程工艺: ")
        append(FAB)
        if (!CPU.isNullOrEmpty()) {
            append("\n")
            append("CPU: ")
            append(CPU)
        }
        if (!MEMORY.isNullOrEmpty()) {
            append("\n")
            append("内存: ")
            append(MEMORY)
        }
        if (!CHANNELS.isNullOrEmpty()) {
            append("\n")
            append("通道: ")
            append(CHANNELS)
        }
        if (!BANDWIDTH.isNullOrEmpty()) {
            append("\n")
            append("Bandwidth: ")
            append(BANDWIDTH)
        }
    }

    companion object {
        private var map: Map<String, AndroidSoc> = emptyMap()

        fun getSocByCpuMode(name: String): AndroidSoc? {
            if (map.isEmpty()) {
                val content = TweakApplication.application.assets.open("socs.json").use {
                    String(it.readBytes())
                }
                map = json.decodeFromString(content)
            }
            return map[name]
        }
    }
}