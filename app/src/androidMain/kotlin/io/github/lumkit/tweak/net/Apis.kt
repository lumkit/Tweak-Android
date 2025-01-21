package io.github.lumkit.tweak.net

import io.github.lumkit.tweak.BuildConfig
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.status.TweakException
import io.github.lumkit.tweak.model.Config
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.net.pojo.ResponseBody
import io.github.lumkit.tweak.net.pojo.jsonToBean
import io.github.lumkit.tweak.net.pojo.resp.ClientVersionBean
import io.github.lumkit.tweak.util.Aes
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.plugin
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

object Apis {

    private val baseUrl: String
        get() {
            return if (Config.DEBUG) {
                "http://localhost:9888/"
            } else {
                "https://api.tweak.lumtoolkit.com/"
            }
        }

    private val appKtor by lazy {
        ktorClient.config {
            defaultRequest {
                url(baseUrl)
                headers {
                    append("Authorization", "Bearer ${TweakApplication.shared.getString(Const.APP_AUTHORIZATION, "")}")
                }
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = Config.DEBUG
                        encodeDefaults = false
                    }
                )
            }
        }.apply {
            plugin(HttpSend).intercept {
                val originalCall = execute(it)
                val response = originalCall.response
                val status = response.status
                if (status == HttpStatusCode.OK) {
                    val channel = response.bodyAsChannel()
                    val cipherText = channel.readRemaining().readText()
                    if (!cipherText.startsWith("{")) {
                        val responseJson = if (!Config.DEBUG) {
                            Aes.decrypt(cipherText).toString()
                        } else {
                            cipherText
                        }
                        val responseBody = Json.decodeFromString<ResponseBody<JsonElement>>(responseJson)
                        if (responseBody.status != 200) {
                            // TODO 清除登录信息
                            if (responseBody.status == 401) {

                            }
                            throw TweakException(message = responseBody.message ?: "")
                        }
                    }
                    execute(it)
                } else {
                    val msg = when (status) {
                        HttpStatusCode.Unauthorized -> {
                            // TODO 清除登录信息

                            "登录信息已过期"
                        }
                        else -> "网络请求出错：${status.value}"
                    }
                    throw TweakException(message = msg)
                }
            }
        }
    }

    /**
     * 版本接口
     */
    object Version {
        suspend fun latest(): ResponseBody<ClientVersionBean?> = appKtor.get("/client/version/latest?clientType=1")
            .jsonToBean()

        suspend fun all(): ResponseBody<List<ClientVersionBean>> = appKtor.get("/client/version/all?clientType=1")
            .jsonToBean()

        suspend fun latestMustBe(): ResponseBody<ClientVersionBean?> = appKtor.get("/client/version/latestMustBe?clientType=1")
            .jsonToBean()
    }

}