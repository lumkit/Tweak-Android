package io.github.lumkit.tweak.ui.screen.runtime

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lumkit.tweak.LocalScreenNavigationController
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.common.shell.provide.ReusableShells
import io.github.lumkit.tweak.common.util.makeText
import io.github.lumkit.tweak.model.Config
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.component.AnimatedLogo
import io.github.lumkit.tweak.ui.component.FolderItem
import io.github.lumkit.tweak.ui.local.LocalStorageStore
import io.github.lumkit.tweak.ui.local.StorageStore
import io.github.lumkit.tweak.ui.screen.ScreenRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun RuntimeModeScreen(
    activity: ComponentActivity = LocalContext.current as ComponentActivity,
    storageStore: StorageStore = LocalStorageStore.current,
    viewModel: RuntimeModeViewModel = viewModel {
        RuntimeModeViewModel(
            context = activity,
            storageStore = storageStore
        )
    }
) {
    LaunchedEffect(viewModel) {
        if (storageStore.getBoolean(Const.APP_ACCEPT_RISK)) {
            viewModel.initRootModeConfig {
                Toast.makeText(activity, it.makeText(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedLogo(
                modifier = Modifier
                    .padding(vertical = 32.dp)
                    .size(160.dp),
                isStart = true
            )
            Text(
                text = stringResource(R.string.text_runtime_mode_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = .45f),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = stringResource(R.string.text_runtime_mode_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(16.dp))
            FolderItems(viewModel, storageStore)
        }
    }
}

@Composable
private fun FolderItems(viewModel: RuntimeModeViewModel, storageStore: StorageStore) {

    val activity = LocalContext.current as ComponentActivity

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FolderItem(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                viewModel.setRootModeDialogState(true)
            },
            icon = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = .55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pound),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxSize(),
                        tint = MaterialTheme.colorScheme.surface
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.text_runtime_mode_root_mode)
                )
            },
            subtitle = {
                Text(
                    text = stringResource(R.string.text_runtime_mode_root_mode_tips)
                )
            },
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_right),
                    contentDescription = null,
                )
            }
        )
    }

    UseProtocolDialog(viewModel, activity, storageStore)
    CheckPermissionsDialog(viewModel, activity, storageStore)
}

