package io.github.lumkit.tweak.ui.screen.notice.model

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.Glide
import com.google.android.material.slider.Slider
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.util.AppInfoLoader
import io.github.lumkit.tweak.common.util.formatUptimeMinute
import io.github.lumkit.tweak.common.util.getStatusBarHeight
import io.github.lumkit.tweak.databinding.SmartNoticeMusicExpandedBinding
import io.github.lumkit.tweak.databinding.SmartNoticeMusicMinimizeBinding
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.model.LiveData
import io.github.lumkit.tweak.services.SmartNoticeNotificationListenerService
import io.github.lumkit.tweak.services.media.MediaCallback
import io.github.lumkit.tweak.services.media.MediaStruct
import io.github.lumkit.tweak.ui.component.DetailItem
import io.github.lumkit.tweak.ui.component.FolderItem
import io.github.lumkit.tweak.ui.local.json
import io.github.lumkit.tweak.ui.view.SmartNoticeFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToLong

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

    private val listenerForActiveSessions =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            controllers?.registerCallBack()
        }

    private val mediaCallbackPool: MutableLiveData<Map<String, MediaCallback>> =
        MutableLiveData(mapOf())

    val topMediaCallback = LiveData<MediaCallback?>()

    // 折叠View
    val minimizeBinding = SmartNoticeMusicMinimizeBinding.bind(
        View.inflate(
            factory.context,
            R.layout.smart_notice_music_minimize,
            null
        )
    )

    // 展开View
    private val expandedBinding = SmartNoticeMusicExpandedBinding.bind(
        View.inflate(
            factory.context,
            R.layout.smart_notice_music_expanded,
            null
        )
    )

    private var lastBitmap: Bitmap? = null
    private var animator: ObjectAnimator? = null

    private val sliderChangedListener = Slider.OnChangeListener { _, value, fromUser ->
        if (fromUser) {
            sliderAnimator?.cancel()
            autoMinimize()
        }
        try {
            expandedBinding.currentPosition.text = (value * (topMediaCallback.value?.struct?.value?.duration ?: 0)).roundToLong().formatUptimeMinute()
        }catch (e: Exception) {
            expandedBinding.currentPosition.text = (0L).formatUptimeMinute()
        }
    }

    private val sliderOnTouchListener = object : Slider.OnSliderTouchListener {
        override fun onStartTrackingTouch(slider: Slider) {
            sliderAnimator?.pause()
        }

        override fun onStopTrackingTouch(slider: Slider) {
            val progress = slider.value
            topMediaCallback.value?.mediaController?.transportControls?.seekTo(
                ((topMediaCallback.value?.struct?.value?.duration ?: 0) * progress).roundToLong()
            )

            topMediaCallback.value?.struct?.value?.also {
                sliderAnimator?.cancel()
                sliderAnimator = ValueAnimator.ofFloat(
                    progress,
                    1f
                ).apply {
                    duration = (it.duration.toFloat() - (progress * it.duration.toFloat())).roundToLong()
                    interpolator = LinearInterpolator()
                    addUpdateListener {
                        val value = it.animatedValue as Float
                        expandedBinding.slider.value = min(1f, value)
                    }
                }
                if (it.isPlaying()) {
                    sliderAnimator?.start()
                }
            }
        }
    }

    private val mediaStructObserver: (MediaStruct?) -> Unit = {
        println("struct: $it")
        animator?.cancel()
        if (it == null) {
            lastBitmap = null
            println("remove")
        } else {
            println("observe")

            val mediaCallback = topMediaCallback.value

            Glide.with(minimizeBinding.cover)
                .load(it.cover)
                .placeholder(lastBitmap?.toDrawable(factory.context.resources))
                .into(minimizeBinding.cover)

            Glide.with(expandedBinding.cover)
                .load(it.cover)
                .placeholder(lastBitmap?.toDrawable(factory.context.resources))
                .into(expandedBinding.cover)

            expandedBinding.title.text = it.title
            expandedBinding.artist.text = it.artist

            expandedBinding.cover.setOnClickListener { _ ->
                factory.context.packageManager.getLaunchIntentForPackage(
                    mediaCallback?.mediaController?.packageName ?: ""
                )?.let { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    factory.context.startActivity(intent)
                }
                mediaCallback?.mediaController?.sessionActivity?.send(0)
                factory.expanded = false
                factory.handler.removeCallbacksAndMessages(null)
                factory.showMedia(view = minimizeBinding.root)
            }

            expandedBinding.previous.setOnClickListener { _ ->
                mediaCallback?.mediaController?.transportControls?.skipToPrevious()
                autoMinimize()
            }
            expandedBinding.next.setOnClickListener { _ ->
                mediaCallback?.mediaController?.transportControls?.skipToNext()
                autoMinimize()
            }
            expandedBinding.playContainer.setOnClickListener { _ ->
                if (it.isPlaying()) {
                    mediaCallback?.mediaController?.transportControls?.pause()
                    sliderAnimator?.pause()
                } else {
                    mediaCallback?.mediaController?.transportControls?.play()
                    sliderAnimator?.start()
                }
                autoMinimize()
            }
            expandedBinding.duration.text = it.duration.formatUptimeMinute()
            expandedBinding.currentPosition.text = it.playbackState.position.formatUptimeMinute()
            expandedBinding.slider.apply {
                value = min(it.playbackState.position.toFloat() / it.duration.toFloat(), 1f)
                setLabelFormatter { value ->
                    (value * it.duration).roundToLong().formatUptimeMinute()
                }
                removeOnChangeListener(sliderChangedListener)
                removeOnSliderTouchListener(sliderOnTouchListener)
                addOnChangeListener(sliderChangedListener)
                addOnSliderTouchListener(sliderOnTouchListener)
            }

            if (it.isPlaying()) {
                animator = ObjectAnimator.ofFloat(
                    minimizeBinding.cover,
                    "rotation",
                    minimizeBinding.cover.rotation,
                    minimizeBinding.cover.rotation + 360f,
                ).apply {
                    duration = 15_000
                    interpolator = LinearInterpolator()
                    repeatCount = ValueAnimator.INFINITE
                }
                animator?.start()
                if (lastBitmap == it.cover) {
                    sliderAnimator?.start()
                } else {
                    sliderAnimator?.cancel()
                    sliderAnimator = ValueAnimator.ofFloat(
                        it.playbackState.position.toFloat() / it.duration.toFloat(),
                        1f
                    ).apply {
                        duration = abs((it.duration - it.playbackState.position.toFloat()).roundToLong())
                        interpolator = LinearInterpolator()
                        addUpdateListener {
                            val value = it.animatedValue as Float
                            expandedBinding.slider.value = min(value, 1f)
                        }
                        start()
                    }
                }
                minimizeBinding.visualizer.start()
                expandedBinding.visualizer.start()
                expandedBinding.play.visibility = View.GONE
                expandedBinding.pause.visibility = View.VISIBLE
            } else {
                sliderAnimator?.pause()
                minimizeBinding.visualizer.stop()
                expandedBinding.visualizer.stop()
                expandedBinding.play.visibility = View.VISIBLE
                expandedBinding.pause.visibility = View.GONE
            }

            lastBitmap = it.cover
        }
    }

    private val appInfoLoader = AppInfoLoader(factory.context, 100)

    private var lastCallback: MediaCallback? = null
    private var sliderAnimator: ValueAnimator? = null
    private val topCallbackObserver: (MediaCallback?) -> Unit = {
        animator?.cancel()
        lastCallback?.struct?.removeObserver(mediaStructObserver)
        if (it == null) {
            println("end")
            lastCallback?.struct?.value = null
            factory.minimize(minimize = true)
        } else {
            println("start")
            it.struct.observe(mediaStructObserver)
            lastCallback = it
            factory.showMedia(
                view = minimizeBinding.root,
            )
            Glide.with(minimizeBinding.cover)
                .load(it.struct.value?.cover)
                .placeholder(lastBitmap?.toDrawable(factory.context.resources))
                .into(minimizeBinding.cover)

            Glide.with(expandedBinding.cover)
                .load(it.struct.value?.cover)
                .placeholder(lastBitmap?.toDrawable(factory.context.resources))
                .into(expandedBinding.cover)

            expandedBinding.title.text = it.struct.value?.title
            expandedBinding.artist.text = it.struct.value?.artist

            expandedBinding.cover.setOnClickListener { _ ->
                factory.context.packageManager.getLaunchIntentForPackage(it.mediaController.packageName)
                    ?.let { intent ->
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        factory.context.startActivity(intent)
                    }
                it.mediaController.sessionActivity?.send(0)
                factory.expanded = false
                factory.handler.removeCallbacksAndMessages(null)
                factory.showMedia(view = minimizeBinding.root)
            }

            expandedBinding.previous.setOnClickListener { _ ->
                it.mediaController.transportControls.skipToPrevious()
                autoMinimize()
            }
            expandedBinding.next.setOnClickListener { _ ->
                it.mediaController.transportControls.skipToNext()
                autoMinimize()
            }
            expandedBinding.playContainer.setOnClickListener { _ ->
                if (it.struct.value?.isPlaying() == true) {
                    it.mediaController.transportControls.pause()
                    sliderAnimator?.pause()
                } else {
                    it.mediaController.transportControls.play()
                    sliderAnimator?.start()
                }
                autoMinimize()
            }
            expandedBinding.duration.text = it.struct.value?.duration?.formatUptimeMinute()
            expandedBinding.currentPosition.text =
                it.struct.value?.playbackState?.position?.formatUptimeMinute()

            expandedBinding.slider.apply {
                value = min(1f, (it.mediaController.playbackState?.position ?: 0).toFloat() / (it.struct.value?.duration ?: 0).toFloat())
                setLabelFormatter { value ->
                    (value * (it.struct.value?.duration ?: 0)).roundToLong().formatUptimeMinute()
                }
                removeOnChangeListener(sliderChangedListener)
                removeOnSliderTouchListener(sliderOnTouchListener)
                addOnChangeListener(sliderChangedListener)
                addOnSliderTouchListener(sliderOnTouchListener)
            }

            CoroutineScope(Dispatchers.IO).launch {
                val icon = appInfoLoader.loadIcon(it.mediaController.packageName).await()
                withContext(Dispatchers.Main) {
                    Glide.with(expandedBinding.logo)
                        .load(icon)
                        .into(expandedBinding.logo)
                }
            }

            if (animator == null || lastBitmap != it.struct.value?.cover) {
                animator = ObjectAnimator.ofFloat(
                    minimizeBinding.cover,
                    "rotation",
                    minimizeBinding.cover.rotation,
                    minimizeBinding.cover.rotation + 360f,
                ).apply {
                    duration = 15_000
                    interpolator = LinearInterpolator()
                    repeatCount = ValueAnimator.INFINITE
                }
            }
            it.struct.value?.let { struct ->
                if (struct.isPlaying()) {
                    animator?.start()
                    if (sliderAnimator == null) {
                        sliderAnimator = ValueAnimator.ofFloat(
                            struct.playbackState.position.toFloat() / struct.duration.toFloat(),
                            1f
                        ).apply {
                            duration = abs((struct.duration - struct.playbackState.position.toFloat()).roundToLong())
                            interpolator = LinearInterpolator()
                            addUpdateListener {
                                val value = it.animatedValue as Float
                                expandedBinding.slider.value = min(1f, value)
                            }
                            start()
                        }
                    }
                    sliderAnimator?.start()
                    minimizeBinding.visualizer.start()
                    expandedBinding.visualizer.start()
                    expandedBinding.play.visibility = View.GONE
                    expandedBinding.pause.visibility = View.VISIBLE
                } else {
                    animator?.cancel()
                    sliderAnimator?.cancel()
                    minimizeBinding.visualizer.stop()
                    expandedBinding.visualizer.stop()
                    expandedBinding.play.visibility = View.VISIBLE
                    expandedBinding.pause.visibility = View.GONE
                }
            }
        }
    }

    private var isRegister = false
    private fun registerCallBacks() {
        if (isRegister) return
        isRegister = true
        mediaSessionManager.addOnActiveSessionsChangedListener(
            listenerForActiveSessions,
            componentName
        )
        mediaSessionManager.getActiveSessions(componentName).registerCallBack()
        topMediaCallback.observe(topCallbackObserver)
    }

    private fun unregisterCallBacks() {
        if (!isRegister) return
        isRegister = false
        mediaSessionManager.getActiveSessions(componentName).forEach { controller ->
            mediaCallbackPool.value?.apply {
                this[controller.packageName]?.let {
                    controller.unregisterCallback(it)
                }
            }
        }
        topMediaCallback.clear()
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
            val callbackMap = mediaCallbackPool.value ?: mapOf()
            map.putAll(callbackMap)
            val callback = map[controller.packageName]
            map[controller.packageName] =
                callback ?: MediaCallback(controller, this@MusicPlugin).also {
                    controller.registerCallback(it)
                }
            mediaCallbackPool.value = map
        }

        val callback = mediaCallbackPool.value?.get(mediaControllers.lastOrNull()?.packageName)
        if (topMediaCallback.value == null || topMediaCallback.value?.mediaController?.packageName != callback?.mediaController?.packageName) {
            println("更新Callback")
            if (callback != null) {
                topMediaCallback.observe(topCallbackObserver)
            }
            topMediaCallback.value = callback
        }

        if (mediaControllers.isEmpty()) {
            topMediaCallback.value = null
            factory.isMediaComponent = false
            mediaCallbackPool.value?.forEach { (_, call) ->
                call.mediaController.unregisterCallback(call)
            }
            mediaCallbackPool.value = mapOf()
        }
    }

    init {
        registerCallBacks()
        loadEnabled()

        minimizeBinding.root.setOnClickListener {
            factory.showMedia(
                expandedBinding.root,
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
            autoMinimize()
        }

        expandedBinding.minimize.setOnClickListener {
            factory.expanded = false
            factory.handler.removeCallbacksAndMessages(null)
            factory.showMedia(
                view = minimizeBinding.root,
            )
        }
    }

    private fun autoMinimize() {
        factory.expanded = true
        factory.handler.removeCallbacksAndMessages(null)
        factory.handler.postDelayed(
            {
                factory.expanded = false
                factory.showMedia(view = minimizeBinding.root)
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
}