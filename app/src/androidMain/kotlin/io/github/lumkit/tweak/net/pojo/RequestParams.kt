package io.github.lumkit.tweak.net.pojo

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * 请求验证
 */
@Serializable
open class RequestParams {
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault val milliseconds: Long = System.currentTimeMillis()
}