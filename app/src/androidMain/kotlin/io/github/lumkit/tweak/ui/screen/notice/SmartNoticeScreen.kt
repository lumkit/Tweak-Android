package io.github.lumkit.tweak.ui.screen.notice

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import io.github.lumkit.tweak.common.util.ServiceUtils
import io.github.lumkit.tweak.common.util.getDiveSize
import io.github.lumkit.tweak.common.util.getStatusBarHeight
import io.github.lumkit.tweak.data.SmartNoticeProperties
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.services.SmartNoticeService
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.disableGameMode
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.enableGameMode
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.reloadProperties
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.showNotificationStatus
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.startSmartNotice
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.stopSmartNotice
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.updateCutoutGravity
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.updateCutoutHeight
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.updateCutoutRadius
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.updateCutoutWidth
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.updateMediaObserve
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.updateStart
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.updateTop
import io.github.lumkit.tweak.ui.component.DetailItem
import io.github.lumkit.tweak.ui.component.DevelopmentStage
import io.github.lumkit.tweak.ui.component.FolderItem
import io.github.lumkit.tweak.ui.component.GroupDetail
import io.github.lumkit.tweak.ui.component.RichTooltipBox
import io.github.lumkit.tweak.ui.component.ScreenScaffold
import io.github.lumkit.tweak.ui.component.SharedTransitionText
import io.github.lumkit.tweak.ui.local.LocalStorageStore
import io.github.lumkit.tweak.ui.local.StorageStore
import io.github.lumkit.tweak.ui.local.json
import io.github.lumkit.tweak.ui.screen.notice.SmartNoticeViewModel.Companion.gotoNotificationAccessSetting
import io.github.lumkit.tweak.ui.token.SmartNoticeCapsuleDefault
import io.github.lumkit.tweak.ui.view.SmartNoticeWindow
import io.github.lumkit.tweak.util.Aes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong


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
    val permissionsState by viewModel.permissionsState.collectAsStateWithLifecycle()
    val coroutine = rememberCoroutineScope()

    // 已全部授予权限
    var grantedAll by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(permissionsState) {
        grantedAll = permissionsState.all { it.grant }
    }

    GroupDetail(
        title = {
            Text(
                text = stringResource(R.string.text_preference)
            )
        }
    ) {

        // 开关
        run {
            var switch by rememberSaveable { mutableStateOf(storageStore.getBoolean(Const.SmartNotice.SMART_NOTICE_SWITCH)) }
            var enabled by rememberSaveable { mutableStateOf(true) }

            DetailItem(
                enabled = grantedAll && enabled,
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
                        if (switch) {
                            enabled = false
                            coroutine.launch {
                                activity.stopSmartNotice()
                                enabled = true
                            }
                            switch = false
                        } else {
                            activity.startSmartNotice()
                            switch = true
                        }
                        storageStore.putBoolean(Const.SmartNotice.SMART_NOTICE_SWITCH, switch)
                    }
                },
                actions = {
                    Switch(
                        checked = switch,
                        onCheckedChange = null,
                    )
                }
            )
        }

        // 状态通知
        run {
            var switch by rememberSaveable {
                mutableStateOf(
                    storageStore.getBoolean(
                        Const.SmartNotice.SMART_NOTICE_NOTIFICATION,
                        true
                    )
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
                    if (!storageStore.getBoolean(Const.SmartNotice.SMART_NOTICE_SWITCH)) {
                        Toast.makeText(
                            activity,
                            R.string.text_please_start_smart_notice,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        switch = !switch
                        activity.showNotificationStatus(switch)
                        storageStore.putBoolean(Const.SmartNotice.SMART_NOTICE_NOTIFICATION, switch)
                    }
                },
                actions = {
                    Switch(
                        checked = switch,
                        onCheckedChange = null,
                    )
                }
            )
        }

        // 游戏模式
        run {
            var switch by rememberSaveable {
                mutableStateOf(
                    storageStore.getBoolean(
                        Const.SmartNotice.SMART_NOTICE_GAME_MODE,
                        true
                    )
                )
            }

            DetailItem(
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
                    switch = !switch
                    storageStore.putBoolean(Const.SmartNotice.SMART_NOTICE_GAME_MODE, switch)
                    if (ServiceUtils.isServiceRunning(
                            activity,
                            SmartNoticeService::class.java.name
                        )
                    ) {
                        if (switch) {
                            activity.enableGameMode()
                        } else {
                            activity.disableGameMode()
                        }
                    }
                },
                actions = {
                    Switch(
                        checked = switch,
                        onCheckedChange = null,
                    )
                }
            )
        }

        // 游戏模式
        run {
            var switch by rememberSaveable {
                mutableStateOf(
                    storageStore.getBoolean(
                        Const.SmartNotice.SMART_NOTICE_ALWAYS_SHOW,
                        false
                    )
                )
            }

            DetailItem(
                title = {
                    Text(
                        text = stringResource(R.string.text_smart_notice_always_show)
                    )
                },
                subTitle = {
                    Text(
                        text = stringResource(R.string.text_smart_notice_always_show_tips)
                    )
                },
                onClick = {
                    switch = !switch
                    storageStore.putBoolean(Const.SmartNotice.SMART_NOTICE_ALWAYS_SHOW, switch)
                },
                actions = {
                    Switch(
                        checked = switch,
                        onCheckedChange = null,
                    )
                }
            )
        }

        // 位置调整
        run {
            var dialogState by rememberSaveable { mutableStateOf(false) }
            DetailItem(
                title = {
                    Text(
                        text = stringResource(R.string.text_smart_notice_set_position)
                    )
                },
                subTitle = {
                    Text(
                        text = stringResource(R.string.text_smart_notice_set_position_tips)
                    )
                },
                onClick = {
                    dialogState = true
                },
                actions = {
                    Icon(
                        painter = painterResource(R.drawable.ic_right),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )

            SetNoticeFieldDialog(dialogState, activity, storageStore) {
                dialogState = false
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    GroupDetail(
        title = {
            Text(
                text = stringResource(R.string.text_notice_observer)
            )
        }
    ) {
        // 充电
        run {
            var observed by rememberSaveable {
                mutableStateOf(
                    storageStore.getBoolean(
                        Const.SmartNotice.Observe.SMART_NOTICE_OBSERVE_CHARGE,
                        true
                    )
                )
            }

            var attrDialog by rememberSaveable { mutableStateOf(false) }

            DetailItem(
                onClick = {
                    attrDialog = true
                },
                title = {
                    Text(
                        text = stringResource(R.string.text_smart_notice_observe_charge)
                    )
                },
                subTitle = {
                    Text(
                        text = stringResource(R.string.text_smart_notice_observe_charge_tips)
                    )
                },
                actions = {
                    Switch(
                        checked = observed,
                        onCheckedChange = {
                            observed = it
                            storageStore.putBoolean(
                                Const.SmartNotice.Observe.SMART_NOTICE_OBSERVE_CHARGE,
                                it
                            )
                        }
                    )
                }
            )
        }

        // 音乐
        run {
            var observed by rememberSaveable {
                mutableStateOf(
                    storageStore.getBoolean(
                        Const.SmartNotice.Observe.SMART_NOTICE_OBSERVE_MUSIC,
                        true
                    )
                )
            }

            var attrDialog by rememberSaveable { mutableStateOf(false) }

            DetailItem(
                onClick = {
                    attrDialog = true
                },
                title = {
                    Text(
                        text = stringResource(R.string.text_smart_notice_observe_music)
                    )
                },
                subTitle = {
                    Text(
                        text = stringResource(R.string.text_smart_notice_observe_music_tips)
                    )
                },
                actions = {
                    Switch(
                        checked = observed,
                        onCheckedChange = {
                            observed = it
                            storageStore.putBoolean(
                                Const.SmartNotice.Observe.SMART_NOTICE_OBSERVE_MUSIC,
                                it
                            )
                            activity.updateMediaObserve(it)
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun SetNoticeFieldDialog(
    visible: Boolean,
    activity: ComponentActivity,
    storageStore: StorageStore,
    onDismissRequest: () -> Unit,
) {
    if (visible) {
        AlertDialog(
            modifier = Modifier.padding(vertical = 28.dp),
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = stringResource(R.string.text_smart_notice_set_position)
                )
            },
            text = {
                SetNoticeFieldBox(activity, storageStore, onDismissRequest)
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissRequest()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.text_confirm)
                    )
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = onDismissRequest
                ) {
                    Text(
                        text = stringResource(R.string.text_cancel)
                    )
                }
            }
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun SetNoticeFieldBox(
    activity: ComponentActivity,
    storageStore: StorageStore,
    onDismissRequest: () -> Unit
) {

    val deviceSize = remember { activity.getDiveSize() }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    with(density) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            //挖孔位置
            run {
                var gravity by rememberSaveable { mutableStateOf(SmartNoticeWindow.Companion.Gravity.Center) }

                LaunchedEffect(Unit) {
                    try {
                        gravity = SmartNoticeWindow.Companion.Gravity.entries[
                            storageStore.getInt(
                                Const.SmartNotice.SMART_NOTICE_CUTOUT_POSITION,
                                SmartNoticeWindow.Companion.Gravity.Center.ordinal
                            )
                        ]
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                FolderItem(
                    title = {
                        Text(
                            text = stringResource(R.string.text_cutout_position)
                        )
                    },
                    subtitle = {
                        Text(
                            text = stringResource(R.string.text_cutout_position_tips)
                        )
                    },
                    trailingIcon = {
                        Column {
                            var expanded by rememberSaveable { mutableStateOf(false) }
                            TextButton(
                                onClick = {
                                    expanded = true
                                }
                            ) {
                                Text(
                                    text = gravity.asString()
                                )
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                SmartNoticeWindow.Companion.Gravity.entries.forEach {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = it.asString()
                                            )
                                        },
                                        onClick = {
                                            gravity = it
                                            activity.updateCutoutGravity(it.gravity)
                                            storageStore.putInt(
                                                Const.SmartNotice.SMART_NOTICE_CUTOUT_POSITION,
                                                it.ordinal
                                            )
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // y轴
            run {
                var yPosition by rememberSaveable { mutableFloatStateOf(0f) }
                var valueEditDialogState by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    with(density) {
                        try {
                            val size = SmartNoticeWindow.islandCustomSize.filter { it != Size.Zero }
                                .first()
                            yPosition = storageStore.getFloat(
                                Const.SmartNotice.SMART_NOTICE_OFFSET_Y,
                                (getStatusBarHeight() - size.height.roundToInt() - SmartNoticeCapsuleDefault.CapsulePaddingTop.roundToPx() / 2f).toDp().value
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                ValueEditDialog(
                    visible = valueEditDialogState,
                    value = yPosition.toString()
                ) {
                    if (it.isBlank()) {
                        valueEditDialogState = false
                        return@ValueEditDialog
                    }
                    try {
                        val float = it.toFloat()
                        if (float !in 0f..deviceSize.height.toDp().value) {
                            Toast.makeText(
                                activity,
                                R.string.text_value_is_out_bounds,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            yPosition = float
                            valueEditDialogState = false
                            activity.updateTop(float)
                            storageStore.putFloat(
                                Const.SmartNotice.SMART_NOTICE_OFFSET_Y,
                                yPosition
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            activity,
                            String.format(
                                activity.getString(R.string.text_throws_error),
                                e.message
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                FolderItem(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.text_position_top),
                            )

                            Text(
                                text = String.format("%.2fdp", yPosition),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.clickable(
                                    indication = null,
                                    interactionSource = null
                                ) {
                                    valueEditDialogState = true
                                }
                            )
                        }
                    },
                    subtitle = {
                        Slider(
                            modifier = Modifier.fillMaxWidth(),
                            value = yPosition,
                            onValueChange = {
                                yPosition = it
                                activity.updateTop(it)
                                storageStore.putFloat(
                                    Const.SmartNotice.SMART_NOTICE_OFFSET_Y,
                                    it
                                )
                            },
                            onValueChangeFinished = {

                            },
                            valueRange = -deviceSize.height.toDp().value.roundToInt()
                                .toFloat()..deviceSize.height.toDp().value.roundToInt().toFloat(),
                        )
                    }
                )
            }

            // x轴
            run {
                var xPosition by rememberSaveable { mutableFloatStateOf(0f) }
                var valueEditDialogState by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    try {
                        xPosition = storageStore.getFloat(
                            Const.SmartNotice.SMART_NOTICE_OFFSET_X,
                            0f
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                ValueEditDialog(
                    visible = valueEditDialogState,
                    value = xPosition.toString()
                ) {
                    if (it.isBlank()) {
                        valueEditDialogState = false
                        return@ValueEditDialog
                    }
                    try {
                        val float = it.toFloat()
                        if (float !in -deviceSize.width.toDp().value..deviceSize.width.toDp().value) {
                            Toast.makeText(
                                activity,
                                R.string.text_value_is_out_bounds,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            xPosition = float
                            valueEditDialogState = false
                            activity.updateStart(float)
                            storageStore.putFloat(
                                Const.SmartNotice.SMART_NOTICE_OFFSET_X,
                                xPosition
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            activity,
                            String.format(
                                activity.getString(R.string.text_throws_error),
                                e.message
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                FolderItem(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.text_position_start),
                            )
                            Text(
                                text = String.format("%.2fdp", xPosition),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.clickable(
                                    indication = null,
                                    interactionSource = null
                                ) {
                                    valueEditDialogState = true
                                }
                            )
                        }
                    },
                    subtitle = {
                        Slider(
                            modifier = Modifier.fillMaxWidth(),
                            value = xPosition,
                            onValueChange = {
                                xPosition = it
                                activity.updateStart(it)
                                storageStore.putFloat(
                                    Const.SmartNotice.SMART_NOTICE_OFFSET_X,
                                    it
                                )
                            },
                            onValueChangeFinished = {

                            },
                            valueRange = -deviceSize.width.toDp().value.roundToInt()
                                .toFloat()..deviceSize.width.toDp().value.roundToInt().toFloat(),
                        )
                    }
                )
            }

            // 宽度
            run {
                var width by rememberSaveable { mutableFloatStateOf(0f) }
                var valueEditDialogState by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    with(density) {
                        try {
                            width = storageStore.getFloat(
                                Const.SmartNotice.SMART_NOTICE_WIDTH,
                                SmartNoticeWindow.islandCustomSize.value.width.toDp().value
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                ValueEditDialog(
                    visible = valueEditDialogState,
                    value = width.toString()
                ) {
                    if (it.isBlank()) {
                        valueEditDialogState = false
                        return@ValueEditDialog
                    }
                    try {
                        val float = it.toFloat()
                        if (float !in 0f..deviceSize.width.toDp().value) {
                            Toast.makeText(
                                activity,
                                R.string.text_value_is_out_bounds,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            width = float
                            valueEditDialogState = false
                            activity.updateCutoutWidth(float)
                            storageStore.putFloat(
                                Const.SmartNotice.SMART_NOTICE_WIDTH,
                                width
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            activity,
                            String.format(
                                activity.getString(R.string.text_throws_error),
                                e.message
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                FolderItem(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.text_smart_notice_width),
                            )
                            Text(
                                text = String.format("%.2fdp", width),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.clickable(
                                    indication = null,
                                    interactionSource = null
                                ) {
                                    valueEditDialogState = true
                                }
                            )
                        }
                    },
                    subtitle = {
                        Slider(
                            modifier = Modifier.fillMaxWidth(),
                            value = width,
                            onValueChange = {
                                width = it
                                activity.updateCutoutWidth(it)
                                storageStore.putFloat(
                                    Const.SmartNotice.SMART_NOTICE_WIDTH,
                                    it
                                )
                            },
                            onValueChangeFinished = {

                            },
                            valueRange = 0f..deviceSize.width.toDp().value.roundToInt().toFloat(),
                        )
                    }
                )
            }

            // 高度
            run {
                var height by rememberSaveable { mutableFloatStateOf(0f) }
                var valueEditDialogState by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    with(density) {
                        try {
                            height = storageStore.getFloat(
                                Const.SmartNotice.SMART_NOTICE_HEIGHT,
                                SmartNoticeWindow.islandCustomSize.value.height.toDp().value
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                ValueEditDialog(
                    visible = valueEditDialogState,
                    value = height.toString()
                ) {
                    if (it.isBlank()) {
                        valueEditDialogState = false
                        return@ValueEditDialog
                    }
                    try {
                        val float = it.toFloat()
                        if (float !in 0f..deviceSize.width.toDp().value) {
                            Toast.makeText(
                                activity,
                                R.string.text_value_is_out_bounds,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            height = float
                            valueEditDialogState = false
                            activity.updateCutoutHeight(float)
                            storageStore.putFloat(
                                Const.SmartNotice.SMART_NOTICE_HEIGHT,
                                height
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            activity,
                            String.format(
                                activity.getString(R.string.text_throws_error),
                                e.message
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                FolderItem(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.text_smart_notice_height),
                            )
                            Text(
                                text = String.format("%.2fdp", height),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.clickable(
                                    indication = null,
                                    interactionSource = null
                                ) {
                                    valueEditDialogState = true
                                }
                            )
                        }
                    },
                    subtitle = {
                        Slider(
                            modifier = Modifier.fillMaxWidth(),
                            value = height,
                            onValueChange = {
                                height = it
                                activity.updateCutoutHeight(it)
                                storageStore.putFloat(
                                    Const.SmartNotice.SMART_NOTICE_HEIGHT,
                                    it
                                )
                            },
                            onValueChangeFinished = {

                            },
                            valueRange = 0f..deviceSize.width.toDp().value.roundToInt().toFloat(),
                        )
                    }
                )
            }

            // 圆角
            run {
                var radius by rememberSaveable { mutableFloatStateOf(0f) }
                var valueEditDialogState by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    with(density) {
                        try {
                            radius = storageStore.getFloat(
                                Const.SmartNotice.SMART_NOTICE_CUTOUT_RADIUS,
                                SmartNoticeWindow.islandCustomSize.value.height.toDp().value / 2f
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                ValueEditDialog(
                    visible = valueEditDialogState,
                    value = radius.toString()
                ) {
                    if (it.isBlank()) {
                        valueEditDialogState = false
                        return@ValueEditDialog
                    }
                    try {
                        val float = it.toFloat()
                        if (float !in 0f..(deviceSize.width.toDp().value / 2f)) {
                            Toast.makeText(
                                activity,
                                R.string.text_value_is_out_bounds,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            radius = float
                            valueEditDialogState = false
                            activity.updateCutoutRadius(float)
                            storageStore.putFloat(
                                Const.SmartNotice.SMART_NOTICE_CUTOUT_RADIUS,
                                radius
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            activity,
                            String.format(
                                activity.getString(R.string.text_throws_error),
                                e.message
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                FolderItem(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.text_smart_notice_radius),
                            )
                            Text(
                                text = String.format("%.2fdp", radius),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.clickable(
                                    indication = null,
                                    interactionSource = null
                                ) {
                                    valueEditDialogState = true
                                }
                            )
                        }
                    },
                    subtitle = {
                        Slider(
                            modifier = Modifier.fillMaxWidth(),
                            value = radius,
                            onValueChange = {
                                radius = it
                                activity.updateCutoutRadius(it)
                                storageStore.putFloat(
                                    Const.SmartNotice.SMART_NOTICE_CUTOUT_RADIUS,
                                    it
                                )
                            },
                            onValueChangeFinished = {

                            },
                            valueRange = 0f..(deviceSize.width.toDp().value.roundToInt()
                                .toFloat() / 2f),
                        )
                    }
                )
            }

            // animate duration
            run {
                var duration by rememberSaveable { mutableLongStateOf(0L) }
                var valueEditDialogState by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    try {
                        duration = storageStore.getLong(
                            Const.SmartNotice.SMART_NOTICE_ANIMATION_DURATION,
                            550L
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                ValueEditDialog(
                    visible = valueEditDialogState,
                    unit = "ms",
                    value = duration.toString()
                ) {
                    if (it.isBlank()) {
                        valueEditDialogState = false
                        return@ValueEditDialog
                    }
                    try {
                        val long = it.toLong()
                        if (long !in 200L..5000L) {
                            Toast.makeText(
                                activity,
                                R.string.text_value_is_out_bounds,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            duration = long
                            valueEditDialogState = false

                            SmartNoticeWindow.animatorDuration.value = duration
                            storageStore.putLong(
                                Const.SmartNotice.SMART_NOTICE_ANIMATION_DURATION,
                                duration
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            activity,
                            String.format(
                                activity.getString(R.string.text_throws_error),
                                e.message
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                FolderItem(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.text_smart_notice_animation_duration),
                            )
                            Text(
                                text = String.format("%dms", duration),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.clickable(
                                    indication = null,
                                    interactionSource = null
                                ) {
                                    valueEditDialogState = true
                                }
                            )
                        }
                    },
                    subtitle = {
                        Slider(
                            modifier = Modifier.fillMaxWidth(),
                            value = duration.toFloat(),
                            onValueChange = {
                                duration = it.roundToLong()
                                SmartNoticeWindow.animatorDuration.value = duration
                                storageStore.putLong(
                                    Const.SmartNotice.SMART_NOTICE_ANIMATION_DURATION,
                                    duration
                                )
                            },
                            onValueChangeFinished = {

                            },
                            valueRange = 200f..5000f,
                        )
                    }
                )
            }

            // delay duration
            run {
                var duration by rememberSaveable { mutableLongStateOf(0L) }
                var valueEditDialogState by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    try {
                        duration = storageStore.getLong(
                            Const.SmartNotice.SMART_NOTICE_ANIMATION_DELAY,
                            5000L
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                ValueEditDialog(
                    visible = valueEditDialogState,
                    unit = "ms",
                    value = duration.toString()
                ) {
                    if (it.isBlank()) {
                        valueEditDialogState = false
                        return@ValueEditDialog
                    }
                    try {
                        val long = it.toLong()
                        if (long !in 500L..10_000L) {
                            Toast.makeText(
                                activity,
                                R.string.text_value_is_out_bounds,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            duration = long
                            valueEditDialogState = false

                            SmartNoticeWindow.animatorDelay.value = duration
                            storageStore.putLong(
                                Const.SmartNotice.SMART_NOTICE_ANIMATION_DELAY,
                                duration
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            activity,
                            String.format(
                                activity.getString(R.string.text_throws_error),
                                e.message
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                FolderItem(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.text_smart_notice_delay_duration),
                            )
                            Text(
                                text = String.format("%dms", duration),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.clickable(
                                    indication = null,
                                    interactionSource = null
                                ) {
                                    valueEditDialogState = true
                                }
                            )
                        }
                    },
                    subtitle = {
                        Slider(
                            modifier = Modifier.fillMaxWidth(),
                            value = duration.toFloat(),
                            onValueChange = {
                                duration = it.roundToLong()
                                SmartNoticeWindow.animatorDelay.value = duration
                                storageStore.putLong(
                                    Const.SmartNotice.SMART_NOTICE_ANIMATION_DELAY,
                                    duration
                                )
                            },
                            onValueChangeFinished = {

                            },
                            valueRange = 500f..10_000f,
                        )
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 导入配置
                run {
                    var dialogState by rememberSaveable { mutableStateOf(false) }

                    TextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        onClick = {
                            dialogState = true
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.text_export_configs)
                        )
                    }

                    ExportDialog(dialogState, activity, storageStore) { dialogState = false }
                }

                run {
                    var dialogState by rememberSaveable { mutableStateOf(false) }
                    TextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        onClick = {
                            dialogState = true
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.text_import_configs)
                        )
                    }

                    ImportDialog(
                        dialogState,
                        activity,
                        storageStore,
                        onDismissRequest,
                    ) { dialogState = false }
                }
            }

            run {
                var resetDialogState by rememberSaveable { mutableStateOf(false) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        resetDialogState = true
                    },
                    colors = ButtonDefaults.buttonColors()
                        .copy(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                ) {
                    Text(
                        text = stringResource(R.string.text_smart_notice_reset_properties)
                    )
                }

                if (resetDialogState) {
                    AlertDialog(
                        onDismissRequest = { resetDialogState = false },
                        title = {
                            Text(
                                text = stringResource(R.string.text_alert)
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.text_smart_notice_reset_properties_msg)
                            )
                        },
                        confirmButton = {
                            var enabled by rememberSaveable { mutableStateOf(true) }
                            Button(
                                onClick = {
                                    enabled = false
                                    coroutineScope.launch {
                                        storageStore.putInt(
                                            Const.SmartNotice.SMART_NOTICE_CUTOUT_POSITION,
                                            SmartNoticeWindow.Companion.Gravity.Center.ordinal
                                        )
                                        storageStore.putFloat(
                                            Const.SmartNotice.SMART_NOTICE_OFFSET_X,
                                            0f
                                        )
                                        storageStore.putFloat(
                                            Const.SmartNotice.SMART_NOTICE_OFFSET_Y,
                                            (getStatusBarHeight() - SmartNoticeCapsuleDefault.CapsuleHeight.roundToPx() - SmartNoticeCapsuleDefault.CapsulePaddingTop.roundToPx() / 2).toDp().value
                                        )
                                        storageStore.putFloat(
                                            Const.SmartNotice.SMART_NOTICE_WIDTH,
                                            SmartNoticeCapsuleDefault.CapsuleWidth.value
                                        )
                                        storageStore.putFloat(
                                            Const.SmartNotice.SMART_NOTICE_HEIGHT,
                                            SmartNoticeCapsuleDefault.CapsuleHeight.value
                                        )
                                        storageStore.putFloat(
                                            Const.SmartNotice.SMART_NOTICE_CUTOUT_RADIUS,
                                            SmartNoticeCapsuleDefault.CapsuleHeight.value / 2f
                                        )
                                        storageStore.putLong(
                                            Const.SmartNotice.SMART_NOTICE_ANIMATION_DURATION,
                                            550L
                                        )
                                        storageStore.putLong(
                                            Const.SmartNotice.SMART_NOTICE_ANIMATION_DELAY,
                                            5000L
                                        )
                                        activity.reloadProperties()
                                        enabled = true
                                        resetDialogState = false
                                        onDismissRequest()
                                    }
                                },
                                enabled = enabled
                            ) {
                                Text(
                                    text = stringResource(R.string.text_confirm)
                                )
                            }
                        },
                        dismissButton = {
                            FilledTonalButton(
                                onClick = {
                                    resetDialogState = false
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
        }
    }
}

@Composable
private fun ExportDialog(
    visible: Boolean,
    activity: ComponentActivity,
    storageStore: StorageStore,
    onDismissRequest: () -> Unit
) {
    if (visible) {
        val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
        var label by rememberSaveable { mutableStateOf("") }
        val density = LocalDensity.current
        val clipboard = LocalClipboard.current

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = stringResource(R.string.text_export_configs)
                )
            },
            text = {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = label,
                    onValueChange = {
                        label = it
                    },
                    singleLine = true,
                    label = {
                        Text(
                            text = stringResource(R.string.text_input_label)
                        )
                    }
                )
            },
            confirmButton = {
                var enabled by rememberSaveable { mutableStateOf(true) }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            enabled = false
                            val size = SmartNoticeWindow.islandCustomSize.filter { it != Size.Zero }
                                .first()
                            val properties = with(density) {
                                SmartNoticeProperties(
                                    label = label,
                                    gravity = storageStore.getInt(
                                        Const.SmartNotice.SMART_NOTICE_CUTOUT_POSITION,
                                        SmartNoticeWindow.Companion.Gravity.Center.ordinal
                                    ),
                                    x = storageStore.getFloat(
                                        Const.SmartNotice.SMART_NOTICE_OFFSET_X,
                                        0f
                                    ),
                                    y = storageStore.getFloat(
                                        Const.SmartNotice.SMART_NOTICE_OFFSET_Y,
                                        (getStatusBarHeight() - size.height.roundToInt() - SmartNoticeCapsuleDefault.CapsulePaddingTop.roundToPx() / 2f).toDp().value
                                    ),
                                    width = size.width.toDp().value,
                                    height = size.height.toDp().value,
                                    radius = storageStore.getFloat(
                                        Const.SmartNotice.SMART_NOTICE_CUTOUT_RADIUS,
                                        (min(size.width, size.height) / 2f).toDp().value
                                    ),
                                    duration = storageStore.getLong(
                                        Const.SmartNotice.SMART_NOTICE_ANIMATION_DURATION,
                                        550L
                                    ),
                                    delay = storageStore.getLong(
                                        Const.SmartNotice.SMART_NOTICE_ANIMATION_DELAY,
                                        5000L
                                    )
                                )
                            }

                            val jsonText = json.encodeToString(properties)
                            val encrypt = Aes.encrypt(TweakApplication.application, jsonText)

                            withContext(Dispatchers.Main) {
                                clipboard.setClipEntry(
                                    ClipEntry(
                                        clipData = ClipData.newPlainText(
                                            label,
                                            buildString {
                                                append("tweak-smart-notice://")
                                                append(encrypt)
                                            }
                                        )
                                    )
                                )

                                Toast.makeText(
                                    activity,
                                    R.string.text_configs_exported,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            enabled = true
                            onDismissRequest()
                        }
                    },
                    enabled = enabled
                ) {
                    Text(
                        text = stringResource(R.string.text_copy_to_cut)
                    )
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = onDismissRequest
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
private fun ImportDialog(
    visible: Boolean,
    activity: ComponentActivity,
    storageStore: StorageStore,
    onSuccess: () -> Unit,
    onDismissRequest: () -> Unit
) {
    if (visible) {
        val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
        var text by rememberSaveable { mutableStateOf("") }
        var dialogState by rememberSaveable { mutableStateOf(false) }
        var propertiesImport by rememberSaveable { mutableStateOf<SmartNoticeProperties?>(null) }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = stringResource(R.string.text_import_configs)
                )
            },
            text = {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = text,
                    onValueChange = {
                        text = it
                    },
                    singleLine = true,
                    label = {
                        Text(
                            text = stringResource(R.string.text_input_properties)
                        )
                    }
                )
            },
            confirmButton = {
                var enabled by rememberSaveable { mutableStateOf(true) }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            enabled = false
                            try {
                                propertiesImport = null
                                val decrypt = Aes.decrypt(
                                    TweakApplication.application,
                                    if (text.startsWith("tweak-smart-notice://")) {
                                        text.substring("tweak-smart-notice://".length).also {
                                            println(
                                                it
                                            )
                                        }
                                    } else {
                                        text
                                    }
                                ).toString()
                                val properties =
                                    json.decodeFromString<SmartNoticeProperties>(decrypt)
                                propertiesImport = properties
                                dialogState = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        activity,
                                        R.string.text_import_configs_fail,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } finally {
                                enabled = true
                            }
                        }
                    },
                    enabled = enabled
                ) {
                    Text(
                        text = stringResource(R.string.text_import)
                    )
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = onDismissRequest
                ) {
                    Text(
                        text = stringResource(R.string.text_cancel)
                    )
                }
            }
        )

        if (dialogState) {
            AlertDialog(
                onDismissRequest = {
                    dialogState = false
                },
                title = {
                    Text(
                        text = stringResource(R.string.text_alert)
                    )
                },
                text = {
                    Text(
                        text = String.format(
                            stringResource(R.string.text_import_configs_ask),
                            propertiesImport?.label ?: stringResource(R.string.text_unknown)
                        )
                    )
                },
                confirmButton = {
                    var enabled by rememberSaveable { mutableStateOf(true) }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                enabled = false
                                try {
                                    if (propertiesImport == null) {
                                        throw NullPointerException(activity.getString(R.string.text_properties_null))
                                    }

                                    propertiesImport?.apply {
                                        storageStore.putInt(
                                            Const.SmartNotice.SMART_NOTICE_CUTOUT_POSITION,
                                            gravity
                                        )
                                        storageStore.putFloat(
                                            Const.SmartNotice.SMART_NOTICE_OFFSET_X,
                                            x
                                        )
                                        storageStore.putFloat(
                                            Const.SmartNotice.SMART_NOTICE_OFFSET_Y,
                                            y
                                        )
                                        storageStore.putFloat(
                                            Const.SmartNotice.SMART_NOTICE_WIDTH,
                                            width
                                        )
                                        storageStore.putFloat(
                                            Const.SmartNotice.SMART_NOTICE_HEIGHT,
                                            height
                                        )
                                        storageStore.putFloat(
                                            Const.SmartNotice.SMART_NOTICE_CUTOUT_RADIUS,
                                            radius
                                        )
                                        storageStore.putLong(
                                            Const.SmartNotice.SMART_NOTICE_ANIMATION_DURATION,
                                            duration
                                        )
                                        storageStore.putLong(
                                            Const.SmartNotice.SMART_NOTICE_ANIMATION_DELAY,
                                            delay
                                        )
                                    }

                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            activity,
                                            R.string.text_import_configs_success,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        activity.reloadProperties()
                                        onSuccess()
                                        dialogState = false
                                        onDismissRequest()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            activity,
                                            e.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } finally {
                                    enabled = true
                                }
                            }
                        },
                        enabled = enabled
                    ) {
                        Text(
                            text = stringResource(R.string.text_confirm)
                        )
                    }
                },
                dismissButton = {
                    FilledTonalButton(
                        onClick = {
                            dialogState = false
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
}


@Composable
private fun ValueEditDialog(
    visible: Boolean,
    value: String,
    unit: String = "dp",
    onDismissRequest: (String) -> Unit,
) {
    if (visible) {
        var text by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { onDismissRequest("") },
            title = {
                Text(
                    text = stringResource(R.string.text_edit_value)
                )
            },
            text = {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = text,
                    onValueChange = {
                        text = it
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.text_please_input_value)
                        )
                    },
                    placeholder = {
                        Text(
                            text = buildString {
                                append(value)
                                append(unit)
                            }
                        )
                    },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissRequest(text)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.text_confirm)
                    )
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = {
                        onDismissRequest("")
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