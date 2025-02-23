package io.github.lumkit.tweak.services

import android.Manifest
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
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.lifecycle.ComposeViewLifecycleOwner
import io.github.lumkit.tweak.ui.screen.ScreenRoute
import io.github.lumkit.tweak.ui.view.SmartNoticeWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LocalSmartNoticeView = staticCompositionLocalOf<ComposeView> { error("not provided.") }
val LocalSmartNoticeWindowManager = staticCompositionLocalOf<WindowManager> { error("not provided.") }
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

        fun Context.startSmartNotice() {
            val intent = Intent(this, SmartNoticeService::class.java)
            intent.action = ACTION_START
            startService(intent)
        }

        suspend fun Context.stopSmartNotice() {
            val intent = Intent(this, SmartNoticeService::class.java)
            intent.action = ACTION_STOP
            startService(intent)
            delay(SmartNoticeWindow.duration)
            stopService(intent)
        }

        fun Context.showNotificationStatus(show: Boolean) {
            val intent = Intent(this, SmartNoticeService::class.java)
            intent.action =
                if (show) ACTION_NOTIFICATION_STATUS_SHOW else ACTION_NOTIFICATION_STATUS_HIDE
            startService(intent)
        }

        fun Context.enableGameMode() {
            val intent = Intent(this, SmartNoticeService::class.java)
            intent.action = ACTION_GAME_MODE_ENABLED
            startService(intent)
        }

        fun Context.disableGameMode() {
            val intent = Intent(this, SmartNoticeService::class.java)
            intent.action = ACTION_GAME_MODE_DISABLED
            startService(intent)
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
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

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

        val filter = IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED)
        registerReceiver(orientationChangeReceiver, filter)
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
        params.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//6.0+
            LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            LayoutParams.TYPE_SYSTEM_ALERT
        }

        params.format = PixelFormat.TRANSLUCENT
        params.width = LayoutParams.WRAP_CONTENT
        params.height = LayoutParams.WRAP_CONTENT
        params.gravity = Gravity.TOP or Gravity.CENTER
        params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL or
                LayoutParams.FLAG_NOT_FOCUSABLE or
                LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                LayoutParams.FLAG_DISMISS_KEYGUARD or
                LayoutParams.FLAG_KEEP_SCREEN_ON or
                LayoutParams.FLAG_TURN_SCREEN_ON or
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

        smartNoticeView?.show()
    }

    // 游戏模式广播
    private val orientationChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (TweakApplication.shared.getBoolean(Const.SmartNotice.SMART_NOTICE_GAME_MODE, true)) {
                gameModeChange()
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
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        try {
            windowManager?.removeView(smartNoticeView)
        } catch (_: Exception) { }
        windowManager = null
        smartNoticeLifecycleOwner?.onPause()
        smartNoticeLifecycleOwner?.onStop()
        smartNoticeLifecycleOwner?.onDestroy()
        smartNoticeLifecycleOwner = null
        smartNoticeView = null
        unregisterReceiver(orientationChangeReceiver)
        super.onDestroy()
    }
}