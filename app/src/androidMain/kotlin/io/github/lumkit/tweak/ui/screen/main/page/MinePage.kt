package io.github.lumkit.tweak.ui.screen.main.page

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import io.github.lumkit.tweak.LocalScreenNavigationController
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.common.util.AuthorizationUtils
import io.github.lumkit.tweak.data.LoadState
import io.github.lumkit.tweak.data.watch
import io.github.lumkit.tweak.net.Apis
import io.github.lumkit.tweak.ui.component.dialog.LoginDialog
import io.github.lumkit.tweak.ui.component.dialog.UserSelfDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import tweak_android.app.generated.resources.Res
import tweak_android.app.generated.resources.logo

@Composable
fun MinePage() {

    val navHostController = LocalScreenNavigationController.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val observer = remember {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val token = AuthorizationUtils.load()
                    LoginViewModel.loginState = token != null
                }
                else -> Unit
            }
        }
    }

    // 生命周期订阅
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        UserDetail(navHostController, context)

    }
}

@Composable
private fun UserDetail(navHostController: NavHostController, context: Context) {
    AnimatedContent(
        targetState = LoginViewModel.loginState
    ) {
        if (it) {
            LoginCard(navHostController, context)
        } else {
            UnLoginCard(navHostController)
        }
    }
}

@Composable
private fun LoginCard(navHostController: NavHostController, context: Context, userViewModel: UserViewModel = viewModel { UserViewModel() }) {

    var userInfoDialogState by rememberSaveable { mutableStateOf(false) }
    val userState by userViewModel.userState.collectAsStateWithLifecycle()
    val loadState = userViewModel.loadState.collectAsStateWithLifecycle()

    LaunchedEffect(userViewModel) {
        userViewModel.user()

        loadState.watch("user") {
            when (it) {
                is LoadState.Fail -> {
                    userViewModel.clearLoadState("user")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                    }
                }
                is LoadState.Loading -> Unit
                is LoadState.Success -> Unit
                null -> Unit
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                userInfoDialogState = true
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(Apis.User.avatar(userState?.username))
                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .clip(CircleShape)
                .border(.5.dp, DividerDefaults.color, CircleShape)
                .size(32.dp),
            error = painterResource(Res.drawable.logo),
            placeholder = painterResource(Res.drawable.logo)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Text(
                text = userState?.nickname ?: stringResource(R.string.text_unknown),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = buildString {
                    append(stringResource(R.string.text_user_id))
                    append(userState?.username)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Icon(
            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }

    UserSelfDialog(
        visible = userInfoDialogState,
        onDismissRequest = { userInfoDialogState = false },
        viewModel = userViewModel
    )
}

@Composable
private fun UnLoginCard(navHostController: NavHostController) {
    var loginDialogState by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.outlinedCardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    loginDialogState = true
                }
            ) {
                Text(
                    text = stringResource(R.string.text_login_and_register)
                )
            }
            Text(
                text = stringResource(R.string.text_un_login_tip),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }

    LoginDialog(
        visible = loginDialogState,
        onDismissRequest = { loginDialogState = false }
    )
}