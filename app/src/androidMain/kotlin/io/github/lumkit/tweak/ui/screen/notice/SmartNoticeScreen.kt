package io.github.lumkit.tweak.ui.screen.notice

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lumkit.tweak.LocalSharedTransitionScope
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.shell.provide.ReusableShells
import io.github.lumkit.tweak.data.SmartNoticeRunningState
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.services.TweakAccessibilityService
import io.github.lumkit.tweak.ui.component.DetailItem
import io.github.lumkit.tweak.ui.component.DevelopmentStage
import io.github.lumkit.tweak.ui.component.GroupDetail
import io.github.lumkit.tweak.ui.component.RichTooltipBox
import io.github.lumkit.tweak.ui.component.ScreenScaffold
import io.github.lumkit.tweak.ui.component.SharedTransitionText
import io.github.lumkit.tweak.ui.local.LocalStorageStore
import io.github.lumkit.tweak.ui.local.StorageStore
import io.github.lumkit.tweak.ui.screen.notice.SmartNoticeViewModel.Companion.gotoNotificationAccessSetting
import io.github.lumkit.tweak.ui.screen.notice.model.SmartNoticeNotificationPlugin
import io.github.lumkit.tweak.ui.view.SmartNoticeFactory
import io.github.lumkit.tweak.ui.view.SmartNoticeFactory.Companion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SmartNoticeScreen(
    activity: ComponentActivity = LocalActivity.current as ComponentActivity,
    viewModel: SmartNoticeViewModel = viewModel {
        SmartNoticeViewModel(
            activity = activity,
        )
    },
    storageStore: StorageStore = LocalStorageStore.current,
) {

    var show by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            if (!storageStore.getBoolean(Const.APP_AUTO_START_SERVICE) && !show) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        activity,
                        R.string.text_please_open_app_auto_start,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
                show = true
            }
        }
    }

    ScreenScaffold(
        title = {
            SharedTransitionText(
                text = stringResource(R.string.text_smart_notice),
            )
        },
        stage = DevelopmentStage.Testing
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .animateContentSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier)
            PermissionsRow(viewModel, storageStore)
            Spacer(modifier = Modifier)
            ContentList(viewModel, activity, storageStore)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PermissionsRow(viewModel: SmartNoticeViewModel, storageStore: StorageStore) {

    val sharedTransitionScope = LocalSharedTransitionScope.current

    val permissionsState by viewModel.permissionsState.collectAsStateWithLifecycle()

    // 已全部授予权限
    var grantedAll by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(permissionsState) {
        grantedAll = permissionsState.all { it.grant }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    val observer = remember(lifecycleOwner) {
        LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> viewModel.checkPermissions()
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

    AnimatedContent(
        grantedAll,
        transitionSpec = {
            fadeIn(tween(700)).togetherWith(fadeOut(tween(700)))
        }
    ) {
        if (it) {
            PermissionGrantAll(sharedTransitionScope, this)
        } else {
            PermissionsList(
                sharedTransitionScope,
                this,
                viewModel,
                permissionsState,
                storageStore,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PermissionGrantAll(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope
) {
    with(sharedTransitionScope) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState("permission-box"),
                    animatedVisibilityScope = animatedContentScope,
                ),
            onClick = {

            },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.text_smart_notice_granted_all_permissions),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState("permission-text"),
                        animatedVisibilityScope = animatedContentScope,
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PermissionsList(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    viewModel: SmartNoticeViewModel,
    permissionsState: List<SmartNoticeViewModel.PermissionState>,
    storageStore: StorageStore
) {
    with(sharedTransitionScope) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState("permission-box"),
                    animatedVisibilityScope = animatedContentScope,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.text_need_permissions),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState("permission-text"),
                        animatedVisibilityScope = animatedContentScope,
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.size(8.dp))
                for (permissionState in permissionsState) {
                    PermissionItem(
                        permissionState,
                        viewModel,
                        storageStore
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionItem(
    permissionState: SmartNoticeViewModel.PermissionState,
    viewModel: SmartNoticeViewModel,
    storageStore: StorageStore
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.requestPermission(permission = permissionState.permission, it)
    }

    val alertLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.requestPermission(
            permission = permissionState.permission,
            it.resultCode == Activity.RESULT_OK
        )
    }

    var enabled by rememberSaveable { mutableStateOf(true) }

    RichTooltipBox(
        tooltip = {
            Text(text = permissionState.describe)
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxHeight()
                .width(150.dp),
            enabled = enabled,
            onClick = {
                when (permissionState.permission) {
                    Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE -> {
                        context.gotoNotificationAccessSetting()
                    }

                    Manifest.permission.SYSTEM_ALERT_WINDOW -> {
                        alertLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri()
                            )
                        )
                    }

                    Manifest.permission.BIND_ACCESSIBILITY_SERVICE -> {
                        enabled = false
                        coroutineScope.launch {
                            try {
                                ReusableShells.execSync("settings put secure enabled_accessibility_services ${context.packageName}/.services.TweakAccessibilityService")
                                ReusableShells.execSync("settings put secure accessibility_enabled 1")
                                storageStore.putBoolean(
                                    Const.APP_ENABLED_ACCESSIBILITY_SERVICE,
                                    SmartNoticeViewModel.checkAccessibilityService()
                                )
                                viewModel.checkPermissions()
                            } finally {
                                enabled = true
                            }
                        }
                    }

                    else -> {
                        launcher.launch(permissionState.permission)
                    }
                }
            },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Card(
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Icon(
                            painter = painterResource(permissionState.icon),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(16.dp)
                        )
                    }

                    Checkbox(checked = permissionState.grant, onCheckedChange = null)
                }
                Text(
                    text = permissionState.name,
                    style = MaterialTheme.typography.titleLarge,
                )

                Text(
                    text = permissionState.describe,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun ContentList(
    viewModel: SmartNoticeViewModel,
    activity: ComponentActivity,
    storageStore: StorageStore
) {
    val coroutine = rememberCoroutineScope()

    GroupDetail(
        title = {
            Text(
                text = stringResource(R.string.text_preference)
            )
        }
    ) {
        SmartNoticeRunningSwitch(activity, storageStore, viewModel, coroutine)
        SmartNoticeStatusNotificationSwitch(activity, storageStore)
        SmartNoticeDisplayModeSwitch(activity, storageStore)
        SmartNoticeGameModeSwitch(activity, storageStore, coroutine)
        SmartNoticePropertiesEditor(activity)
        Spacer(modifier = Modifier.height(16.dp))
    }

    GroupDetail(
        title = {
            Text(
                text = stringResource(R.string.text_notice_observer)
            )
        }
    ) {
        ObservePlugins()
    }
}

@Composable
fun SmartNoticeRunningSwitch(
    activity: ComponentActivity,
    storageStore: StorageStore,
    viewModel: SmartNoticeViewModel,
    coroutine: CoroutineScope
) {
    val permissionsState by viewModel.permissionsState.collectAsStateWithLifecycle()

    // 已全部授予权限
    var grantedAll by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(permissionsState) {
        grantedAll = permissionsState.all { it.grant }
    }

    var switch by rememberSaveable { mutableStateOf(storageStore.getBoolean(Const.SmartNotice.SMART_NOTICE_SWITCH)) }
    var done by rememberSaveable { mutableStateOf(true) }

    DetailItem(
        enabled = grantedAll && done,
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice)
            )
        },
        onClick = {
            if (!grantedAll) {
                Toast.makeText(
                    activity,
                    R.string.text_please_grant_all_permissions,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                coroutine.launch {
                    done = false
                    switch = !switch
                    if (switch) {
                        SmartNoticeFactory.show(activity)
                    } else {
                        SmartNoticeFactory.hide(activity)
                    }
                    storageStore.putBoolean(Const.SmartNotice.SMART_NOTICE_SWITCH, switch)
                    delay(SmartNoticeFactory.globalSmartNoticeData.value.duration)
                    done = true
                }
            }
        },
        actions = {
            Switch(
                checked = switch,
                onCheckedChange = null,
                enabled = done
            )
        }
    )
}

@Composable
private fun SmartNoticeStatusNotificationSwitch(
    activity: ComponentActivity,
    storageStore: StorageStore
) {
    var showNotification by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showNotification = storageStore.getBoolean(
            Const.SmartNotice.SMART_NOTICE_NOTIFICATION,
            true
        )
    }

    DetailItem(
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice_notification_show)
            )
        },
        subTitle = {
            Text(
                text = stringResource(R.string.text_smart_notice_notification_show_tips)
            )
        },
        onClick = {
            showNotification = !showNotification
            storageStore.putBoolean(
                Const.SmartNotice.SMART_NOTICE_NOTIFICATION,
                showNotification
            )
            if (showNotification) {
                SmartNoticeFactory.showStatusNotification(activity)
            } else {
                SmartNoticeFactory.hideStatusNotification(activity)
            }
        },
        actions = {
            Switch(
                checked = showNotification,
                onCheckedChange = null,
            )
        }
    )
}

