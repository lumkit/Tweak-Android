package io.github.lumkit.tweak.model

import android.content.pm.ActivityInfo

data class SceneConfigInfo(
    var packageName: String,

    // 使用独立亮度
    var aloneLight: Boolean = false,
    // 独立亮度值
    var aloneLightValue: Int = -1,
    // 屏蔽通知
    var disNotice: Boolean = false,
    // 拦截按键
    var disButton: Boolean = false,
    // 启动时开启GPS
    var gpsOn: Boolean = false,
    // 应用偏见（自动冻结）
    var freeze: Boolean = false,
    // 屏幕旋转方向
    var screenOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,

    // cgroup - memory
    var fgCGroupMem: String = "",
    var bgCGroupMem: String = "",
    var dynamicBoostMem: Boolean = false,

    // 显示性能监视器
    var showMonitor: Boolean = false
)
