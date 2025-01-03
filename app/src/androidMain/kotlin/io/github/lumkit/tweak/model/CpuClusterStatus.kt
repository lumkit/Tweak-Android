package io.github.lumkit.tweak.model

import java.io.Serializable

data class CpuClusterStatus(
    var minFreq: String = "",
    var maxFreq: String = "",
    var governor: String = "",
    var governorParams: MutableMap<String, String> = mutableMapOf()
) : Serializable
