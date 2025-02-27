package io.github.lumkit.tweak.data

import kotlinx.serialization.Serializable

@Serializable
data class CutoutRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)
