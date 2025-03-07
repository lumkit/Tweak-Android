package io.github.lumkit.tweak.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.util.ServiceUtils
import io.github.lumkit.tweak.common.util.getDiveSize
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.updateNotificationFilter
import io.github.lumkit.tweak.services.media.MediaCallback
import io.github.lumkit.tweak.ui.lifecycle.ComposeViewLifecycleOwner
import io.github.lumkit.tweak.ui.local.json
import io.github.lumkit.tweak.ui.screen.ScreenRoute
import io.github.lumkit.tweak.ui.screen.notice.SmartNoticeViewModel
import io.github.lumkit.tweak.ui.token.SmartNoticeCapsuleDefault
import io.github.lumkit.tweak.ui.view.SmartNoticeWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.math.min

val LocalSmartNoticeView = staticCompositionLocalOf<ComposeView> { error("not provided.") }
val LocalSmartNoticeWindowManager =
    staticCompositionLocalOf<WindowManager> { error("not provided.") }
val LocalSmartNoticeWindowParams = staticCompositionLocalOf<LayoutParams> { error("not provided.") }

class SmartNoticeService : Service() {

    companion object {
        const val SMART_NOTICE_CHANNEL_ID = "Tweak Smart Notice"
        const val SMART_NOTICE_CHANNEL_NAME = "Smart Notice"

        const val ACTION_START = "acton_start"
        const val ACTION_STOP = "acton_stop"
        const val ACTION_NOTIFICATION_STATUS_SHOW = "action_notification_status_show"
        const val ACTION_NOTIFICATION_STATUS_HIDE = "action_notification_status_hide"
        const val ACTION_GAME_MODE_ENABLED = "action_game_mode_enabled"
        const val ACTION_GAME_MODE_DISABLED = "action_game_mode_disabled"

        // 充电变化
        const val ACTION_POWER_CONNECTED = "ACTION_POWER_CONNECTED"
        const val ACTION_POWER_DISCONNECTED = "ACTION_POWER_DISCONNECTED"

        // 属性变化
        const val ACTION_MARGIN_TOP = "ACTION_MARGIN_TOP"
        const val ACTION_MARGIN_START = "ACTION_MARGIN_START"
        const val ACTION_CUTOUT_GRAVITY = "ACTION_CUTOUT_GRAVITY"
        const val ACTION_CUTOUT_WIDTH = "ACTION_CUTOUT_WIDTH"
        const val ACTION_CUTOUT_HEIGHT = "ACTION_CUTOUT_HEIGHT"
        const val ACTION_CUTOUT_RADIUS = "ACTION_CUTOUT_RADIUS"
        const val ACTION_MEDIA_OBSERVE = "ACTION_MEDIA_OBSERVE"
        const val ACTION_UPDATE_MEDIA_FILTER = "ACTION_UPDATE_MEDIA_FILTER"
        const val ACTION_NOTIFICATION_OBSERVE = "ACTION_NOTIFICATION_OBSERVE"
        const val ACTION_UPDATE_NOTIFICATION_FILTER = "ACTION_UPDATE_NOTIFICATION_FILTER"
        const val ACTION_POST_NOTIFICATION = "ACTION_POST_NOTIFICATION"

        fun canStartSmartNotice(): Boolean {
            val switch =
                TweakApplication.shared.getBoolean(Const.SmartNotice.SMART_NOTICE_SWITCH, false)
            val eas =
                TweakApplication.shared.getBoolean(Const.APP_ENABLED_ACCESSIBILITY_SERVICE, false)
            return switch && eas
        }

        fun isRunning() = ServiceUtils.isServiceRunning(
            TweakApplication.application,
            SmartNoticeService::class.java.name
        )

        fun Context.startSmartNotice() {
            val intent = Intent(this, SmartNoticeService::class.java)
            intent.action = ACTION_START
            startService(intent)
        }

        suspend fun Context.stopSmartNotice(now: Boolean = false) {
            val intent = Intent(this, SmartNoticeService::class.java)
            intent.action = ACTION_STOP
            startService(intent)
            if (!now) {
                delay(SmartNoticeWindow.animatorDuration.value)
            }
            stopService(intent)
        }

        suspend fun Context.reloadProperties() {
            if (canStartSmartNotice() && isRunning() && runBlocking { SmartNoticeViewModel.checkAccessibilityService() }) {
                stopSmartNotice(true)
                startSmartNotice()
            }
        }

        fun Context.showNotificationStatus(show: Boolean) {
            if (canStartSmartNotice() && isRunning()) {
                val intent = Intent(this, SmartNoticeService::class.java)
                intent.action =
                    if (show) ACTION_NOTIFICATION_STATUS_SHOW else ACTION_NOTIFICATION_STATUS_HIDE
                startService(intent)
            }
        }

        fun Context.enableGameMode() {
            if (canStartSmartNotice() && isRunning()) {
                val intent = Intent(this, SmartNoticeService::class.java)
                intent.action = ACTION_GAME_MODE_ENABLED
                startService(intent)
            }
        }

        fun Context.disableGameMode() {
            if (canStartSmartNotice() && isRunning()) {
                val intent = Intent(this, SmartNoticeService::class.java)
                intent.action = ACTION_GAME_MODE_DISABLED
                startService(intent)
            }
        }

        fun Context.chargeChange(
            action: String
        ) {
            if (canStartSmartNotice() && isRunning() && runBlocking { SmartNoticeViewModel.checkAccessibilityService() }) {
                val intent = Intent(this, SmartNoticeService::class.java)
                intent.action = action
                startService(intent)
            }
        }

        fun Context.updateTop(position: Float) {
            if (canStartSmartNotice() && isRunning() && runBlocking { SmartNoticeViewModel.checkAccessibilityService() }) {
                val intent = Intent(this, SmartNoticeService::class.java)
                intent.action = ACTION_MARGIN_TOP
                intent.putExtra("y", position)
                startService(intent)
            }
        }

        fun Context.updateStart(position: Float) {
            if (canStartSmartNotice() && isRunning() && runBlocking { SmartNoticeViewModel.checkAccessibilityService() }) {
                val intent = Intent(this, SmartNoticeService::class.java)
                intent.action = ACTION_MARGIN_START
                intent.putExtra("x", position)
                startService(intent)
            }
        }

        fun Context.updateCutoutGravity(gravity: Int) {
            if (canStartSmartNotice() && isRunning() && runBlocking { SmartNoticeViewModel.checkAccessibilityService() }) {
                val intent = Intent(this, SmartNoticeService::class.java)
                intent.action = ACTION_CUTOUT_GRAVITY
                intent.putExtra("gravity", gravity)
                startService(intent)
            }
        }

        fun Context.updateCutoutWidth(widthDp: Float) {
            if (canStartSmartNotice() && isRunning() && runBlocking { SmartNoticeViewModel.checkAccessibilityService() }) {
                val intent = Intent(this, SmartNoticeService::class.java)
                intent.action = ACTION_CUTOUT_WIDTH
                intent.putExtra("width", widthDp)
                startService(intent)
            }
        }

        fun Context.updateCutoutHeight(heightDp: Float) {
            if (canStartSmartNotice() && isRunning() && runBlocking { SmartNoticeViewModel.checkAccessibilityService() }) {
                val intent = Intent(this, SmartNoticeService::class.java)
                intent.action = ACTION_CUTOUT_HEIGHT
                intent.putExtra("height", heightDp)
                startService(intent)
            }
        }

        fun Context.updateCutoutRadius(radiusDp: Float) {
            if (canStartSmartNotice() && isRunning() && runBlocking { SmartNoticeViewModel.checkAccessibilityService() }) {
                val intent = Intent(this, SmartNoticeService::class.java)
                intent.action = ACTION_CUTOUT_RADIUS
                intent.putExtra("radius", radiusDp)
                startService(intent)
            }
        }

        fun Context.updateMediaObserve(observe: Boolean) {
            if (canStartSmartNotice() && isRunning() && runBlocking { SmartNoticeViewModel.checkAccessibilityService() }) {
                val intent = Intent(this, SmartNoticeService::class.java)
                intent.action = ACTION_MEDIA_OBSERVE
                intent.putExtra("observe", observe)
                startService(intent)
            }
        }

        fun Context.updateMediaFilter() {
            if (canStartSmartNotice() && isRunning() && runBlocking { SmartNoticeViewModel.checkAccessibilityService() }) {
                val intent = Intent(this, SmartNoticeService::class.java)
                intent.action = ACTION_UPDATE_MEDIA_FILTER
                startService(intent)
            }
        }

        fun Context.updateNotificationObserve(observe: Boolean) {
            if (canStartSmartNotice() && isRunning() && runBlocking { SmartNoticeViewModel.checkAccessibilityService() }) {
                val intent = Intent(this, SmartNoticeService::class.java)
                intent.action = ACTION_NOTIFICATION_OBSERVE
                intent.putExtra("observe", observe)
                startService(intent)
            }
        }

        fun Context.updateNotificationFilter() {
            if (canStartSmartNotice() && isRunning() && runBlocking { SmartNoticeViewModel.checkAccessibilityService() }) {
                val intent = Intent(this, SmartNoticeService::class.java)
                intent.action = ACTION_UPDATE_NOTIFICATION_FILTER
                startService(intent)
            }
        }

        fun Context.postNotification(notification: StatusBarNotification?) {
            if (canStartSmartNotice() && isRunning() && runBlocking { SmartNoticeViewModel.checkAccessibilityService() }) {
                val intent = Intent(this, SmartNoticeService::class.java)
                intent.action = ACTION_POST_NOTIFICATION
                intent.putExtra("notification", notification)
                startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        initService()
    }

    private lateinit var notificationManager: NotificationManager
    private var windowManager: WindowManager? = null
    private var smartNoticeView: SmartNoticeWindow? = null
    private var smartNoticeLifecycleOwner: ComposeViewLifecycleOwner? = null
    private val params = LayoutParams()
    val callbackMap = MutableStateFlow<Map<String, MediaCallback>>(mapOf())
    var topMediaCallback = MutableStateFlow<MediaCallback?>(null)

    private fun initService() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                SMART_NOTICE_CHANNEL_ID,
                SMART_NOTICE_CHANNEL_NAME,
                importance
            ).apply {
                description = getString(R.string.text_smart_notice_notification_channel_description)
            }
            // 注册通知渠道
            notificationManager.createNotificationChannel(channel)
        }

