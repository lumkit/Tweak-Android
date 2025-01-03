package io.github.lumkit.tweak.model

data class CpuStatus(
    val cpuClusterStatuses: ArrayList<CpuClusterStatus> = ArrayList(),
    val coreControl: String = "",
    val vdd: String = "",
    val msmThermal: String = "",
    val coreOnline: ArrayList<Boolean>? = null,
    val exynosHmpUP: Int = 0,
    val exynosHmpDown: Int = 0,
    val exynosHmpBooster: Boolean = false,
    val exynosHotplug: Boolean = false,
    val adrenoMinFreq: String = "",
    val adrenoMaxFreq: String = "",
    val adrenoMinPL: String = "",
    val adrenoMaxPL: String = "",
    val adrenoDefaultPL: String = "",
    val adrenoGovernor: String = "",
    val cpusetBackground: String = "",
    val cpusetSysBackground: String = "",
    val cpusetForeground: String = "",
    val cpusetTopApp: String = "",
    val cpusetRestricted: String = ""
)