@Composable
private fun ColumnScope.SmartNoticeDisplayModeSwitch(
    activity: ComponentActivity,
    storageStore: StorageStore,
) {
    val runningState = SmartNoticeFactory.runningState
    var alwaysShow by rememberSaveable { mutableStateOf(false) }
    var clockShow by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        alwaysShow = storageStore.getBoolean(
            Const.SmartNotice.SMART_NOTICE_ALWAYS_SHOW,
            true
        )

        clockShow = storageStore.getBoolean(
            Const.SmartNotice.SMART_NOTICE_SCREEN_LOCKED_ALWAYS_SHOW,
            false
        )
    }

    DetailItem(
        enabled = runningState == SmartNoticeRunningState.ONLINE,
        onClick = {
            alwaysShow = !alwaysShow
            storageStore.putBoolean(
                Const.SmartNotice.SMART_NOTICE_ALWAYS_SHOW,
                alwaysShow
            )
            SmartNoticeFactory.updateDisplayMode(activity, alwaysShow)
        },
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice_display_mode)
            )
        },
        subTitle = {
            Text(
                text = stringResource(R.string.text_smart_notice_display_mode_tips)
            )
        }
    ) {
        Switch(
            checked = alwaysShow,
            onCheckedChange = null,
            enabled = runningState == SmartNoticeRunningState.ONLINE
        )
    }

    AnimatedVisibility(
        visible = alwaysShow
    ) {
        DetailItem(
            enabled = runningState == SmartNoticeRunningState.ONLINE && alwaysShow,
            onClick = {
                clockShow = !clockShow
                storageStore.putBoolean(
                    Const.SmartNotice.SMART_NOTICE_SCREEN_LOCKED_ALWAYS_SHOW,
                    clockShow
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.text_smart_notice_always_show)
                )
            },
            subTitle = {
                Text(
                    text = stringResource(R.string.text_smart_notice_always_show_tips)
                )
            }
        ) {
            Switch(
                checked = clockShow,
                onCheckedChange = null,
                enabled = runningState == SmartNoticeRunningState.ONLINE && alwaysShow
            )
        }
    }
}

