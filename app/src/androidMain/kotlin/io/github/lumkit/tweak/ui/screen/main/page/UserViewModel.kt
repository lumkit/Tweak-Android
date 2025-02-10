package io.github.lumkit.tweak.ui.screen.main.page

import io.github.lumkit.tweak.common.BaseViewModel
import io.github.lumkit.tweak.common.status.TweakException
import io.github.lumkit.tweak.data.LoadState
import io.github.lumkit.tweak.net.Apis
import io.github.lumkit.tweak.net.pojo.User
import io.github.lumkit.tweak.net.pojo.resp.BindEmail
import io.github.lumkit.tweak.net.pojo.resp.GetCaptchaParam
import io.github.lumkit.tweak.net.pojo.resp.UpdatePassword
import io.github.lumkit.tweak.net.pojo.resp.UpdateUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class UserViewModel: BaseViewModel() {

    private val _userState = MutableStateFlow<User?>(null)
    val userState = _userState.asStateFlow()

    fun user() = suspendLaunch(
        id = "user"
    ) {
        loading()
        val self = Apis.User.self()
        _userState.value = self.data ?: throw TweakException("用户信息不存在！")
        success(self.message.toString())
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

    fun bindEmail(
        bindEmail: BindEmail,
        onSuccess: () -> Unit = {},
    ) = suspendLaunch(
        id = "bindEmail",
    ) {
        loading()
        val result = Apis.User.bindEmail(bindEmail)
        onSuccess()
        success(result.message.toString())
    }

    fun updateUser(
        updateUser: UpdateUser,
    ) = suspendLaunch(
        id = "updateUser",
    ) {
        loading()
        val result = Apis.User.updateUser(updateUser)
        success(result.message.toString())
    }

    fun uploadAvatar(
        path: String,
    ) = suspendLaunch(
        id = "uploadAvatar"
    ) {
        loading()
        val result = Apis.User.uploadAvatar(File(path))
        success(result.message.toString())
    }

    fun updatePassword(
        updatePassword: UpdatePassword,
    ) = suspendLaunch(
        id = "updatePassword"
    ) {
        loading()
        val result = Apis.User.updatePassword(updatePassword)
        success(result.message.toString())
    }
}