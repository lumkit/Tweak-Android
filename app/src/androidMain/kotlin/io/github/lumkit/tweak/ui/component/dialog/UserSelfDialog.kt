package io.github.lumkit.tweak.ui.component.dialog

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.data.LoadState
import io.github.lumkit.tweak.data.asString
import io.github.lumkit.tweak.data.color
import io.github.lumkit.tweak.data.watch
import io.github.lumkit.tweak.model.Config
import io.github.lumkit.tweak.net.Apis
import io.github.lumkit.tweak.net.pojo.CaptchaType
import io.github.lumkit.tweak.net.pojo.User
import io.github.lumkit.tweak.net.pojo.isVip
import io.github.lumkit.tweak.net.pojo.resp.BindEmail
import io.github.lumkit.tweak.net.pojo.resp.GetCaptchaParam
import io.github.lumkit.tweak.net.pojo.resp.UpdatePassword
import io.github.lumkit.tweak.ui.component.FolderItem
import io.github.lumkit.tweak.ui.screen.main.page.UserViewModel
import io.github.lumkit.tweak.common.util.FieldValidatorUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun UserSelfDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    context: Context = LocalContext.current,
    viewModel: UserViewModel,
) {
    if (visible) {
        val user by viewModel.userState.collectAsStateWithLifecycle()
        val loadState = viewModel.loadState.collectAsStateWithLifecycle()

        LaunchedEffect(viewModel) {
            viewModel.user()

            loadState.watch("user") {
                when (it) {
                    is LoadState.Fail -> {
                        viewModel.clearLoadState("user")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                        }
                        onDismissRequest()
                    }
                    is LoadState.Loading -> Unit
                    is LoadState.Success -> Unit
                    null -> Unit
                }
            }
        }

        AlertDialog(
            onDismissRequest,
            text = {
                user?.let { InfoBox(user = it, viewModel, context) }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissRequest()
                        Apis.clearToken()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors().copy(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = stringResource(R.string.text_log_out)
                    )
                }
            }
        )
    }
}

@Composable
private fun InfoBox(
    user: User,
    viewModel: UserViewModel,
    context: Context,
) {
    val updateEmail = rememberSaveable { mutableStateOf(false) }
    val updateUser = rememberSaveable { mutableStateOf(false) }
    val updatePassword = rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(Apis.User.avatar(user.username))
                    .diskCachePolicy(CachePolicy.DISABLED) // 禁用磁盘缓存
                    .memoryCachePolicy(CachePolicy.DISABLED) // 禁用内存缓存
                    .build(),
                contentDescription = null,
                error = painterResource(R.drawable.logo),
                placeholder = painterResource(R.drawable.logo),
                modifier = Modifier
                    .clip(CircleShape)
                    .size(65.dp)
                    .border(.5.dp, DividerDefaults.color, CircleShape)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = "${user.nickname}",
                    style = MaterialTheme.typography.titleMedium,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    color = if (user.isVip()) Color(0xfffc583d) else MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "${stringResource(R.string.text_user_id)}${user.username}",
                    style = MaterialTheme.typography.labelLarge,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
                Surface(
                    color = user.status?.color() ?: MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = user.status?.asString() ?: stringResource(R.string.text_user),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(vertical = 2.dp, horizontal = 6.dp),
                        color = Color.White
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.user_sign),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(8.dp))
        var exp by remember { mutableStateOf(false) }
        Text(
            text = user.signature?.trim()?.ifEmpty { stringResource(R.string.text_not_has_user_sign) } ?: stringResource(R.string.text_not_has_user_sign),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                    exp = !exp
                }
                .animateContentSize(),
            overflow = TextOverflow.Ellipsis,
            maxLines = if (exp) Int.MAX_VALUE else 5,
        )
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FolderItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        updateEmail.value = true
                    },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_email),
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(8.dp),
                    )
                },
                title = {
                    Column {
                        Text(
                            text = "${user.email}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.text_bind_email),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_right),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            FolderItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        updateUser.value = true
                    },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(8.dp),
                    )
                },
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.text_update_user_info),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_right),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            FolderItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        updatePassword.value = true
                    },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_password),
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(8.dp),
                    )
                },
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.text_update_password),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_right),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    UpdateEmail(user = user, visible = updateEmail, userViewModel = viewModel, context = context)
    UpdateUser(user = user, visible = updateUser, userViewModel = viewModel, context = context)
    UpdatePassword(user = user, visible = updatePassword, userViewModel = viewModel, context = context)
}

