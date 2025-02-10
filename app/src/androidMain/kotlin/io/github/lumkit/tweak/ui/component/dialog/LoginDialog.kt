package io.github.lumkit.tweak.ui.component.dialog

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.data.LoadState
import io.github.lumkit.tweak.data.watch
import io.github.lumkit.tweak.net.Apis
import io.github.lumkit.tweak.net.pojo.CaptchaType
import io.github.lumkit.tweak.net.pojo.resp.GetCaptchaParam
import io.github.lumkit.tweak.net.pojo.resp.GetUserParam
import io.github.lumkit.tweak.net.pojo.resp.RegisterParam
import io.github.lumkit.tweak.net.pojo.resp.ResetPassWord
import io.github.lumkit.tweak.ui.component.AnimatedLogo
import io.github.lumkit.tweak.ui.local.LocalStorageStore
import io.github.lumkit.tweak.ui.local.StorageStore
import io.github.lumkit.tweak.ui.screen.main.page.LoginViewModel
import io.github.lumkit.tweak.util.Aes
import io.github.lumkit.tweak.util.FieldValidatorUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import tweak_android.app.generated.resources.Res
import tweak_android.app.generated.resources.logo

@Composable
fun LoginDialog(
    context: Context = LocalContext.current,
    storageStore: StorageStore = LocalStorageStore.current,
    viewModel: LoginViewModel = viewModel { LoginViewModel(context, storageStore) },
    visible: Boolean,
    onDismissRequest: () -> Unit
) {
    if (visible) {

        val pageState by viewModel.pageState.collectAsStateWithLifecycle()
        val enabled = rememberSaveable { mutableStateOf(true) }

        val loadState = viewModel.loadState.collectAsStateWithLifecycle()

        val username by viewModel.usernameState.collectAsStateWithLifecycle()
        val password by viewModel.passwordState.collectAsStateWithLifecycle()
        val confirmPassword by viewModel.confirmPasswordState.collectAsStateWithLifecycle()
        val email by viewModel.emailState.collectAsStateWithLifecycle()
        val captcha by viewModel.captchaState.collectAsStateWithLifecycle()

        LaunchedEffect(viewModel) {
            loadState.watch("login") {
                when (it) {
                    is LoadState.Fail -> {
                        viewModel.clearLoadState("login")
                        enabled.value = true
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    is LoadState.Loading -> {
                        enabled.value = false
                    }
                    is LoadState.Success -> {
                        viewModel.clearLoadState("login")
                        enabled.value = true
                        onDismissRequest()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                        }
                        LoginViewModel.loginState = true
                        storageStore.putString("username", username)
                        storageStore.putString("password", Aes.encrypt(context, password) ?: "")
                    }
                    null -> Unit
                }
            }
        }

        LaunchedEffect(loadState) {
            loadState.watch("getCaptcha") {
                when (it) {
                    is LoadState.Fail -> {
                        viewModel.clearLoadState("getCaptcha")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    is LoadState.Loading -> Unit
                    is LoadState.Success -> {
                        viewModel.clearLoadState("getCaptcha")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    null -> Unit
                }
            }
        }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = when (pageState) {
                        LoginViewModel.LoginPageState.LOGIN -> stringResource(R.string.title_login)
                        LoginViewModel.LoginPageState.REGISTER -> stringResource(R.string.title_register)
                    }
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(Apis.User.avatar(username))
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(vertical = 32.dp)
                            .clip(CircleShape)
                            .align(Alignment.CenterHorizontally)
                            .size(80.dp),
                        error = painterResource(Res.drawable.logo),
                        placeholder = painterResource(Res.drawable.logo)
                    )
                    InputBox(viewModel, enabled, context)
                }
            },
            confirmButton = {

                LaunchedEffect(loadState) {
                    loadState.watch("register") {
                        when (it) {
                            is LoadState.Fail -> {
                                viewModel.clearLoadState("register")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                            is LoadState.Loading -> Unit
                            is LoadState.Success -> {
                                viewModel.clearLoadState("register")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                                }
                                viewModel.setPageState(LoginViewModel.LoginPageState.LOGIN)
                            }
                            null -> Unit
                        }
                    }
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        when (pageState) {
                            LoginViewModel.LoginPageState.LOGIN -> {
                                if (!FieldValidatorUtil.usernameValid(username)) {
                                    Toast.makeText(context, R.string.text_id_check_fail, Toast.LENGTH_SHORT).show()
                                } else if (!FieldValidatorUtil.passwordValid(password)) {
                                    Toast.makeText(context, R.string.text_password_check_fail, Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.login(
                                        getUserParam = GetUserParam(
                                            username = if (!FieldValidatorUtil.emailValid(username)) username else null,
                                            email = if (FieldValidatorUtil.emailValid(username)) username else null,
                                            password = password
                                        )
                                    )
                                }
                            }
                            LoginViewModel.LoginPageState.REGISTER -> {
                                if (!FieldValidatorUtil.usernameValid(username)) {
                                    Toast.makeText(
                                        context,
                                        "账号格式错误.（6 ~ 16位由数字、字母或符号._组成的文本）",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (!FieldValidatorUtil.passwordValid(password)) {
                                    Toast.makeText(
                                        context,
                                        "密码格式错误。（6 ~ 20位由数字、字母或符号._@组成的文本）",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (password != confirmPassword) {
                                    Toast.makeText(
                                        context,
                                        "两次输入的密码不一致！",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (!FieldValidatorUtil.emailValid(email)) {
                                    Toast.makeText(
                                        context,
                                        "电子邮箱格式错误！",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (captcha.trim().isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "验证码不能为空！",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    viewModel.register(
                                        RegisterParam(
                                            username = username,
                                            email = email,
                                            password = password,
                                            captcha = captcha,
                                        )
                                    )
                                }
                            }
                        }
                    },
                    enabled = enabled.value
                ) {
                    Text(
                        text = when (pageState) {
                            LoginViewModel.LoginPageState.LOGIN -> stringResource(R.string.text_login)
                            LoginViewModel.LoginPageState.REGISTER -> stringResource(R.string.text_register)
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun ColumnScope.InputBox(
    viewModel: LoginViewModel,
    enabled: MutableState<Boolean>,
    context: Context
) {

    val pageState by viewModel.pageState.collectAsStateWithLifecycle()
    val username by viewModel.usernameState.collectAsStateWithLifecycle()
    val password by viewModel.passwordState.collectAsStateWithLifecycle()
    val confirmPassword by viewModel.confirmPasswordState.collectAsStateWithLifecycle()
    val email by viewModel.emailState.collectAsStateWithLifecycle()
    val captcha by viewModel.captchaState.collectAsStateWithLifecycle()

    val resetPassWordState = rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = username,
        onValueChange = viewModel::setUsername,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = {
            Text(
                text = stringResource(R.string.text_username)
            )
        },
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = password,
        onValueChange = viewModel::setPassword,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = {
            Text(
                text = stringResource(R.string.text_password)
            )
        },
        visualTransformation = PasswordVisualTransformation(),
    )

    AnimatedVisibility(
        visible = pageState == LoginViewModel.LoginPageState.REGISTER
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = viewModel::setConfirmPassword,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text(
                        text = stringResource(R.string.text_confirm_password)
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
            )

            OutlinedTextField(
                value = email,
                onValueChange = viewModel::setEmail,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text(
                        text = stringResource(R.string.text_email)
                    )
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = captcha,
                    onValueChange = viewModel::setCaptcha,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    singleLine = true,
                    label = {
                        Text(
                            text = stringResource(R.string.text_captcha)
                        )
                    },
                )

                var captchaEnabled by rememberSaveable { mutableStateOf(true) }
                var clickable by rememberSaveable { mutableStateOf(true) }
                var time by rememberSaveable { mutableStateOf(-1) }

                LaunchedEffect(captchaEnabled) {
                    if (!captchaEnabled) {
                        time = 60
                        while (time >= 0) {
                            delay(1000)
                            time--
                        }
                        captchaEnabled = true
                    }
                }

                TextButton(
                    onClick = {
                        if (!FieldValidatorUtil.emailValid(email)) {
                            Toast.makeText(context, "请输入正确的电子邮箱！", Toast.LENGTH_SHORT).show()
                        } else {
                            clickable = false
                            viewModel.getCaptcha(
                                GetCaptchaParam(
                                    email = email,
                                    type = CaptchaType.Register
                                ),
                                onComplete = {
                                    clickable = true
                                }
                            ) {
                                captchaEnabled = false
                            }
                        }
                    },
                    enabled = enabled.value && captchaEnabled && clickable
                ) {
                    Text(
                        text =  if (captchaEnabled) {
                            stringResource(R.string.text_get_captcha)
                        } else {
                            "$time S"
                        }
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = buildAnnotatedString {
                when (pageState) {
                    LoginViewModel.LoginPageState.LOGIN -> append(stringResource(R.string.text_no_id))
                    LoginViewModel.LoginPageState.REGISTER -> append(stringResource(R.string.text_has_id))
                }
                withLink(
                    link = LinkAnnotation.Clickable(
                        tag = "to register"
                    ) {
                        viewModel.setPageState(
                            state = when (pageState) {
                                LoginViewModel.LoginPageState.LOGIN -> LoginViewModel.LoginPageState.REGISTER
                                LoginViewModel.LoginPageState.REGISTER -> LoginViewModel.LoginPageState.LOGIN
                            }
                        )
                    }
                ) {
                    withStyle(
                        style = SpanStyle(color = MaterialTheme.colorScheme.primary)
                    ) {
                        when (pageState) {
                            LoginViewModel.LoginPageState.LOGIN -> append(stringResource(R.string.text_to_register))
                            LoginViewModel.LoginPageState.REGISTER -> append(stringResource(R.string.text_to_login))
                        }
                    }
                }
            },
            style = MaterialTheme.typography.labelSmall
        )

        Text(
            text = buildAnnotatedString {
                withLink(
                    link = LinkAnnotation.Clickable("") {
                        resetPassWordState.value = true
                    }
                ) {
                    append(stringResource(R.string.text_reset_password))
                }
            },
            style = MaterialTheme.typography.labelSmall
        )
    }

    ResetPassWordDialog(context, viewModel, resetPassWordState)
}

@Composable
fun ResetPassWordDialog(
    context: Context,
    viewModel: LoginViewModel,
    visible: MutableState<Boolean>
) {
    if (visible.value) {
        AlertDialog(
            onDismissRequest = {
                visible.value = false
            },
            title = {
                Text(text = stringResource(R.string.text_reset_password))
            },
            text = {
                EditBox(
                    context = context,
                    loginViewModel = viewModel,
                    onCloseable = {
                        visible.value = false
                        Apis.clearToken()
                    }
                )
            },
            confirmButton = {

            }
        )
    }
}

@Composable
private fun EditBox(
    context: Context,
    loginViewModel: LoginViewModel,
    onCloseable: () -> Unit
) {
    val idState by loginViewModel.usernameState.collectAsStateWithLifecycle()
    val passwordState by loginViewModel.passwordState.collectAsStateWithLifecycle()
    val cPasswordState by loginViewModel.confirmPasswordState.collectAsStateWithLifecycle()
    val emailState by loginViewModel.emailState.collectAsStateWithLifecycle()
    val captchaState by loginViewModel.captchaState.collectAsStateWithLifecycle()

    val loadState = loginViewModel.loadState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedLogo(
            modifier = Modifier
                .padding(vertical = 32.dp)
                .size(80.dp),
            isStart = true,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.size(28.dp))
            // 账号
            InputBox(
                modifier = Modifier,
                text = idState,
                placeholderText = "输入Tweak账号",
                onValueChange = { loginViewModel.setUsername(it) },
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_cross),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(
                            enabled = true,
                            interactionSource = null,
                            indication = null,
                            onClick = {
                                loginViewModel.setUsername("")
                            }
                        ),
                )
            }

            Spacer(modifier = Modifier.size(12.dp))
            InputBox(
                modifier = Modifier,
                text = passwordState,
                placeholderText = "输入Tweak新密码",
                isPassword = true,
                onValueChange = { loginViewModel.setPassword(it) },
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_cross),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(
                            enabled = true,
                            interactionSource = null,
                            indication = null,
                            onClick = {
                                loginViewModel.setPassword("")
                            }
                        ),
                )
            }

            Spacer(modifier = Modifier.size(12.dp))
            InputBox(
                modifier = Modifier,
                text = cPasswordState,
                placeholderText = "再次输入新密码",
                isPassword = true,
                onValueChange = { loginViewModel.setConfirmPassword(it) },
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_cross),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(
                            enabled = true,
                            interactionSource = null,
                            indication = null,
                            onClick = {
                                loginViewModel.setConfirmPassword("")
                            }
                        ),
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            InputBox(
                modifier = Modifier,
                text = emailState,
                placeholderText = "输入Tweak绑定的电子邮箱",
                onValueChange = { loginViewModel.setEmail(it) },
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_cross),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(
                            enabled = true,
                            interactionSource = null,
                            indication = null,
                            onClick = {
                                loginViewModel.setEmail("")
                            }
                        ),
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Row (
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InputBox(
                    modifier = Modifier.weight(1f),
                    text = captchaState,
                    placeholderText = "输入验证码",
                    onValueChange = { loginViewModel.setCaptcha(it) },
                )

                var captchaEnabled by rememberSaveable { mutableStateOf(true) }
                var clickable by rememberSaveable { mutableStateOf(true) }
                var time by rememberSaveable { mutableStateOf(-1) }

                LaunchedEffect(captchaEnabled) {
                    if (!captchaEnabled) {
                        time = 60
                        while (time >= 0) {
                            delay(1000)
                            time --
                        }
                        captchaEnabled = true
                    }
                }

                TextButton(
                    onClick = {
                        if (!FieldValidatorUtil.emailValid(emailState)) {
                            Toast.makeText(
                                context,
                                "请输入正确的电子邮箱！",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            clickable = false
                            loginViewModel.getCaptcha(
                                GetCaptchaParam(
                                    email = emailState,
                                    type = CaptchaType.Password
                                ),
                                onComplete = {
                                    clickable = true
                                }
                            ) {
                                captchaEnabled = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    enabled = captchaEnabled && clickable,
                    modifier = Modifier.width(120.dp)
                ) {
                    Text(
                        text = if (captchaEnabled) {
                            stringResource(R.string.text_get_captcha)
                        } else {
                            "$time S"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.size(16.dp))
            var enabled by rememberSaveable { mutableStateOf(true) }

            LaunchedEffect(idState, passwordState, loadState.value, cPasswordState, captchaState, emailState) {
                enabled = idState.isNotEmpty() && FieldValidatorUtil.passwordValid(passwordState)
                        && loadState.value["resetPassword"] !is LoadState.Loading
                        && FieldValidatorUtil.passwordValid(cPasswordState)
                        && FieldValidatorUtil.emailValid(emailState)
                        && captchaState.isNotEmpty()
            }

            LaunchedEffect(loadState) {
                loadState.watch("resetPassword") {
                    when (it) {
                        is LoadState.Fail -> {
                            loginViewModel.clearLoadState("resetPassword")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                        is LoadState.Loading -> Unit
                        is LoadState.Success -> {
                            loginViewModel.clearLoadState("resetPassword")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                            }
                            onCloseable()
                        }
                        null -> Unit
                    }
                }
            }

            Button(
                onClick = {
                    if (!FieldValidatorUtil.usernameValid(idState)) {
                        Toast.makeText(
                            context,
                            "账号格式错误.（6 ~ 16位由数字、字母或符号._组成的文本）",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (!FieldValidatorUtil.passwordValid(passwordState)) {
                        Toast.makeText(
                            context,
                            "密码格式错误。（6 ~ 20位由数字、字母或符号._@组成的文本）",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (passwordState != cPasswordState) {
                        Toast.makeText(
                            context,
                            "两次输入的密码不一致！",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (!FieldValidatorUtil.emailValid(emailState)) {
                        Toast.makeText(
                            context,
                            "电子邮箱格式错误！",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (captchaState.trim().isEmpty()) {
                        Toast.makeText(context, "验证码不能为空！", Toast.LENGTH_SHORT).show()
                    } else {
                        loginViewModel.resetPassword(
                            ResetPassWord(
                                username = idState,
                                password = passwordState,
                                email = emailState,
                                captcha = captchaState,
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                enabled = enabled,
            ) {
                Text(
                    text = "重置密码"
                )
            }
        }
    }
}