@Composable
private fun UseProtocolDialog(
    viewModel: RuntimeModeViewModel,
    activity: ComponentActivity,
    storageStore: StorageStore
) {
    val rootModeDialogState by viewModel.rootModeDialogState.collectAsStateWithLifecycle()

    if (rootModeDialogState) {
        var acceptRisk by remember { mutableStateOf(false) }
        var countdown by remember { mutableIntStateOf(8) }

        LaunchedEffect(Unit) {
            acceptRisk = storageStore.getBoolean(Const.APP_ACCEPT_RISK)
        }

        LaunchedEffect(Unit) {
            while (isActive && countdown >= 0) {
                delay(1000)
                countdown--
            }
        }

        AlertDialog(
            onDismissRequest = {
                viewModel.setRootModeDialogState(false)
            },
            title = {
                Text(
                    text = stringResource(R.string.text_use_protocol)
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.text_use_protocol_content),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .weight(1f, false)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FolderItem(
                        modifier = Modifier.fillMaxWidth(),
                        icon = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.error.copy(.8f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_dangerous),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxSize(),
                                    tint = MaterialTheme.colorScheme.surface
                                )
                            }
                        },
                        title = {
                            Text(
                                text = stringResource(R.string.text_accept_risk),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        subtitle = {
                            Text(
                                text = stringResource(R.string.text_accept_risk_tips),
                            )
                        },
                        trailingIcon = {
                            Switch(
                                checked = acceptRisk,
                                onCheckedChange = { acceptRisk = it }
                            )
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.initRootModeConfig {
                            Toast.makeText(activity, it.makeText(), Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = countdown < 0 && acceptRisk
                ) {
                    Text(
                        text = if (countdown < 0) {
                            stringResource(R.string.text_accept_and_continue)
                        } else {
                            "${countdown}S"
                        }
                    )
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = {
                        viewModel.setRootModeDialogState(false)
                        activity.finish()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.text_cancel)
                    )
                }
            }
        )
    }
}

@Composable
private fun CheckPermissionsDialog(
    viewModel: RuntimeModeViewModel,
    activity: ComponentActivity,
    storageStore: StorageStore
) {
    val ioScope = rememberCoroutineScope{ Dispatchers.IO }
    val initConfigDialogState by viewModel.initConfigDialogState.collectAsStateWithLifecycle()
    val initConfigDialogMessage by viewModel.initConfigDialogMessage.collectAsStateWithLifecycle()
    val permissionState by viewModel.permissionState.collectAsStateWithLifecycle()

    val navHostController = LocalScreenNavigationController.current

    if (initConfigDialogState) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(text = stringResource(R.string.text_init_app))
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (permissionState) {
                        RuntimeModeViewModel.PermissionState.Request -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(55.dp),
                                    strokeWidth = 4.dp
                                )
                                Text(text = initConfigDialogMessage)
                            }
                        }
                        RuntimeModeViewModel.PermissionState.Success -> {
                            LaunchedEffect(Unit) {
                                viewModel.setInitConfigDialogState(false)
                                navHostController.navigate(ScreenRoute.MAIN) {
                                    popUpTo(ScreenRoute.RUNTIME_MODE) {
                                        inclusive = true
                                    }
                                }
                            }
                            Text(text = stringResource(R.string.text_init_app_success))
                        }
                        RuntimeModeViewModel.PermissionState.RetryCheckRoot -> {
                            Column {
                                var userId by remember { mutableStateOf("su") }

                                LaunchedEffect(Unit) {
                                    userId = storageStore.getString(Const.APP_SHELL_ROOT_USER) ?: "su"
                                }

                                Text(text = initConfigDialogMessage)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.text_change_root_user),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = String.format("%s: %s", stringResource(R.string.text_change_root_user_tip), userId),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Column {
                                        var expanded by remember { mutableStateOf(false) }

                                        TextButton(
                                            onClick = {
                                                expanded = true
                                            }
                                        ) {
                                            Text(text = stringResource(R.string.text_change_user))
                                        }

                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            Config.ROOT_USERS.forEach { id ->
                                                DropdownMenuItem(
                                                    leadingIcon = {
                                                        Checkbox(
                                                            checked = id == userId,
                                                            onCheckedChange = null
                                                        )
                                                    },
                                                    text = {
                                                        Text(text = id)
                                                    },
                                                    onClick = {
                                                        ioScope.launch {
                                                            userId = id
                                                            // 切换所有用户
                                                            ReusableShells.changeUserIdAtAll(id)
                                                            expanded = false
                                                        }
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row {
                   AnimatedContent(
                       targetState = permissionState
                   ) {
                       when (it) {
                           RuntimeModeViewModel.PermissionState.Request -> Unit
                           RuntimeModeViewModel.PermissionState.Success -> Unit
                           RuntimeModeViewModel.PermissionState.RetryCheckRoot -> {
                               Button(
                                   onClick = {
                                       viewModel.initRootModeConfig { throwable ->
                                           Toast.makeText(activity, throwable.makeText(), Toast.LENGTH_SHORT).show()
                                       }
                                   }
                               ) {
                                   Text(
                                       text = stringResource(R.string.text_retry)
                                   )
                               }
                           }
                       }
                   }
                }
            },
            dismissButton = {
                Row {
                    AnimatedContent(
                        targetState = permissionState
                    ) {
                        when (it) {
                            RuntimeModeViewModel.PermissionState.Request -> Unit
                            RuntimeModeViewModel.PermissionState.Success -> Unit
                            RuntimeModeViewModel.PermissionState.RetryCheckRoot -> {
                                FilledTonalButton(
                                    onClick = {
                                        activity.finish()

                                    }
                                ) {
                                    Text(text = stringResource(R.string.text_exit_app))
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}