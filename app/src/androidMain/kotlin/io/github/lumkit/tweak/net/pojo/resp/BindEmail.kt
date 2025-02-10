package io.github.lumkit.tweak.net.pojo.resp

import io.github.lumkit.tweak.net.pojo.RequestParams
import kotlinx.serialization.Serializable

@Serializable
data class BindEmail(
    val newEmail: String,
    val oldCaptcha: String,
    val newCaptcha: String
): RequestParams()