package io.github.lumkit.tweak.ui.screen.main.page

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.lumkit.tweak.common.BaseViewModel
import io.github.lumkit.tweak.common.util.AuthorizationUtils
import io.github.lumkit.tweak.data.LoadState
import io.github.lumkit.tweak.net.Apis
import io.github.lumkit.tweak.net.pojo.resp.GetCaptchaParam
import io.github.lumkit.tweak.net.pojo.resp.GetUserParam
import io.github.lumkit.tweak.net.pojo.resp.RegisterParam
import io.github.lumkit.tweak.net.pojo.resp.ResetPassWord
import io.github.lumkit.tweak.ui.local.StorageStore
import io.github.lumkit.tweak.util.Aes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel(
    context: Context,
    storageStore: StorageStore,
) : BaseViewModel() {

    companion object {
        var loginState by mutableStateOf(false)
    }

    @Immutable
    enum class LoginPageState {
        LOGIN, REGISTER
    }

    private val _pageState = MutableStateFlow(LoginPageState.LOGIN)
    val pageState = _pageState.asStateFlow()

    private val _usernameState = MutableStateFlow(storageStore.getString("username") ?: "")
    val usernameState = _usernameState.asStateFlow()

    private val _passwordState = MutableStateFlow(storageStore.getString("password")?.let {
        try {
            Aes.decrypt(context, it)
        } catch (e: Exception) {
            ""
        }
    } ?: "")
    val passwordState = _passwordState.asStateFlow()

    private val _confirmPasswordState = MutableStateFlow("")
    val confirmPasswordState = _confirmPasswordState.asStateFlow()

    private val _emailState = MutableStateFlow("")
    val emailState = _emailState.asStateFlow()

    private val _captchaState = MutableStateFlow("")
    val captchaState = _captchaState.asStateFlow()

    fun setPageState(state: LoginPageState) {
        _pageState.value = state
    }

    fun setUsername(username: String) {
        _usernameState.value = username
    }

    fun setPassword(password: String) {
        _passwordState.value = password
    }

    fun setConfirmPassword(password: String) {
        _confirmPasswordState.value = password
    }

    fun setEmail(email: String) {
        _emailState.value = email
    }

    fun setCaptcha(captcha: String) {
        _captchaState.value = captcha
    }

    fun login(
        getUserParam: GetUserParam,
    ) = suspendLaunch(
        "login"
    ) {
        loading()
        val login = Apis.User.login(getUserParam)
        AuthorizationUtils.save(login.data?.token)
        success(login.message.toString())
    }

    fun getCaptcha(
        getCaptchaParam: GetCaptchaParam,
        onComplete: () -> Unit = {},
        onSuccess: () -> Unit = {},
    ) = suspendLaunch(
        id = "getCaptcha",
        onComplete = {
            onComplete()
        }
    ) {
        loading()
        val result = Apis.User.getCaptcha(getCaptchaParam)
        onSuccess()
        success(result.message.toString())
    }

    fun register(
        registerParam: RegisterParam,
    ) = suspendLaunch(
        id = "register",
    ) {
        loading()
        val result = Apis.User.register(registerParam)
        success(result.message.toString())
    }

    fun resetPassword(
        resetPassWord: ResetPassWord,
    ) = suspendLaunch(
        id = "resetPassword"
    ) {
        loading()
        val result = Apis.User.resetPW(resetPassWord)
        success(result.message.toString())
    }
}