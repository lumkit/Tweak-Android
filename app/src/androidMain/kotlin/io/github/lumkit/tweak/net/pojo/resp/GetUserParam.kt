package io.github.lumkit.tweak.net.pojo.resp

import io.github.lumkit.tweak.net.pojo.RequestParams
import kotlinx.serialization.Serializable

@Serializable
data class GetUserParam(
    val username: String? = null,
    val email: String? = null,
    val password: String,
): RequestParams()