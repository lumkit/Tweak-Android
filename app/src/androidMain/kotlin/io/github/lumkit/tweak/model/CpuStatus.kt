package io.github.lumkit.tweak.model

data class CpuStatus(
    var cpuClusterStatuses: ArrayList<CpuClusterStatus> = ArrayList(),
    var coreControl: String = "",
    var vdd: String = "",
    var msmThermal: String = "",
    var coreOnline: ArrayList<Boolean>? = null,
    var exynosHmpUP: Int = 0,
    var exynosHmpDown: Int = 0,
    var exynosHmpBooster: Boolean = false,
    var exynosHotplug: Boolean = false,
    var adrenoMinFreq: String = "",
    var adrenoMaxFreq: String = "",
    var adrenoMinPL: String = "",
    var adrenoMaxPL: String = "",
    var adrenoDefaultPL: String = "",
    var adrenoGovernor: String = "",
    var cpusetBackground: String = "",
    var cpusetSysBackground: String = "",
    var cpusetForeground: String = "",
    var cpusetTopApp: String = "",
    var cpusetRestricted: String = ""
)