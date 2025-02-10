package io.github.lumkit.tweak.data

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/**
 * 用户身份
 */
@Serializable
enum class UserStatus {
    Normal, Assessor, Manager, Developer
}

fun UserStatus.asString(): String = when (this) {
    io.github.lumkit.tweak.data.UserStatus.Assessor -> "审核员"
    io.github.lumkit.tweak.data.UserStatus.Manager -> "管理员"
    io.github.lumkit.tweak.data.UserStatus.Developer -> "开发者"
    else -> "用户"
}

fun UserStatus.color(): Color = when (this) {
    io.github.lumkit.tweak.data.UserStatus.Normal -> Color(0xFF757575)
    io.github.lumkit.tweak.data.UserStatus.Assessor -> Color(0xFFF44336)
    io.github.lumkit.tweak.data.UserStatus.Manager -> Color(0xFFFB8C00)
    io.github.lumkit.tweak.data.UserStatus.Developer -> Color(0xFF2196F3)
}