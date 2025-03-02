package io.github.lumkit.tweak.services

import android.Manifest
import android.annotation.SuppressLint
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
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
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
import io.github.lumkit.tweak.ui.lifecycle.ComposeViewLifecycleOwner
import io.github.lumkit.tweak.ui.screen.ScreenRoute
import io.github.lumkit.tweak.ui.screen.notice.SmartNoticeViewModel
import io.github.lumkit.tweak.ui.token.SmartNoticeCapsuleDefault
import io.github.lumkit.tweak.ui.view.SmartNoticeWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

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

        suspend fun Context.restartSmartNotice() {
            stopSmartNotice(true)
            startSmartNotice()
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

            if (TweakApplication.shared.getBoolean(
                    Const.SmartNotice.SMART_NOTICE_GAME_MODE,
                    true
                )
            ) {
                gameModeChange()
            } else {
                smartNoticeView?.show()
            }
        }

        run {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(batteryReceiver, filter)
        }

        run {
            val filter = IntentFilter(Intent.ACTION_POWER_CONNECTED)
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
            registerReceiver(chargingReceiver, filter)
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
                LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                LayoutParams.FLAG_SHOW_WHEN_LOCKED

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
                context = this,
                windowManager = it,
                windowLayoutParams = params
            )
        }

        smartNoticeLifecycleOwner = ComposeViewLifecycleOwner().apply {
            onCreate()
            attachToDecorView(smartNoticeView)
        }

        windowManager?.addView(
            smartNoticeView,
            params,
        )

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
                smartNoticeView?.toast(
                    componentSize = {
                        val diveSize = getDiveSize()
                        DpSize(
                            width = with(density) {
                                diveSize.width.toDp() - 28.dp * 2f
                            },
                            height = 32.dp
                        )
                    }
                ) {
                    Charge(true, it)
                }
            }

            ACTION_POWER_DISCONNECTED -> {
                smartNoticeView?.toast(
                    componentSize = {
                        val diveSize = getDiveSize()
                        DpSize(
                            width = with(density) {
                                diveSize.width.toDp() - 28.dp * 2f
                            },
                            height = 32.dp
                        )
                    }
                ) {
                    Charge(false, it)
                }
            }

            ACTION_MARGIN_TOP -> {
                val offsetY = intent.getFloatExtra("y", 0f)
                try {
                    params.y = with(TweakApplication.density) {
                        offsetY.dp.roundToPx()
                    }
                    windowManager?.updateViewLayout(
                        smartNoticeView,
                        params,
                    )
                }catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            ACTION_MARGIN_START -> {
                val offsetX = intent.getFloatExtra("x", 0f)
                try {
                    params.x = with(TweakApplication.density) {
                        offsetX.dp.roundToPx()
                    }
                    windowManager?.updateViewLayout(
                        smartNoticeView,
                        params,
                    )
                }catch (e: Exception) {
                    e.printStackTrace()
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
                }catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            ACTION_CUTOUT_WIDTH -> {
                val width = intent.getFloatExtra("width", SmartNoticeCapsuleDefault.CapsuleWidth.value)
                try {
                    params.width = with(TweakApplication.density) {
                        width.dp.roundToPx()
                    }
                    windowManager?.updateViewLayout(
                        smartNoticeView,
                        params,
                    )
                }catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            ACTION_CUTOUT_HEIGHT -> {
                val height = intent.getFloatExtra("height", SmartNoticeCapsuleDefault.CapsuleHeight.value)
                try {
                    params.height = with(TweakApplication.density) {
                        height.dp.roundToPx()
                    }
                    windowManager?.updateViewLayout(
                        smartNoticeView,
                        params,
                    )
                }catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            ACTION_CUTOUT_RADIUS -> {
                with(TweakApplication.density) {
                    val radius = intent.getFloatExtra("radius", SmartNoticeCapsuleDefault.CapsuleHeight.value / 2f)
                    smartNoticeView?.radius = radius.dp.toPx()
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
        unregisterReceiver(orientationChangeReceiver)
        unregisterReceiver(batteryReceiver)
        unregisterReceiver(chargingReceiver)
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