@Composable
private fun SmartNoticeGameModeSwitch(
    activity: ComponentActivity,
    storageStore: StorageStore,
    coroutine: CoroutineScope
) {
    var gameModeState by rememberSaveable { mutableStateOf(false) }
    var done by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        gameModeState = storageStore.getBoolean(
            Const.SmartNotice.SMART_NOTICE_GAME_MODE,
            true
        )
    }

    DetailItem(
        enabled = done,
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice_game_mode)
            )
        },
        subTitle = {
            Text(
                text = stringResource(R.string.text_smart_notice_game_mode_tips)
            )
        },
        onClick = {
            coroutine.launch {
                done = false
                gameModeState = !gameModeState
                storageStore.putBoolean(
                    Const.SmartNotice.SMART_NOTICE_GAME_MODE,
                    gameModeState
                )
                if (gameModeState) {
                    SmartNoticeFactory.enableGameMode(activity)
                } else {
                    SmartNoticeFactory.disableGameMode(activity)
                }
                SmartNoticeFactory.gameModeState = activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && gameModeState
                delay(SmartNoticeFactory.globalSmartNoticeData.value.duration)
                done = true
            }
        },
        actions = {
            Switch(
                checked = gameModeState,
                onCheckedChange = null,
                enabled = done
            )
        }
    )
}

@Composable
private fun SmartNoticePropertiesEditor(
    activity: ComponentActivity,
) {
    var editorDialogState by rememberSaveable { mutableStateOf(false) }

    DetailItem(
        enabled = TweakAccessibilityService.isRunning,
        onClick = {
            editorDialogState = true
        },
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice_diy_properties)
            )
        },
        subTitle = {
            Text(
                text = stringResource(R.string.text_smart_notice_set_position_tips)
            )
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }

    SmartNoticePropertiesEditorDialog(visible = editorDialogState, activity) { editorDialogState = false }
}

@Composable
private fun ObservePlugins() {
    val plugins by SmartNoticeFactory.observerPluginMap.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = plugins,
    ) {
        if (it.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = {

                },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.text_smart_notice_must_start_accessibility_service),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            Plugins(plugins)
        }
    }
}

@Composable
private fun Plugins(plugins: Map<Any, SmartNoticeNotificationPlugin>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        plugins.forEach { (_, plugin) ->
            plugin.PreferenceContent(plugin)
        }
    }
}