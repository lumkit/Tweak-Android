package io.github.lumkit.tweak.net

import android.util.Log
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.status.TweakException
import io.github.lumkit.tweak.common.util.AuthorizationUtils
import io.github.lumkit.tweak.model.Config
import io.github.lumkit.tweak.net.pojo.ResponseBody
import io.github.lumkit.tweak.net.pojo.TokenBean
import io.github.lumkit.tweak.net.pojo.decryptBean
import io.github.lumkit.tweak.net.pojo.jsonToBean
import io.github.lumkit.tweak.net.pojo.resp.BindEmail
import io.github.lumkit.tweak.net.pojo.resp.ClientVersionBean
import io.github.lumkit.tweak.net.pojo.resp.GetCaptchaParam
import io.github.lumkit.tweak.net.pojo.resp.GetUserParam
import io.github.lumkit.tweak.net.pojo.resp.RegisterParam
import io.github.lumkit.tweak.net.pojo.resp.ResetPassWord
import io.github.lumkit.tweak.net.pojo.resp.UpdatePassword
import io.github.lumkit.tweak.net.pojo.resp.UpdateUser
import io.github.lumkit.tweak.ui.screen.main.page.LoginViewModel
import io.github.lumkit.tweak.util.Aes
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.plugin
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.cio.readChannel
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File

object Apis {

    private val baseUrl: String
        get() {
            return if (Config.DEBUG) {
                "http://localhost:9888/"
            } else {
                "https://api.tweak.lumtoolkit.com/"
            }
        }

    fun clearToken() {
        LoginViewModel.loginState = false
        AuthorizationUtils.save(null)
    }

    private val appKtor by lazy {
        ktorClient.config {
            defaultRequest {
                url(baseUrl)
                headers {
                    AuthorizationUtils.load()?.let {
                        append("Authorization", "Bearer $it")
                    }
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

                Log.d("Ktor-Test", response.bodyAsText())

                if (status == HttpStatusCode.OK) {
                    val channel = response.bodyAsChannel()
                    val cipherText = channel.readRemaining().readText()
                    if (!cipherText.startsWith("{")) {
                        val responseJson = if (!Config.DEBUG) {
                            Aes.decrypt(TweakApplication.application.applicationContext, cipherText).toString()
                        } else {
                            cipherText
                        }
                        val responseBody = Json.decodeFromString<ResponseBody<JsonElement>>(responseJson)
                        if (responseBody.status != 200) {
                            if (responseBody.status == 401) {
                                clearToken()
                            }
                            throw TweakException(message = responseBody.message ?: "")
                        }
                    }
                    originalCall
                } else {
                    val msg = when (status) {
                        HttpStatusCode.Unauthorized -> {
                            clearToken()
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

    object User {
        suspend fun getCaptcha(getCaptchaParam: GetCaptchaParam): ResponseBody<String> =
            appKtor.postEncrypt("/user/getCaptcha", getCaptchaParam)
                .decryptBean()

        suspend fun register(registerParam: RegisterParam): ResponseBody<String> =
            appKtor.postEncrypt("/user/register", registerParam)
                .decryptBean()

        suspend fun self(): ResponseBody<io.github.lumkit.tweak.net.pojo.User> =
            appKtor.get("/user/self").decryptBean()

        suspend fun login(getUserParam: GetUserParam): ResponseBody<TokenBean> =
            appKtor.postEncrypt("/user/login", getUserParam)
                .decryptBean()

        suspend fun resetPW(resetPassWord: ResetPassWord): ResponseBody<String> =
            appKtor.postEncrypt("/user/resetPW", resetPassWord)
                .decryptBean()

        fun avatar(username: String?): String = "${baseUrl}avatar/get/$username"

        suspend fun bindEmail(bindEmail: BindEmail): ResponseBody<String> =
            appKtor.putEncrypt("/user/bindEmail", bindEmail)
                .decryptBean()

        suspend fun uploadAvatar(file: File): ResponseBody<String> {
            return appKtor.post("/user/uploadAvatar") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "file",
                                ChannelProvider(file.length()) {
                                    file.readChannel()
                                },
                                Headers.build {
                                    append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"${file.name}\"")
                                    append(HttpHeaders.ContentType, ContentType.Application.OctetStream)
                                }
                            )
                        }
                    )
                )
            }.decryptBean()
        }

        suspend fun updateUser(updateUser: UpdateUser): ResponseBody<String> =
            appKtor.putEncrypt("/user/update", updateUser)
                .decryptBean()

        suspend fun updatePassword(updatePassword: UpdatePassword): ResponseBody<String> =
            appKtor.putEncrypt("/user/updatePW", updatePassword)
                .decryptBean()
    }
}