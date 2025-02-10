package io.github.lumkit.tweak.net.pojo.resp

import io.github.lumkit.tweak.net.pojo.CaptchaType
import io.github.lumkit.tweak.net.pojo.RequestParams
import kotlinx.serialization.Serializable

@Serializable
data class GetCaptchaParam(
    val email: String,
    val type: CaptchaType
): RequestParams()