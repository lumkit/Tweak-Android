package io.github.lumkit.tweak.data

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class SmartNoticeProperties @OptIn(ExperimentalUuidApi::class) constructor(
    val uuid: String = Uuid.random().toString(),
    val label: String,
    val gravity: Int,
    val y: Float,
    val x: Float,
    val width: Float,
    val height: Float,
    val radius: Float,
    val duration: Long,
    val delay: Long,
)
