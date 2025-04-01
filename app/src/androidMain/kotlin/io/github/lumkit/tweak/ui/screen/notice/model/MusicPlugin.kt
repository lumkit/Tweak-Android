package io.github.lumkit.tweak.ui.screen.notice.model

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.collection.mutableFloatSetOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.util.AppInfoLoader
import io.github.lumkit.tweak.common.util.formatUptimeMinute
import io.github.lumkit.tweak.common.util.getStatusBarHeight
import io.github.lumkit.tweak.model.AppInfo
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.services.SmartNoticeNotificationListenerService
import io.github.lumkit.tweak.services.media.MediaCallback
import io.github.lumkit.tweak.ui.component.DetailItem
import io.github.lumkit.tweak.ui.component.FolderItem
import io.github.lumkit.tweak.ui.local.LocalStorageStore
import io.github.lumkit.tweak.ui.local.json
import io.github.lumkit.tweak.ui.view.AudioVisualizer
import io.github.lumkit.tweak.ui.view.BottomAlignedAudioVisualizer
import io.github.lumkit.tweak.ui.view.SmartNoticeFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlin.math.min

class MusicPlugin(
    override val factory: SmartNoticeFactory
) : SmartNoticeNotificationPlugin(
    factory,
    Const.SmartNotice.Observe.SMART_NOTICE_OBSERVE_MUSIC
) {
    @Composable
    override fun PreferenceContent(plugin: SmartNoticeNotificationPlugin) {
        MusicObserver(plugin)
    }

    private val componentName = ComponentName(
        factory.context,
        SmartNoticeNotificationListenerService::class.java
    )

    private val mediaSessionManager = factory.context.getSystemService(
        Context.MEDIA_SESSION_SERVICE
    ) as MediaSessionManager

    private val listenerForActiveSessions = MediaSessionManager.OnActiveSessionsChangedListener {
        it?.registerCallBack()
    }

    // 折叠View
    val minimizeComposeView = ComposeView(factory.context).apply {
        setContent {
            MinimizeContent(this@MusicPlugin)
        }
    }

    // 展开View
    val expandedComposeView = ComposeView(factory.context).apply {
        setContent {
            ExpandedContent(this@MusicPlugin)
        }
    }

    val appInfoLoader = AppInfoLoader(factory.context, 100)
    val callbackMap = MutableStateFlow<Map<String, MediaCallback>>(mapOf())
    var topMediaCallback = MutableStateFlow<MediaCallback?>(null)

    private var isRegister = false
    private fun registerCallBacks() {
        if (isRegister) return
        isRegister = true
        mediaSessionManager.addOnActiveSessionsChangedListener(
            listenerForActiveSessions,
            componentName
        )
        mediaSessionManager.getActiveSessions(componentName).registerCallBack()
    }

    private fun unregisterCallBacks() {
        if (!isRegister) return
        isRegister = false
        mediaSessionManager.getActiveSessions(componentName).forEach { controller ->
            callbackMap.value[controller.packageName]?.let(controller::unregisterCallback)
        }
        mediaSessionManager.removeOnActiveSessionsChangedListener(listenerForActiveSessions)
        factory.minimize(minimize = true)
        factory.isMediaComponent = false
    }

    private fun List<MediaController>.registerCallBack() {
        val filterText = TweakApplication.shared.getString(
            Const.SmartNotice.SMART_NOTICE_MEDIA_FILTER,
            null
        ) ?: Const.SmartNotice.MEDIA_FILTER_DEFAULT
        val filter = try {
            json.decodeFromString<List<String>>(filterText)
        } catch (_: Exception) {
            json.decodeFromString<List<String>>(Const.SmartNotice.MEDIA_FILTER_DEFAULT)
        }

        val map = mutableMapOf<String, MediaCallback>()

        val mediaControllers = filter {
            filter.contains(it.packageName)
        }

        mediaControllers.forEach { controller ->
            val mediaCallback = callbackMap.value[controller.packageName] ?: run {
                MediaCallback(controller, this@MusicPlugin).apply {
                    controller.registerCallback(this)
                }
            }
            map[controller.packageName] = mediaCallback
        }

        if (map.isEmpty()) {
            callbackMap.value.values.forEach {
                it.mediaController.unregisterCallback(it)
            }
        }

        callbackMap.value = map

        val callback = map.values.minByOrNull { it.struct.isPlaying() }

        topMediaCallback.value = callback

        if (topMediaCallback.value == null) {
            factory.minimize(minimize = true)
        } else {
            factory.showMedia(
                view = minimizeComposeView,
            )
        }
    }

    init {
        registerCallBacks()
        loadEnabled()
    }

    fun autoMinimize() {
        factory.expanded = true
        factory.handler.removeCallbacksAndMessages(null)
        factory.handler.postDelayed(
            {
                factory.expanded = false
                factory.showMedia(view = minimizeComposeView)
            },
            15_000
        )
    }

    override fun onEnableChanged(enabled: Boolean) {
        super.onEnableChanged(enabled)
        if (enabled) {
            registerCallBacks()
        } else {
            unregisterCallBacks()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterCallBacks()
    }
}

@Composable
private fun MusicObserver(plugin: SmartNoticeNotificationPlugin) {
    var dialogState by rememberSaveable { mutableStateOf(false) }

    DetailItem(
        onClick = {
            dialogState = true
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
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }

    MusicObserverDialog(dialogState, plugin) { dialogState = false }
}

@Composable
private fun MusicObserverDialog(
    visible: Boolean,
    plugin: SmartNoticeNotificationPlugin,
    onDismissRequest: () -> Unit,
) {
    if (visible) {
        AlertDialog(
            modifier = Modifier.padding(vertical = 28.dp),
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = stringResource(R.string.text_smart_notice_observe_music)
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
private fun SwitchRow(plugin: SmartNoticeNotificationPlugin) {
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

    MusicAppManagerDialog(visible = appManagerDialogState) { appManagerDialogState = false }
}

@Composable
private fun MusicAppManagerDialog(
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
                Const.SmartNotice.SMART_NOTICE_MEDIA_FILTER,
                null
            ) ?: Const.SmartNotice.MEDIA_FILTER_DEFAULT
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
                            Const.SmartNotice.SMART_NOTICE_MEDIA_FILTER,
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

private val rotation by lazy {
    Animatable(0f)
}
private var lastCover by mutableStateOf<Bitmap?>(null)
private const val rotationDuration = 16_000

@Composable
private fun MinimizeContent(musicPlugin: MusicPlugin) {
    val topCallback by musicPlugin.topMediaCallback.collectAsStateWithLifecycle()
    var visualizerView by remember { mutableStateOf<AudioVisualizer?>(null) }

    LaunchedEffect(Unit) {
        snapshotFlow { topCallback?.struct }
            .collectLatest { struct ->
                val cover = struct?.cover

                if (cover != lastCover) {
                    rotation.snapTo(0f)
                    lastCover = cover
                }

                if (struct?.isPlaying() == true) {
                    visualizerView?.start()
                    while (isActive) {
                        rotation.animateTo(
                            targetValue = rotation.value + 360f,
                            animationSpec = tween(
                                durationMillis = rotationDuration,
                                easing = LinearEasing
                            )
                        )
                        rotation.snapTo(rotation.value % 360f)
                    }
                } else {
                    rotation.stop()
                    visualizerView?.stop()
                }
            }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        contentColor = Color.White,
        color = Color.Black,
        onClick = {
            musicPlugin.factory.showMedia(
                musicPlugin.expandedComposeView,
                contentSize = { _, density, size, _ ->
                    with(density) {
                        DpSize(
                            min(size.width, size.height).toDp() - 28.dp,
                            190.dp
                        )
                    }
                },
                offset = { _, density, _, _ ->
                    with(density) {
                        DpOffset(
                            x = 0.dp,
                            y = getStatusBarHeight().toDp(),
                        )
                    }
                },
                radius = { _, density, _, _ ->
                    with(density) {
                        24.dp.toPx()
                    }
                }
            )
            musicPlugin.autoMinimize()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedCard(
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation.value),
                shape = CircleShape
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = topCallback?.struct?.cover,
                    contentDescription = null
                )
            }

            AndroidView(
                modifier = Modifier.size(32.dp, 24.dp),
                factory = {
                    AudioVisualizer(it)
                },
                update = {
                    visualizerView = it
                }
            )
        }
    }
}

private val durationAnimation by lazy {
    Animatable(0f)
}
private var durationState by mutableFloatStateOf(0f)

@Composable
private fun ExpandedContent(musicPlugin: MusicPlugin) {
    val topCallback by musicPlugin.topMediaCallback.collectAsStateWithLifecycle()
    var visualizerView by remember { mutableStateOf<BottomAlignedAudioVisualizer?>(null) }
    var appLogo by remember { mutableStateOf<Drawable?>(null) }
    var touched by remember { mutableStateOf(false) }
    val ioScope = rememberCoroutineScope { Dispatchers.IO }

    LaunchedEffect(Unit) {
        snapshotFlow { topCallback?.struct }
            .collectLatest {
                appLogo = it?.packageName?.let { pk ->
                    musicPlugin.appInfoLoader.loadIcon(pk).await()
                }
            }
    }

    LaunchedEffect(topCallback) {
        snapshotFlow { topCallback?.struct }
            .collectLatest {
                val targetValue =
                    (it?.playbackState?.position?.toFloat() ?: 0f) / (it?.duration?.toFloat() ?: 0f)
                durationState = targetValue
                durationAnimation.snapTo(targetValue)
                if (it?.isPlaying() == true) {
                    visualizerView?.start()
                    durationAnimation.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = (it.duration * (1 - durationState)).toInt(),
                            easing = LinearEasing
                        )
                    )
                } else {
                    visualizerView?.stop()
                    durationAnimation.stop()
                }
            }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        contentColor = Color.White,
        color = Color.Black,
    ) {
        Box {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomStart)
                    .alpha(.34f),
                factory = {
                    BottomAlignedAudioVisualizer(it)
                },
                update = {
                    visualizerView = it
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedCard(
                        modifier = Modifier.size(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            try {
                                musicPlugin.factory
                                    .context
                                    .packageManager
                                    .getLaunchIntentForPackage(
                                        topCallback?.struct?.packageName ?: ""
                                    )?.let {
                                        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        musicPlugin.factory.context.startActivity(it)
                                    }
                            } catch (_: Exception) {
                            }
                            topCallback?.mediaController?.sessionActivity?.send(0)
                            musicPlugin.factory.expanded = false
                            musicPlugin.factory.handler.removeCallbacksAndMessages(null)
                            musicPlugin.factory.showMedia(
                                view = musicPlugin.minimizeComposeView,
                            )
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AsyncImage(
                                model = topCallback?.struct?.cover,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                            Card(
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.BottomEnd),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                AsyncImage(
                                    modifier = Modifier.fillMaxSize(),
                                    model = appLogo,
                                    contentDescription = null,
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = buildString {
                                topCallback?.struct?.title?.let(::append)
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildString {
                                topCallback?.struct?.artist?.let(::append)
                            },
                            color = Color.White.copy(alpha = .85f),
                            style = MaterialTheme.typography.bodySmall,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Icon(
                        painter = painterResource(R.drawable.ic_up),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(
                                enabled = true,
                                indication = null,
                                interactionSource = null
                            ) {
                                musicPlugin.factory.expanded = false
                                musicPlugin.factory.handler.removeCallbacksAndMessages(null)
                                musicPlugin.factory.showMedia(
                                    view = musicPlugin.minimizeComposeView,
                                )
                            }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_previous),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(
                                enabled = true,
                                indication = null,
                                interactionSource = null,
                            ) {
                                topCallback?.mediaController?.transportControls?.skipToPrevious()
                                musicPlugin.autoMinimize()
                            }
                    )

                    Box(
                        modifier = Modifier.size(28.dp),
                    ) {
                        AnimatedContent(
                            targetState = topCallback?.struct?.isPlaying() == true
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (it) {
                                        R.drawable.ic_play_pause
                                    } else {
                                        R.drawable.ic_play_start
                                    }
                                ),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable(
                                        enabled = true,
                                        indication = null,
                                        interactionSource = null,
                                    ) {
                                        if (it) {
                                            topCallback?.mediaController?.transportControls?.pause()
                                        } else {
                                            topCallback?.mediaController?.transportControls?.play()
                                        }
                                        musicPlugin.autoMinimize()
                                    }
                            )
                        }
                    }

                    Icon(
                        painter = painterResource(R.drawable.ic_play_next),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(
                                enabled = true,
                                indication = null,
                                interactionSource = null,
                            ) {
                                topCallback?.mediaController?.transportControls?.skipToNext()
                                musicPlugin.autoMinimize()
                            }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ((topCallback?.struct?.duration ?: 0L) * if (touched) durationState else durationAnimation.value).toLong().formatUptimeMinute(),
                        textAlign = TextAlign.Start,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(40.dp),
                    )

                    Slider(
                        value = if (!touched) {
                            durationAnimation.value
                        } else {
                            durationState
                        },
                        onValueChange = {
                            durationState = it
                            touched = true
                            musicPlugin.autoMinimize()
                        },
                        onValueChangeFinished = {
                            touched = false
                            ioScope.launch {
                                durationAnimation.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = ((topCallback?.struct?.duration?.toInt() ?: 0) * (1 - durationState)).toInt(),
                                        easing = LinearEasing
                                    )
                                )
                                durationAnimation.snapTo(durationState)
                            }
                            topCallback?.mediaController
                                ?.transportControls
                                ?.seekTo(((topCallback?.struct?.duration ?: 0L) * durationState).toLong())
                        },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0xFF999999)
                        )
                    )

                    Text(
                        text = topCallback?.struct?.duration?.formatUptimeMinute() ?: "--:--",
                        textAlign = TextAlign.End,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(40.dp),
                    )
                }
            }
        }
    }
}