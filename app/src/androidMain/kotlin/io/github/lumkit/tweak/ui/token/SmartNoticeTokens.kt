package io.github.lumkit.tweak.ui.token

import androidx.compose.ui.unit.dp
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.util.getStatusBarHeight

object SmartNoticeCapsuleDefault {
    val CapsulePaddingTop = 6.dp
    val CapsulePadding = 4.dp
    val CapsuleWidth = 100.dp

    // 状态栏高度
    val CapsuleHeight = with(TweakApplication.density) {
        getStatusBarHeight().toDp() - CapsulePaddingTop * 2f
    }

    object Charge {
        val CapsuleWidth = 200.dp
        val CapsuleHeight = 150.dp
    }
}