        initSmartNotice()

        run {
            val filter = IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED)
            registerReceiver(orientationChangeReceiver, filter)
        }

        // 电量
        run {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(batteryReceiver, filter)
        }

        // 充电
        run {
            val filter = IntentFilter(Intent.ACTION_POWER_CONNECTED)
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
            registerReceiver(chargingReceiver, filter)
        }

        // 息屏
        run {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenStateReceiver, filter)
        }

        // 音乐
        run {
            TweakAccessibilityService.mediaSessionManager?.apply {
                addOnActiveSessionsChangedListener(
                    listenerForActiveSessions,
                    TweakAccessibilityService.componentName,
                )

                val filterText = TweakApplication.shared.getString(
                    Const.SmartNotice.SMART_NOTICE_MEDIA_FILTER,
                    null
                ) ?: Const.SmartNotice.MEDIA_FILTER_DEFAULT
                val filter = json.decodeFromString<List<String>>(filterText)
                val map = mutableMapOf<String, MediaCallback>()
                getActiveSessions(TweakAccessibilityService.componentName).filter {
                    filter.contains(it.packageName)
                }.forEach { controller ->
                    val callback = MediaCallback(controller, this@SmartNoticeService)
                    map.putAll(callbackMap.value)
                    map[controller.packageName] = callback
                    controller.registerCallback(callback)
                }
                callbackMap.value = map
            }
        }
    }

    private fun notify(
        id: Int,
        title: String,
        content: String,
        @DrawableRes icon: Int,
        dismiss: Boolean = true,
        contentIntent: PendingIntent? = null,
    ) {
        val notification = NotificationCompat.Builder(this, SMART_NOTICE_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(icon)
            .setAutoCancel(dismiss)
            .setOngoing(!dismiss)
            .setContentIntent(contentIntent)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat.from(this)
            .notify(id, notification)
    }

    private fun showStatusNotification() {
        val deepLinkUri = "${Const.Navigation.DEEP_LINE}/${ScreenRoute.SMART_NOTICE}".toUri()
        val startIntent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notify(
            id = 301,
            title = getString(R.string.text_smart_notice_notification_title),
            content = getString(R.string.text_smart_notice_notification_subtitle),
            icon = R.mipmap.ic_tweak_logo,
            dismiss = false,
            contentIntent = pendingIntent
        )
    }

    private fun hideStatusNotification() {
        NotificationManagerCompat.from(this).cancel(301)
    }

    private fun showAlert() {
        // 类型
        val accessibilityServiceWindowManager = TweakAccessibilityService.windowManager

        params.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//6.0+
            if (accessibilityServiceWindowManager == null) {
                LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            }
        } else {
            LayoutParams.TYPE_SYSTEM_ALERT
        }

        windowManager = accessibilityServiceWindowManager
            ?: getSystemService(Context.WINDOW_SERVICE) as WindowManager

        params.format = PixelFormat.TRANSLUCENT
        params.width = LayoutParams.WRAP_CONTENT
        params.height = LayoutParams.WRAP_CONTENT
        params.gravity = try {
            SmartNoticeWindow.Companion.Gravity.entries[
                TweakApplication.shared.getInt(
                    Const.SmartNotice.SMART_NOTICE_CUTOUT_POSITION,
                    SmartNoticeWindow.Companion.Gravity.Center.ordinal
                )
            ].gravity
        } catch (e: Exception) {
            e.printStackTrace()
            Gravity.CENTER
        } or Gravity.TOP
        params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL or
                LayoutParams.FLAG_NOT_FOCUSABLE or
                LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                LayoutParams.FLAG_LAYOUT_NO_LIMITS

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

//        smartNoticeComposeView = ComposeView(this).apply {
//            windowManager?.let {
//                setContent {
//                    Main {
//                        AppTheme {
//                            windowManager?.let {
//                                CompositionLocalProvider(
//                                    LocalSmartNoticeView provides this,
//                                    LocalSmartNoticeWindowManager provides it,
//                                    LocalSmartNoticeWindowParams provides params,
//                                ) {
//                                    SmartNoticeFloatWindow()
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }

        smartNoticeView = windowManager?.let {
            SmartNoticeWindow(
                service = this,
                windowManager = it,
                windowLayoutParams = params
            )
        }

        smartNoticeLifecycleOwner = ComposeViewLifecycleOwner().apply {
            onCreate()
            attachToDecorView(smartNoticeView)
        }

        try {
            windowManager?.addView(
                smartNoticeView,
                params,
            )
        } catch (_: Exception) {
        }

        smartNoticeLifecycleOwner?.onStart()
        smartNoticeLifecycleOwner?.onResume()

        if (TweakApplication.shared.getBoolean(Const.SmartNotice.SMART_NOTICE_GAME_MODE, true)) {
            gameModeChange()
        } else {
            smartNoticeView?.show()
        }
    }

    // 游戏模式广播
    private val orientationChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (TweakApplication.shared.getBoolean(
                    Const.SmartNotice.SMART_NOTICE_GAME_MODE,
                    true
                )
            ) {
                gameModeChange()
            }
        }
    }

    private var batteryPercentageState by mutableFloatStateOf(0f)
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // 获取电池电量
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

            // 计算电池百分比
            batteryPercentageState = (level / scale.toFloat()) * 100
        }
    }

    private val chargingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // 未开启消息订阅则退出
            if (!TweakApplication.shared.getBoolean(
                    Const.SmartNotice.Observe.SMART_NOTICE_OBSERVE_CHARGE,
                    true
                )
            ) {
                return
            }
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    context.chargeChange(ACTION_POWER_CONNECTED)
                }

                Intent.ACTION_POWER_DISCONNECTED -> {
                    context.chargeChange(ACTION_POWER_DISCONNECTED)
                }
            }
        }
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    if (!TweakApplication.shared.getBoolean(
                            Const.SmartNotice.SMART_NOTICE_ALWAYS_SHOW,
                            false
                        )
                    ) {
                        smartNoticeView?.hide()
                    } else {
                        runBlocking {
                            stopSmartNotice(true)
                            startSmartNotice()
                        }
                    }
                }

                else -> {
                    if (smartNoticeView?.isShow == false) {
                        runBlocking {
                            stopSmartNotice(true)
                            startSmartNotice()
                        }
                    }
                }
            }
        }
    }

    private val listenerForActiveSessions = OnActiveSessionsChangedListener { controllers ->
        val filterText = TweakApplication.shared.getString(
            Const.SmartNotice.SMART_NOTICE_MEDIA_FILTER,
            null
        ) ?: Const.SmartNotice.MEDIA_FILTER_DEFAULT


        val filter = json.decodeFromString<List<String>>(filterText)
        controllers?.filter { filter.contains(it.packageName) }?.forEach { controller ->
            val callback = MediaCallback(controller, this@SmartNoticeService)
            val map = mutableMapOf<String, MediaCallback>()
            map.putAll(callbackMap.value)
            map[controller.packageName] = callback
            controller.registerCallback(callback)
            callbackMap.value = map
        }
    }

    private fun gameModeChange() {
        val newConfig: Configuration = resources.configuration
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            smartNoticeView?.hide()
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            smartNoticeView?.show()
        }
    }

    private fun initSmartNotice() {
        val smartNoticeSwitch = TweakApplication.shared.getBoolean(
            Const.SmartNotice.SMART_NOTICE_SWITCH,
            false
        )

        val showAlert = smartNoticeSwitch && Settings.canDrawOverlays(this)

        // 显示悬浮窗
        if (showAlert) {
            showAlert()
        }

        // 显示通知
        if (TweakApplication.shared.getBoolean(
                Const.SmartNotice.SMART_NOTICE_NOTIFICATION,
                true
            ) && smartNoticeSwitch
        ) {
            showStatusNotification()
        }
    }

    @SuppressLint("DefaultLocale")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            // 开启通知
            ACTION_START -> {

            }

            // 关闭通知
            ACTION_STOP -> {
                smartNoticeView?.hide()
                hideStatusNotification()
            }

            // 显示通知
            ACTION_NOTIFICATION_STATUS_SHOW -> {
                showStatusNotification()
            }

            // 隐藏通知
            ACTION_NOTIFICATION_STATUS_HIDE -> {
                hideStatusNotification()
            }

            ACTION_GAME_MODE_ENABLED -> {
                gameModeChange()
            }

            ACTION_GAME_MODE_DISABLED -> {
                smartNoticeView?.show()
            }

            ACTION_POWER_CONNECTED -> {
                smartNoticeView?.notificationComponentExpandedState?.value = false
                smartNoticeView?.toast(
                    componentSize = {
                        val diveSize = getDiveSize()
                        DpSize(
                            width = with(density) {
                                min(diveSize.width, diveSize.height).toDp() - 28.dp * 2f
                            },
                            height = 32.dp
                        )
                    }
                ) {view, _ ->
                    Charge(true, view)
                }
            }

            ACTION_POWER_DISCONNECTED -> {
                smartNoticeView?.notificationComponentExpandedState?.value = false
                smartNoticeView?.toast(
                    componentSize = {
                        val diveSize = getDiveSize()
                        DpSize(
                            width = with(density) {
                                min(diveSize.width, diveSize.height).toDp() - 28.dp * 2f
                            },
                            height = 32.dp
                        )
                    }
                ) {view, _ ->
                    Charge(false, view)
                }
            }

            ACTION_MARGIN_TOP -> {
                if (smartNoticeView?.mediaComponentExpanded?.value == false) {
                    val offsetY = intent.getFloatExtra("y", 0f)
                    try {
                        params.y = with(TweakApplication.density) {
                            offsetY.dp.roundToPx()
                        }
                        windowManager?.updateViewLayout(
                            smartNoticeView,
                            params,
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            ACTION_MARGIN_START -> {
                if (smartNoticeView?.mediaComponentExpanded?.value == false) {
                    val offsetX = intent.getFloatExtra("x", 0f)
                    try {
                        params.x = with(TweakApplication.density) {
                            offsetX.dp.roundToPx()
                        }
                        windowManager?.updateViewLayout(
                            smartNoticeView,
                            params,
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            ACTION_CUTOUT_GRAVITY -> {
                val gravity = intent.getIntExtra("gravity", Gravity.CENTER)
                try {
                    params.gravity = gravity or Gravity.TOP
                    windowManager?.updateViewLayout(
                        smartNoticeView,
                        params
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            ACTION_CUTOUT_WIDTH -> {
                if (topMediaCallback.value == null) {
                    val width =
                        intent.getFloatExtra("width", SmartNoticeCapsuleDefault.CapsuleWidth.value)
                    try {
                        params.width = with(TweakApplication.density) {
                            width.dp.roundToPx()
                        }
                        SmartNoticeWindow.setCustomSize(
                            SmartNoticeWindow.islandCustomSize.value.copy(
                                width = params.width.toFloat()
                            )
                        )
                        windowManager?.updateViewLayout(
                            smartNoticeView,
                            params,
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            ACTION_CUTOUT_HEIGHT -> {
                if (topMediaCallback.value == null) {
                    val height =
                        intent.getFloatExtra(
                            "height",
                            SmartNoticeCapsuleDefault.CapsuleHeight.value
                        )
                    try {
                        params.height = with(TweakApplication.density) {
                            height.dp.roundToPx()
                        }
                        SmartNoticeWindow.setCustomSize(
                            SmartNoticeWindow.islandCustomSize.value.copy(
                                height = params.height.toFloat()
                            )
                        )
                        windowManager?.updateViewLayout(
                            smartNoticeView,
                            params,
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            ACTION_CUTOUT_RADIUS -> {
                with(TweakApplication.density) {
                    if (topMediaCallback.value == null) {
                        val radius = intent.getFloatExtra(
                            "radius",
                            SmartNoticeCapsuleDefault.CapsuleHeight.value / 2f
                        )
                        smartNoticeView?.radius = radius.dp.toPx()
                    }
                }
            }

            ACTION_MEDIA_OBSERVE -> {
                val observe = intent.getBooleanExtra("observe", false)
                if (observe) {
                    smartNoticeView?.showMedia()
                } else {
                    smartNoticeView?.hideMedia()
                }
            }

            ACTION_UPDATE_MEDIA_FILTER -> {
                TweakAccessibilityService.mediaSessionManager?.apply {
                    val filterText = TweakApplication.shared.getString(
                        Const.SmartNotice.SMART_NOTICE_MEDIA_FILTER,
                        null
                    ) ?: Const.SmartNotice.MEDIA_FILTER_DEFAULT
                    val filter = json.decodeFromString<List<String>>(filterText)
                    val map = mutableMapOf<String, MediaCallback>()
                    getActiveSessions(TweakAccessibilityService.componentName).filter {
                        filter.contains(it.packageName)
                    }.forEach { controller ->
                        val callback = MediaCallback(controller, this@SmartNoticeService)
                        map.putAll(callbackMap.value)
                        map[controller.packageName] = callback
                        controller.registerCallback(callback)
                    }
                    callbackMap.value = map
                    if (map[topMediaCallback.value?.mediaController?.packageName] == null) {
                        topMediaCallback.value = map.values.firstOrNull()
                    }
                }
            }

            ACTION_POST_NOTIFICATION -> {
                val notification: StatusBarNotification? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("notification", StatusBarNotification::class.java)
                } else {
                    intent.getParcelableExtra("notification")
                }
                if (notification != null) {
                    smartNoticeView?.notify(notification)
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        try {
            windowManager?.removeView(smartNoticeView)
        } catch (_: Exception) {
        }
        windowManager = null
        smartNoticeLifecycleOwner?.onPause()
        smartNoticeLifecycleOwner?.onStop()
        smartNoticeLifecycleOwner?.onDestroy()
        smartNoticeLifecycleOwner = null
        smartNoticeView?.release()
        smartNoticeView = null
        callbackMap.value.forEach { (_, callback) ->
            callback.mediaController.unregisterCallback(callback)
        }
        try {
            TweakAccessibilityService.mediaSessionManager?.removeOnActiveSessionsChangedListener(
                listenerForActiveSessions
            )
        } catch (_: Exception) { }
        unregisterReceiver(orientationChangeReceiver)
        unregisterReceiver(batteryReceiver)
        unregisterReceiver(chargingReceiver)
        unregisterReceiver(screenStateReceiver)
        super.onDestroy()
    }

    @SuppressLint("DefaultLocale")
    @Composable
    private fun Charge(connected: Boolean, smartNoticeWindow: SmartNoticeWindow) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    smartNoticeWindow.minimize()
                }
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(
                LocalContentColor provides if (connected) {
                    Color(0xFF09FE75)
                } else {
                    Color.White
                }
            ) {
                Text(
                    text = if (connected) {
                        stringResource(R.string.text_power_connect)
                    } else {
                        stringResource(R.string.text_power_disconnect)
                    },
                    style = MaterialTheme.typography.labelLarge
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = String.format("%.0f%s", batteryPercentageState, "%"),
                        style = MaterialTheme.typography.labelLarge
                    )

                    Icon(
                        painter = if (connected) {
                            painterResource(R.drawable.ic_power_connect)
                        } else {
                            painterResource(R.drawable.ic_power_disconnect)
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}