package io.github.lumkit.tweak.data

import kotlinx.serialization.Serializable

@Serializable
data class SmartNoticeData(
    val label: String? = null,
    val width: Int,
    val height: Int,
    val gravity: SmartNoticeGravity,
    val x: Int,
    val y: Int,
    val radius: Float,
    val duration: Long,
    val delay: Long,
    val interpolator: SmartNoticeInterpolator,
)