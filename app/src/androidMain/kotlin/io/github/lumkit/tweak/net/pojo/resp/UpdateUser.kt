package io.github.lumkit.tweak.net.pojo.resp

import io.github.lumkit.tweak.net.pojo.RequestParams
import kotlinx.serialization.Serializable

@Serializable
data class UpdateUser(
    val nickname: String? = null,
    val signature: String? = null,
): RequestParams()
