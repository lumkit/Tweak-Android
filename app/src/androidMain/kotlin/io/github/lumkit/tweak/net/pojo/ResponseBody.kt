package io.github.lumkit.tweak.net.pojo

import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.model.Config
import io.github.lumkit.tweak.util.Aes
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ResponseBody<T>(
    val data: T? = null,
    val message: String? = null,
    val responseTime: String? = null,
    val status: Int? = null,
)

suspend inline fun <reified T> HttpResponse.jsonToBean(): ResponseBody<T> = body()
suspend inline fun <reified T> HttpResponse.decryptBean(): ResponseBody<T> = if (!Config.DEBUG) {
    val text = bodyAsText()
    val decrypt = Aes.decrypt(TweakApplication.application.applicationContext, text).toString()
    Json.decodeFromString(decrypt)
} else {
    body()
}