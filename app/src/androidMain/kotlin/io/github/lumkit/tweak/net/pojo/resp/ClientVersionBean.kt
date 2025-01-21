package io.github.lumkit.tweak.net.pojo.resp

import io.github.lumkit.tweak.data.ClientType
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ClientVersionBean(
    val id: Long? = null,
    val versionCode: Long,
    val versionName: String,
    val downloadUrl: String,
    val description: String?,
    val mustBe: Boolean,
    val clientType: ClientType,
    val createTime: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Shanghai")),
    val updateTime: LocalDateTime  = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Shanghai")),
)
