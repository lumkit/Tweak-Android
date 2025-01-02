package io.github.lumkit.tweak.common.util

import io.github.lumkit.tweak.common.status.BackendException
import io.github.lumkit.tweak.common.status.TweakException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException

/**
 * 将请求异常转为人类可读文本
 */
fun Throwable.makeText(): String = when (this) {
    is BackendException -> {
        "$message   code: $status"
    }
    is TweakException -> {
        message
    }
    is HttpRequestTimeoutException -> {
        "请求超时"
    }
    is ConnectTimeoutException -> {
        "连接服务器超时"
    }
    is ClientRequestException -> {
        "客户端请求错误：${this.message}"
    }
    is ServerResponseException -> {
        "服务器错误：${this.message}"
    }
    is RedirectResponseException -> {
        "重定向错误：${this.message}"
    }
    is ResponseException -> {
        "HTTP 错误：${this.response.status.value}"
    }
    else -> {
        "未知错误：${this.message}"
    }
}