@Composable
fun InputBox(
    modifier: Modifier = Modifier,
    text: String,
    placeholderText: String = "",
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    trailingIcon: @Composable () -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.09f),
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = placeholderText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                )
            }
            trailingIcon()
        }
    }
}

@Composable
private fun UpdateEmail(
    user: User,
    visible: MutableState<Boolean>,
    userViewModel: UserViewModel,
    context: Context
) {

    val loadState = userViewModel.loadState.collectAsStateWithLifecycle()

    var newEmail by remember { mutableStateOf("") }
    var newCaptcha by remember { mutableStateOf("") }
    var oldCaptcha by remember { mutableStateOf("") }

    var buttonEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(newEmail, newCaptcha, oldCaptcha, loadState.value) {
        buttonEnabled = FieldValidatorUtil.emailValid(newEmail)
                && newCaptcha.trim().isNotEmpty()
                && oldCaptcha.trim().isNotEmpty()
                && loadState.value["getCaptcha"] !is LoadState.Loading
                && loadState.value["bindEmail"] !is LoadState.Loading
    }

    LaunchedEffect(Unit) {
        loadState.watch("getCaptcha") {
            when (it) {
                is LoadState.Fail -> {
                    userViewModel.clearLoadState("getCaptcha")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                    }
                }
                is LoadState.Loading -> Unit
                is LoadState.Success -> {
                    userViewModel.clearLoadState("getCaptcha")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                    }
                }
                null -> Unit
            }
        }
    }

    LaunchedEffect(Unit) {
        loadState.watch("bindEmail") {
            when (it) {
                is LoadState.Fail -> {
                    userViewModel.clearLoadState("bindEmail")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                    }
                }
                is LoadState.Loading -> Unit
                is LoadState.Success -> {
                    userViewModel.clearLoadState("bindEmail")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                    }
                }
                null -> Unit
            }
        }
    }

    if (visible.value) {
        AlertDialog(
            onDismissRequest = { visible.value = false },
            title = {
                Text(
                    text = stringResource(R.string.text_bind_email)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InputBox(
                            modifier = Modifier,
                            text = newEmail,
                            placeholderText = stringResource(R.string.text_input_new_email),
                            onValueChange = { newEmail = it },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_cross),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(
                                        enabled = true,
                                        interactionSource = null,
                                        indication = null,
                                        onClick = {
                                            newEmail = ""
                                        }
                                    ),
                            )
                        }

                        run {
                            InputBox(
                                text = newCaptcha,
                                placeholderText = stringResource(R.string.text_input_new_email_captcha),
                                onValueChange = { newCaptcha = it },
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

                            FilledTonalButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    if (!FieldValidatorUtil.emailValid(newEmail)) {
                                        Toast.makeText(context, R.string.text_please_input_email, Toast.LENGTH_SHORT).show()
                                    } else {
                                        clickable = false
                                        userViewModel.getCaptcha(
                                            GetCaptchaParam(
                                                email = newEmail,
                                                type = CaptchaType.BindEmail
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

                        run {
                            InputBox(
                                text = oldCaptcha,
                                placeholderText = stringResource(R.string.text_input_old_email_captcha),
                                onValueChange = { oldCaptcha = it },
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

                            FilledTonalButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    clickable = false
                                    userViewModel.getCaptcha(
                                        GetCaptchaParam(
                                            email = user.email ?: "",
                                            type = CaptchaType.BindEmail
                                        ),
                                        onComplete = {
                                            clickable = true
                                        }
                                    ) {
                                        captchaEnabled = false
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                enabled = captchaEnabled && clickable,
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
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!FieldValidatorUtil.emailValid(newEmail)) {
                            Toast.makeText(context, R.string.text_new_email_check_fail, Toast.LENGTH_SHORT).show()
                        } else if (newCaptcha.trim().isEmpty() || oldCaptcha.trim().isEmpty()) {
                            Toast.makeText(context, R.string.text_captcha_cannot_blank, Toast.LENGTH_SHORT).show()
                        } else {
                            userViewModel.bindEmail(
                                BindEmail(
                                    newEmail = newEmail,
                                    newCaptcha = newCaptcha,
                                    oldCaptcha = oldCaptcha,
                                )
                            ) {
                                visible.value = false
                                Apis.clearToken()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = buttonEnabled,
                ) {
                    Text(
                        text = stringResource(R.string.text_bind_new_email)
                    )
                }
            }
        )
    }
}

@Composable
private fun UpdateUser(
    user: User,
    visible: MutableState<Boolean>,
    userViewModel: UserViewModel,
    context: Context
) {

    var buttonEnabled by remember { mutableStateOf(true) }
    var originPath by rememberSaveable { mutableStateOf("") }
    var iconPath by rememberSaveable { mutableStateOf("") }
    var clipDialogState by remember { mutableStateOf(false) }

    var nickname by rememberSaveable { mutableStateOf("") }
    var signature by rememberSaveable { mutableStateOf("") }

    val loadState = userViewModel.loadState.collectAsStateWithLifecycle()

    val activityResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) {
        it?.let { uri: Uri ->
            context.contentResolver.openInputStream(uri)?.use { input ->
                val cacheFile = File(Config.Path.appCacheDir, "clip.jpg")
                cacheFile.outputStream().use { os ->
                    input.copyTo(os)
                }
                originPath = cacheFile.absolutePath
                clipDialogState = true
            }
        }
    }

    LaunchedEffect(user) {
        iconPath = Apis.User.avatar(user.username)

        nickname = user.nickname ?: ""
        signature = user.signature ?: ""
    }

    LaunchedEffect(loadState) {
        loadState.watch("updateUser") {
            buttonEnabled = it !is LoadState.Loading
            when (it) {
                is LoadState.Fail -> {
                    userViewModel.clearLoadState("updateUser")
                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                }
                is LoadState.Loading -> Unit
                is LoadState.Success -> {
                    userViewModel.clearLoadState("updateUser")
                    visible.value = false
                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                    userViewModel.user()
                }
                null -> Unit
            }
        }
    }

    if (visible.value) {
        AlertDialog(
            onDismissRequest = {
                visible.value = false
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(85.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                modifier = Modifier.fillMaxSize(),
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(iconPath)
                                    .diskCachePolicy(CachePolicy.DISABLED) // 禁用磁盘缓存
                                    .memoryCachePolicy(CachePolicy.DISABLED) // 禁用内存缓存
                                    .build(),
                                contentDescription = null,
                            )

                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        activityResult.launch("image/*")
                                    },
                                color = Color(0xAA000000),
                            ) { }

                            Icon(
                                painter = painterResource(R.drawable.ic_camera),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFFFFFFFF)
                            )
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "昵称",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.width(60.dp)
                                )
                                BasicTextField(
                                    value = nickname,
                                    onValueChange = { nickname = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                                )
                                Text(
                                    text = "${nickname.length}/20",
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(60.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DividerDefaults.color
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.user_sign),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(60.dp)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "${signature.length}/100",
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.width(60.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DividerDefaults.color
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                BasicTextField(
                                    value = signature,
                                    onValueChange = { signature = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nickname.contains(" ") || nickname.contains("\n") || nickname.contains("\r")) {
                            Toast.makeText(context, R.string.text_nickname_cannot_blank_char, Toast.LENGTH_SHORT).show()
                        } else if (nickname.trim().isEmpty()) {
                            Toast.makeText(context, R.string.text_nickname_cannot_blank, Toast.LENGTH_SHORT).show()
                        } else if (nickname.length > 20) {
                            Toast.makeText(context, R.string.text_nickname_len_check, Toast.LENGTH_SHORT).show()
                        } else if (signature.length > 100) {
                            Toast.makeText(context, R.string.text_user_sign_len_check, Toast.LENGTH_SHORT).show()
                        } else {
                            userViewModel.updateUser(
                                updateUser = io.github.lumkit.tweak.net.pojo.resp.UpdateUser(nickname, signature)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = buttonEnabled,
                ) {
                    Text(
                        text = "保存"
                    )
                }
            }
        )
    }

    var uploadState by remember { mutableStateOf(true) }
    var cacheImagePath by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(loadState) {
        loadState.watch("uploadAvatar") {
            when (it) {
                is LoadState.Fail -> {
                    userViewModel.clearLoadState("uploadAvatar")
                    uploadState = true

                }
                is LoadState.Loading -> Unit
                is LoadState.Success -> {
                    userViewModel.clearLoadState("uploadAvatar")
                    iconPath = cacheImagePath
                    uploadState = true
                    userViewModel.user()
                }
                null -> Unit
            }
        }
    }

    ClipImageDialog(
        visible = clipDialogState,
        onDismissRequest = {
            originPath = ""
            clipDialogState = false
        },
        title = "裁剪图片",
        originFilePath = originPath,
    ) {
        originPath = ""
        if (uploadState) {
            cacheImagePath = it
            uploadState = false
            userViewModel.uploadAvatar(it)
        }
    }
}

@Composable
private fun UpdatePassword(
    user: User,
    visible: MutableState<Boolean>,
    userViewModel: UserViewModel,
    context: Context,
) {

    val loadState = userViewModel.loadState.collectAsStateWithLifecycle()

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newConfirmPassword by remember { mutableStateOf("") }
    var captcha by remember { mutableStateOf("") }

    var buttonEnabled by remember { mutableStateOf(true ) }

    LaunchedEffect(loadState) {
        loadState.watch("updatePassword") {
            buttonEnabled = it !is LoadState.Loading

            when (it) {
                is LoadState.Fail -> {
                    userViewModel.clearLoadState("updatePassword")
                    visible.value = false
                }
                is LoadState.Loading -> Unit
                is LoadState.Success -> {
                    userViewModel.clearLoadState("updatePassword")
                    Apis.clearToken()
                    visible.value = false
                }
                null -> Unit
            }
        }
    }

    if (visible.value) {
        AlertDialog(
            onDismissRequest = {
                visible.value = false
            },
            title = {
                Text("修改密码")
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InputBox(
                            modifier = Modifier,
                            text = oldPassword,
                            placeholderText = "输入旧密码",
                            onValueChange = { oldPassword = it },
                            isPassword = true,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_cross),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(
                                        enabled = true,
                                        interactionSource = null,
                                        indication = null,
                                        onClick = {
                                            oldPassword = ""
                                        }
                                    ),
                            )
                        }

                        InputBox(
                            modifier = Modifier,
                            text = newPassword,
                            placeholderText = "输入新密码",
                            onValueChange = { newPassword = it },
                            isPassword = true,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_cross),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(
                                        enabled = true,
                                        interactionSource = null,
                                        indication = null,
                                        onClick = {
                                            newPassword = ""
                                        }
                                    ),
                            )
                        }

                        InputBox(
                            modifier = Modifier,
                            text = newConfirmPassword,
                            placeholderText = "确认新密码",
                            onValueChange = { newConfirmPassword = it },
                            isPassword = true,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_cross),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(
                                        enabled = true,
                                        interactionSource = null,
                                        indication = null,
                                        onClick = {
                                            newConfirmPassword = ""
                                        }
                                    ),
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            InputBox(
                                modifier = Modifier.weight(1f),
                                text = captcha,
                                placeholderText = "输入验证码",
                                onValueChange = { captcha = it },
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

                            FilledTonalButton(
                                onClick = {
                                    clickable = true
                                    userViewModel.getCaptcha(
                                        GetCaptchaParam(
                                            email = user.email.toString(),
                                            type = CaptchaType.Password
                                        ),
                                        onComplete = {
                                            clickable = false
                                        }
                                    ) {
                                        captchaEnabled = false
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

                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!FieldValidatorUtil.passwordValid(newPassword)) {
                            Toast.makeText(
                                context,
                                "密码格式不正确！(6 ~ 20位字符可包含@._)",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (newPassword != newConfirmPassword) {
                            Toast.makeText(context, "两次密码不一致", Toast.LENGTH_SHORT).show()
                        } else if (captcha.isEmpty()) {
                            Toast.makeText(context, "验证码不能为空", Toast.LENGTH_SHORT).show()
                        } else {
                            userViewModel.updatePassword(
                                UpdatePassword(
                                    newConfirmPassword, oldPassword, captcha
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = buttonEnabled,
                ) {
                    Text(
                        text = "修改密码"
                    )
                }
            }
        )
    }
}