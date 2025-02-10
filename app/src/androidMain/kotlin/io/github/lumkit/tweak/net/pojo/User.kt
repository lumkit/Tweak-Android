package io.github.lumkit.tweak.net.pojo

import io.github.lumkit.tweak.data.UserStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long? = null,
    val username: String? = null,
    val password: String? = null,
    val nickname: String? = null,
    val email: String? = null,
    val integration: Long? = null,
    val latestLogin: LocalDateTime? = null,
    val vipDate: LocalDateTime? = null,
    val signature: String? = null,
    val status: UserStatus? = null,
    val version: Int? = null,
    val delete: Int? = null,
    val banDate: LocalDateTime? = null,
    val createTime: LocalDateTime? = null,
    val updateTime: LocalDateTime? = null,
    val key: Long = Clock.System.now().toEpochMilliseconds(),
)

fun User.isVip(): Boolean {
    val now = Clock.System.now().toEpochMilliseconds()
    if (vipDate == null) return false
    return vipDate.toInstant(TimeZone.of("Asia/Shanghai")).toEpochMilliseconds() >= now
}