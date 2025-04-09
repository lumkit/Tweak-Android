package io.github.lumkit.tweak.ui.screen.notice.model

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.util.AppInfoLoader
import io.github.lumkit.tweak.model.AppInfo
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.component.DetailItem
import io.github.lumkit.tweak.ui.component.FolderItem
import io.github.lumkit.tweak.ui.local.LocalStorageStore
import io.github.lumkit.tweak.ui.local.json
import io.github.lumkit.tweak.ui.view.SmartNoticeFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlin.math.min

class AppNotificationPlugin(
    override val factory: SmartNoticeFactory
) : SmartNoticeNotificationPlugin(
    factory = factory,
    sharedKey = Const.SmartNotice.Observe.SMART_NOTICE_OBSERVE_NOTIFICATION
) {

    companion object {
        val ACTION_APP_NOTIFICATION =
            "${AppNotificationPlugin::class.simpleName}.ACTION_APP_NOTIFICATION"
    }

    @Composable
    override fun PreferenceContent(plugin: SmartNoticeNotificationPlugin) {
        AppNotificationObserver(plugin as AppNotificationPlugin)
    }

    val sbnState = MutableStateFlow<StatusBarNotification?>(null)

    private val notificationBroad = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_APP_NOTIFICATION) {
                val sbn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("sbn", StatusBarNotification::class.java)
                } else {
                    intent.getParcelableExtra("sbn")
                }
                sbnState.value = sbn
                toast()
            }
        }
    }

    private val appInfoLoader = AppInfoLoader(factory.context, 100)

    private fun toast() {
        factory.toast(
            view = minimizeNotificationComposeView,
            contentSize = { _, _, _, _ ->
                DpSize(150.dp, 32.dp)
            },
            radius = { _, density, _, _ ->
                with(density) {
                    20.dp.toPx()
                }
            }
        )
    }

    private fun expanded() {
        factory.toast(
            view = expandedNotificationComposeView,
            contentSize = { _, density, size, _ ->
                with(density) {
                    DpSize(
                        min(size.width, size.height).toDp() - 28.dp * 2f,
                        100.dp
                    )
                }
            }
        )
    }

    private val minimizeNotificationComposeView = ComposeView(factory.context).apply {
        setContent {
            var logo by remember { mutableStateOf<Drawable?>(null) }
            var avatar by remember { mutableStateOf<Drawable?>(null) }
            val sbn by sbnState.collectAsStateWithLifecycle()

            LaunchedEffect(sbn) {
                avatar =
                    sbn?.notification?.extras?.getParcelable<Icon?>(Notification.EXTRA_LARGE_ICON)
                        ?.loadDrawable(factory.context)
                logo =
                    appInfoLoader.loadIcon(sbn?.packageName ?: factory.context.packageName).await()
            }

            Surface(
                onClick = {
                    expanded()
                },
                color = Color.Black,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedCard(
                        modifier = Modifier.size(24.dp),
                        border = BorderStroke(.5.dp, Color.White)
                    ) {
                        AsyncImage(
                            model = logo,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    OutlinedCard(
                        modifier = Modifier.size(24.dp),
                        border = BorderStroke(.5.dp, Color.White)
                    ) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    private val expandedNotificationComposeView = ComposeView(factory.context).apply {
        setContent {
            var logo by remember { mutableStateOf<Drawable?>(null) }
            var avatar by remember { mutableStateOf<Drawable?>(null) }
            val sbn by sbnState.collectAsStateWithLifecycle()
            var title by remember { mutableStateOf("") }
            var content by remember { mutableStateOf("") }

            LaunchedEffect(sbn) {
                val extras = sbn?.notification?.extras

                avatar = extras?.getParcelable<Icon?>(Notification.EXTRA_LARGE_ICON)
                    ?.loadDrawable(factory.context)
                title = extras?.getString(Notification.EXTRA_TITLE) ?: ""
                content = extras?.getString(Notification.EXTRA_TEXT) ?: ""
                logo =
                    appInfoLoader.loadIcon(sbn?.packageName ?: factory.context.packageName).await()
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        factory.minimize()
                        sbn?.packageName?.let(factory.context.packageManager::getLaunchIntentForPackage)
                            ?.apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }?.let(factory.context::startActivity)
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = { },
                            onVerticalDrag = { _, dragAmount ->
                                if (dragAmount < -50) {
                                    factory.minimize()
                                }
                            }
                        )
                    },
                color = Color.Black,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        OutlinedCard(
                            modifier = Modifier.size(36.dp),
                            border = BorderStroke(.5.dp, Color.White.copy(alpha = .5f))
                        ) {
                            AsyncImage(
                                model = logo,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                    ) {
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = .8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedCard(
                        modifier = Modifier.size(60.dp),
                        border = BorderStroke(.5.dp, Color.White.copy(alpha = .5f)),
                        shape = CircleShape
                    ) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    init {
        loadEnabled()
    }

    private var isRegister = false
    private fun register() {
        if (isRegister) {
            return
        }
        isRegister = true

        ContextCompat.registerReceiver(
            factory.context,
            notificationBroad,
            IntentFilter().apply {
                addAction(ACTION_APP_NOTIFICATION)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun unregister() {
        if (!isRegister) {
            return
        }
        isRegister = false

        factory.context.unregisterReceiver(notificationBroad)
    }

    override fun onEnableChanged(enabled: Boolean) {
        super.onEnableChanged(enabled)
        if (enabled) {
            register()
        } else {
            unregister()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregister()
    }
}

@Composable
private fun AppNotificationObserver(plugin: AppNotificationPlugin) {
    var dialogState by rememberSaveable { mutableStateOf(false) }

    DetailItem(
        onClick = {
            dialogState = true
        },
        title = {
            Text(
                text = stringResource(R.string.text_smart_notice_observe_notification)
            )
        },
        subTitle = {
            Text(
                text = stringResource(R.string.text_smart_notice_observe_notification_tips)
            )
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }

    NotificationObserverDialog(dialogState, plugin) { dialogState = false }
}

@Composable
private fun NotificationObserverDialog(
    visible: Boolean,
    plugin: AppNotificationPlugin,
    onDismissRequest: () -> Unit,
) {
    if (visible) {
        AlertDialog(
            modifier = Modifier.padding(vertical = 28.dp),
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = stringResource(R.string.text_smart_notice_observe_notification)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    SwitchRow(plugin)
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismissRequest
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
                        text = stringResource(R.string.text_cross)
                    )
                }
            }
        )
    }
}

@Composable
private fun SwitchRow(plugin: AppNotificationPlugin) {
    val enabled by plugin.enableState.collectAsStateWithLifecycle()
    var appManagerDialogState by rememberSaveable { mutableStateOf(false) }

    FolderItem(
        title = {
            Text(
                text = stringResource(R.string.text_notice_observe_plugin)
            )
        }
    ) {
        Switch(
            checked = enabled,
            onCheckedChange = {
                plugin.setEnabled(it)
                plugin.saveEnabled(plugin.sharedKey)
            }
        )
    }
    FolderItem(
        title = {
            Text(
                text = stringResource(R.string.text_apps_manager)
            )
        },
        onClick = {
            appManagerDialogState = true
        },
        padding = PaddingValues(
            vertical = 16.dp
        )
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }

    NotificationAppManagerDialog(visible = appManagerDialogState) { appManagerDialogState = false }
}

@Composable
private fun NotificationAppManagerDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit
) {
    if (visible) {
        val storageStore = LocalStorageStore.current

        val ioScope = rememberCoroutineScope { Dispatchers.IO }
        var filter by rememberSaveable { mutableStateOf(listOf<String>()) }
        var popup by rememberSaveable { mutableStateOf(false) }
        var apps by remember { mutableStateOf(listOf<AppInfo>()) }
        var searchText by rememberSaveable { mutableStateOf("") }

        LaunchedEffect(Unit) {
            val filterText = TweakApplication.shared.getString(
                Const.SmartNotice.SMART_NOTICE_NOTIFICATION_FILTER,
                null
            ) ?: Const.SmartNotice.NOTIFICATION_FILTER_DEFAULT
            filter = json.decodeFromString<List<String>>(filterText)
        }

        LaunchedEffect(TweakApplication.userApps, filter, searchText) {
            apps = TweakApplication.userApps.sortedWith(
                comparator = compareBy<AppInfo> {
                    !filter.contains(it.packageName)
                }.thenBy {
                    it.appName
                }
            ).filter {
                it.packageName != TweakApplication.application.packageName
            }.filter {
                it.appName.lowercase().contains(searchText.lowercase().trim()) ||
                        it.packageName.lowercase().contains(searchText.lowercase().trim())
            }
        }

        AlertDialog(
            modifier = Modifier.padding(vertical = 28.dp),
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = stringResource(R.string.text_apps_manager)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                text = stringResource(R.string.text_search_app)
                            )
                        },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(apps) { info ->
                            ListItem(
                                modifier = Modifier.fillMaxWidth(),
                                leadingContent = {
                                    OutlinedCard {
                                        AsyncImage(
                                            modifier = Modifier.size(40.dp),
                                            model = info.icon,
                                            contentDescription = null,
                                            error = painterResource(R.mipmap.ic_tweak_logo)
                                        )
                                    }
                                },
                                headlineContent = {
                                    Text(
                                        text = info.appName,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = info.packageName,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                trailingContent = {
                                    var enabled by remember(info) { mutableStateOf(false) }

                                    LaunchedEffect(info) {
                                        enabled = filter.contains(info.packageName)
                                    }

                                    Switch(
                                        checked = enabled,
                                        onCheckedChange = {
                                            enabled = it
                                            ioScope.launch {
                                                popup = true
                                                try {
                                                    val list = mutableListOf<String>()
                                                    list.addAll(filter)
                                                    if (it) {
                                                        list.add(info.packageName)
                                                    } else {
                                                        list.remove(info.packageName)
                                                    }
                                                    filter = list
                                                } finally {
                                                    popup = false
                                                }
                                            }
                                        },
                                        enabled = !popup
                                    )
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val filterText = json.encodeToString(filter)
                        storageStore.putString(
                            Const.SmartNotice.SMART_NOTICE_NOTIFICATION_FILTER,
                            filterText
                        )
                        onDismissRequest()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.text_save)
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