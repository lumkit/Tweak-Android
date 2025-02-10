package io.github.lumkit.tweak.net.pojo.resp

import io.github.lumkit.tweak.net.pojo.RequestParams
import kotlinx.serialization.Serializable

@Serializable
data class UpdatePassword(
    val newPassword: String,
    val oldPassword: String,
    val captcha: String,
): RequestParams()
