package io.github.lumkit.tweak.model

data class CpuClusterStatus(
    val minFreq: String = "",
    val maxFreq: String = "",
    val governor: String = "",
    val governorParams: MutableMap<String, String> = mutableMapOf()
)
