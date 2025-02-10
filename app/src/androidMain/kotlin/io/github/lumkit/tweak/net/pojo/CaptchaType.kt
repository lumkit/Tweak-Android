package io.github.lumkit.tweak.net.pojo

import kotlinx.serialization.Serializable

@Serializable
enum class CaptchaType {
    Register, Login, Password, BindEmail
}