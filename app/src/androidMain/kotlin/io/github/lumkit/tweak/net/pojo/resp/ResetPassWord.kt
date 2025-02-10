package io.github.lumkit.tweak.net.pojo.resp

import io.github.lumkit.tweak.net.pojo.RequestParams
import kotlinx.serialization.Serializable

@Serializable
data class ResetPassWord(
    val username: String,
    val email: String,
    val password: String,
    val captcha: String,
): RequestParams()