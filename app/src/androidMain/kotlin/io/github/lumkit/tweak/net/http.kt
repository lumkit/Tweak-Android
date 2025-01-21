package io.github.lumkit.tweak.net

import io.github.lumkit.tweak.BuildConfig
import io.github.lumkit.tweak.model.Config
import io.github.lumkit.tweak.net.pojo.RequestParams
import io.github.lumkit.tweak.ui.local.json
import io.github.lumkit.tweak.util.Aes
import io.ktor.client.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val ktorClient by lazy {
    HttpClient(CIO) {
        engine {
            pipelining = true
        }
        val jsonConverter = json
        install(ContentNegotiation) {
            json(json = jsonConverter)
        }
        install(HttpCookies)
        install(HttpTimeout) {
            connectTimeoutMillis = 60000
            socketTimeoutMillis = 60000
        }
        Charsets {
            register(Charsets.UTF_8)
        }
    }
}

suspend inline fun <reified T: RequestParams> HttpClient.postEncrypt(
    url: String,
    requestParams: T,
): HttpResponse = post(url) {
    contentType(ContentType.Application.Json)
    if (!Config.DEBUG) {
        setBody(Aes.encrypt(Json.encodeToString(requestParams)))
    } else {
        setBody(requestParams)
    }
}

suspend inline fun <reified T: RequestParams> HttpClient.putEncrypt(
    url: String,
    requestParams: T,
): HttpResponse = put(url) {
    contentType(ContentType.Application.Json)
    if (!Config.DEBUG) {
        setBody(Aes.encrypt(Json.encodeToString(requestParams)))
    } else {
        setBody(requestParams)
    }
}