package io.github.lumkit.tweak.ui.view

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.cardview.widget.CardView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntSize
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.os.postDelayed
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.common.status.TweakException
import io.github.lumkit.tweak.common.util.getDiveSize
import io.github.lumkit.tweak.common.util.makeText
import io.github.lumkit.tweak.data.CutoutRect
import io.github.lumkit.tweak.data.SmartNoticeData
import io.github.lumkit.tweak.data.SmartNoticeGravity
import io.github.lumkit.tweak.data.SmartNoticeInterpolator
import io.github.lumkit.tweak.data.SmartNoticeRunningState
import io.github.lumkit.tweak.data.asInterpolator
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.local.json
import io.github.lumkit.tweak.ui.screen.ScreenRoute
import io.github.lumkit.tweak.ui.screen.notice.model.ChargePlugin
import io.github.lumkit.tweak.ui.screen.notice.model.MusicPlugin
import io.github.lumkit.tweak.ui.screen.notice.model.SmartNoticeNotificationPlugin
import io.github.lumkit.tweak.ui.token.SmartNoticeCapsuleDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 灵动岛工厂，需要通过无障碍服务启动
 */
class SmartNoticeFactory(
    val context: AccessibilityService,
    private val windowManager: WindowManager,
) {

    companion object {
        const val SMART_NOTICE_CHANNEL_ID = "Tweak Smart Notice"
        const val SMART_NOTICE_CHANNEL_NAME = "Smart Notice"

        var runningState by mutableStateOf(SmartNoticeRunningState.OFFLINE)
        var gameModeState by mutableStateOf(false)
        var screenState by mutableStateOf(true)

        private const val ACTION_SMART_NOTICE_ACCESSIBILITY_SERVICE =
            "ACTION_SMART_NOTICE_ACCESSIBILITY_SERVICE"

        private const val ACTION_SMART_NOTICE_SHOW = "ACTION_SMART_NOTICE_SHOW"
        private const val ACTION_SMART_NOTICE_HIDE = "ACTION_SMART_NOTICE_HIDE"
        private const val ACTION_NOTIFICATION_STATUS_SHOW = "ACTION_NOTIFICATION_STATUS_SHOW"
        private const val ACTION_NOTIFICATION_STATUS_HIDE = "ACTION_NOTIFICATION_STATUS_HIDE"
        private const val ACTION_GAME_MODE_ENABLED = "ACTION_GAME_MODE_ENABLED"
        private const val ACTION_GAME_MODE_DISABLED = "ACTION_GAME_MODE_DISABLED"

        private const val ACTION_IMPORT_PROPERTIES = "ACTION_IMPORT_PROPERTIES"
        private const val ACTION_SAVE_PROPERTIES = "ACTION_SAVE_PROPERTIES"
        private const val ACTION_IMITATE_CHARGE = "ACTION_IMITATE_CHARGE"
        private const val ACTION_UPDATE_DISPLAY_MODE = "ACTION_UPDATE_DISPLAY_MODE"

        private val density: Density = TweakApplication.density
        val smartNoticeDataDefault: SmartNoticeData
            get() = with(density) {
                SmartNoticeData(
                    width = SmartNoticeCapsuleDefault.CapsuleWidth.roundToPx(),
                    height = SmartNoticeCapsuleDefault.CapsuleHeight.roundToPx(),
                    gravity = SmartNoticeGravity.Center,
                    x = 0,
                    y = 11.dp.roundToPx(),
                    radius = 24.dp.toPx(),
                    duration = 400,
                    delay = 5000,
                    interpolator = SmartNoticeInterpolator.Overshoot
                )
            }

        private val _plugins = MutableStateFlow<Map<Any, SmartNoticeNotificationPlugin>>(mapOf())
        val observerPluginMap = _plugins.asStateFlow()

        fun installPlugins(
            map: Map<Any, SmartNoticeNotificationPlugin>
        ) {
            val old = _plugins.value
            val new = mutableMapOf<Any, SmartNoticeNotificationPlugin>()
            new.putAll(old)
            new.putAll(map)
            _plugins.value = new
        }

        fun removeAllPlugins() {
            _plugins.value.forEach { (_, plugin) ->
                plugin.onDestroy()
            }
            _plugins.value = mapOf()
        }

        val globalSmartNoticeData = MutableStateFlow(smartNoticeDataDefault)

        fun show(context: Context) {
            val intent = Intent(ACTION_SMART_NOTICE_ACCESSIBILITY_SERVICE)
            intent.setPackage(context.packageName)
            intent.putExtra("action", ACTION_SMART_NOTICE_SHOW)
            context.sendBroadcast(intent)
        }

        fun hide(context: Context) {
            val intent = Intent(ACTION_SMART_NOTICE_ACCESSIBILITY_SERVICE)
            intent.`package` = context.packageName
            intent.putExtra("action", ACTION_SMART_NOTICE_HIDE)
            context.sendBroadcast(intent)
        }

        fun showStatusNotification(context: Context) {
            val intent = Intent(ACTION_SMART_NOTICE_ACCESSIBILITY_SERVICE)
            intent.`package` = context.packageName
            intent.putExtra("action", ACTION_NOTIFICATION_STATUS_SHOW)
            context.sendBroadcast(intent)
        }

        fun hideStatusNotification(context: Context) {
            val intent = Intent(ACTION_SMART_NOTICE_ACCESSIBILITY_SERVICE)
            intent.`package` = context.packageName
            intent.putExtra("action", ACTION_NOTIFICATION_STATUS_HIDE)
            context.sendBroadcast(intent)
        }

        fun enableGameMode(context: Context) {
            val intent = Intent(ACTION_SMART_NOTICE_ACCESSIBILITY_SERVICE)
            intent.`package` = context.packageName
            intent.putExtra("action", ACTION_GAME_MODE_ENABLED)
            context.sendBroadcast(intent)
        }

        fun disableGameMode(context: Context) {
            val intent = Intent(ACTION_SMART_NOTICE_ACCESSIBILITY_SERVICE)
            intent.`package` = context.packageName
            intent.putExtra("action", ACTION_GAME_MODE_DISABLED)
            context.sendBroadcast(intent)
        }

        fun saveData(context: Context) {
            val intent = Intent(ACTION_SMART_NOTICE_ACCESSIBILITY_SERVICE)
            intent.`package` = context.packageName
            intent.putExtra("action", ACTION_SAVE_PROPERTIES)
            context.sendBroadcast(intent)
        }

        fun importProperties(context: Context, smartNoticeData: SmartNoticeData?) {
            val intent = Intent(ACTION_SMART_NOTICE_ACCESSIBILITY_SERVICE)
            intent.`package` = context.packageName
            intent.putExtra("action", ACTION_IMPORT_PROPERTIES)
            intent.putExtra("jsonText", json.encodeToString(smartNoticeData))
            context.sendBroadcast(intent)
        }

        fun imitateCharge(context: Context, state: Boolean) {
            val intent = Intent(ACTION_SMART_NOTICE_ACCESSIBILITY_SERVICE)
            intent.`package` = context.packageName
            intent.putExtra("action", ACTION_IMITATE_CHARGE)
            intent.putExtra("state", state)
            context.sendBroadcast(intent)
        }

        fun updateDisplayMode(context: Context, state: Boolean) {
            val intent = Intent(ACTION_SMART_NOTICE_ACCESSIBILITY_SERVICE)
            intent.`package` = context.packageName
            intent.putExtra("action", ACTION_UPDATE_DISPLAY_MODE)
            intent.putExtra("state", state)
            context.sendBroadcast(intent)
        }
    }

    private var notificationManager: NotificationManager? = null

    private val interactivelyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.`package` == TweakApplication.application.packageName && intent?.action == ACTION_SMART_NOTICE_ACCESSIBILITY_SERVICE) {
                val action = intent.getStringExtra("action")
                when (action) {
                    ACTION_SMART_NOTICE_SHOW -> {
                        runningState = SmartNoticeRunningState.ONLINE
                        val gameMode = TweakApplication.shared.getBoolean(
                            Const.SmartNotice.SMART_NOTICE_GAME_MODE,
                            true
                        )
                        if (gameMode) {
                            gameMode()
                        } else {
                            show()
                        }
                    }

                    ACTION_SMART_NOTICE_HIDE -> {
                        hide()
                        runningState = SmartNoticeRunningState.OFFLINE
                    }

                    ACTION_NOTIFICATION_STATUS_SHOW -> {
                        showStatusNotification()
                    }

                    ACTION_NOTIFICATION_STATUS_HIDE -> {
                        hideStatusNotification()
                    }

                    ACTION_GAME_MODE_ENABLED -> {
                        if (runningState == SmartNoticeRunningState.ONLINE) {
                            gameMode()
                        }
                    }

                    ACTION_GAME_MODE_DISABLED -> {
                        if (runningState == SmartNoticeRunningState.ONLINE) {
                            show()
                        }
                    }

                    ACTION_SAVE_PROPERTIES -> {
                        saveData()
                    }

                    ACTION_IMPORT_PROPERTIES -> {
                        val jsonText = intent.getStringExtra("jsonText").toString()
                        try {
                            val smartNoticeData = json.decodeFromString<SmartNoticeData>(jsonText)
                            applyData(smartNoticeData)
                        } catch (e: Exception) {
                            Toast.makeText(context, e.makeText(), Toast.LENGTH_SHORT).show()
                        }
                    }

                    ACTION_IMITATE_CHARGE -> {
                        if (runningState == SmartNoticeRunningState.ONLINE) {
                            _plugins.value[ChargePlugin::class]?.display(
                                intent.getBooleanExtra("state", false)
                            )
                        }
                    }

                    ACTION_UPDATE_DISPLAY_MODE -> {
                        if (runningState == SmartNoticeRunningState.ONLINE) {
                            if (!intent.getBooleanExtra("state", true)) {
                                minimize()
                            } else {
                                show()
                            }
                        }
                    }
                }
            }
        }
    }

    private val sharedPreferences: SharedPreferences = TweakApplication.shared
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val noticeLayoutParams: LayoutParams =
        LayoutParams().apply {
            format = PixelFormat.TRANSLUCENT

            flags = LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    LayoutParams.FLAG_NOT_FOCUSABLE or
                    LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    LayoutParams.FLAG_LAYOUT_NO_LIMITS

            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                LayoutParams.TYPE_SYSTEM_ALERT
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }

            windowAnimations = 0

            // 移除Android私人动画
            try {
                val paramsClass = LayoutParams::class.java
                val field = paramsClass.getField("privateFlags")
                field.isAccessible = true
                val currentFlags = field.get(this) as Int
                field.set(this, currentFlags or 0x00000040)
                field.isAccessible = false
            } catch (_: Exception) {
            }
        }

    private var animatorContainerParams = RelativeLayout.LayoutParams(0, 0).apply {
        addRule(RelativeLayout.ALIGN_PARENT_TOP)
        addRule(RelativeLayout.CENTER_HORIZONTAL)
    }

    private val cutoutList: List<CutoutRect>
        get() {
            val jsonText = TweakApplication.shared.getString(
                Const.SmartNotice.SMART_NOTICE_CUTOUT_RECT_LIST,
                null
            ) ?: "[]"
            return json.decodeFromString(jsonText)
        }

    init {
        initData()
        launchState()
    }

    fun register() {
        val filter = IntentFilter(ACTION_SMART_NOTICE_ACCESSIBILITY_SERVICE)
        ContextCompat.registerReceiver(
            context,
            interactivelyReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                SMART_NOTICE_CHANNEL_ID,
                SMART_NOTICE_CHANNEL_NAME,
                importance
            ).apply {
                description =
                    context.getString(R.string.text_smart_notice_notification_channel_description)
            }
            // 注册通知渠道
            notificationManager?.createNotificationChannel(channel)
        }

        // 游戏模式广播
        val gameModeFilter = IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED)
        context.registerReceiver(orientationChangeReceiver, gameModeFilter)

        // 息屏广播
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        context.registerReceiver(screenStateReceiver, screenFilter)

        val start = TweakApplication.shared.getBoolean(
            Const.SmartNotice.SMART_NOTICE_SWITCH,
            false
        )

        runningState = if (start) {
            SmartNoticeRunningState.ONLINE
        } else {
            SmartNoticeRunningState.OFFLINE
        }

        if (runningState == SmartNoticeRunningState.ONLINE) {
            val gameMode = TweakApplication.shared.getBoolean(
                Const.SmartNotice.SMART_NOTICE_GAME_MODE,
                true
            )
            gameModeState = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && gameMode
            if (gameMode) {
                gameMode()
            } else {
                show()
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
        val notification = NotificationCompat.Builder(context, SMART_NOTICE_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(icon)
            .setAutoCancel(dismiss)
            .setOngoing(!dismiss)
            .setContentIntent(contentIntent)
            .build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun showStatusNotification() {
        val deepLinkUri = "${Const.Navigation.DEEP_LINE}/${ScreenRoute.SMART_NOTICE}".toUri()
        val startIntent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notify(
            id = 301,
            title = context.getString(R.string.text_smart_notice_notification_title),
            content = context.getString(R.string.text_smart_notice_notification_subtitle),
            icon = R.mipmap.ic_tweak_logo,
            dismiss = false,
            contentIntent = pendingIntent
        )
    }

    private fun hideStatusNotification() {
        NotificationManagerCompat.from(context).cancel(301)
    }

    fun unregister() {
        runningState = SmartNoticeRunningState.OFFLINE
        ioScope.cancel()
        context.unregisterReceiver(interactivelyReceiver)
        context.unregisterReceiver(orientationChangeReceiver)
        context.unregisterReceiver(screenStateReceiver)
        saveData()
    }

    fun saveData() {
        val jsonText = json.encodeToString(globalSmartNoticeData.value)
        sharedPreferences.edit {
            putString(
                Const.SmartNotice.SMART_NOTICE_LOCAL_DATA,
                jsonText
            )
        }
    }

    private fun gameMode() {
        val newConfig: Configuration = context.resources.configuration
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            hide()
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            show()
        }
    }

    /**
     * 游戏模式广播
     */
    private val orientationChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val gameMode = TweakApplication.shared.getBoolean(
                Const.SmartNotice.SMART_NOTICE_GAME_MODE,
                true
            )
            gameModeState = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && gameMode
            if (gameMode && runningState == SmartNoticeRunningState.ONLINE) {
                gameMode()
            }
        }
    }

    /**
     * 息屏广播
     */
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenState = false
                    val lockedShow = sharedPreferences.getBoolean(
                        Const.SmartNotice.SMART_NOTICE_SCREEN_LOCKED_ALWAYS_SHOW,
                        false
                    )
                    if (!lockedShow && runningState == SmartNoticeRunningState.ONLINE) {
                        hide()
                    }
                }

                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_UNLOCKED, Intent.ACTION_USER_PRESENT -> {
                    screenState = true
                    val displayMode = sharedPreferences.getBoolean(
                        Const.SmartNotice.SMART_NOTICE_ALWAYS_SHOW,
                        true
                    )
                    if (runningState == SmartNoticeRunningState.ONLINE && displayMode) {
                        show()
                    }
                }
            }
        }
    }

    private fun launchState() {
        ioScope.launch {
            globalSmartNoticeData.collect {
                noticeLayoutParams.width = it.width
                noticeLayoutParams.height = it.height
                animatorContainerParams.width = it.width
                animatorContainerParams.height = it.height
                noticeLayoutParams.x = it.x
                noticeLayoutParams.y = it.y
                noticeLayoutParams.gravity = it.gravity.gravity or Gravity.TOP

                withContext(Dispatchers.Main) {
                    animatorContainer.radius = it.radius
                    update()
                    updateAnimatorContainer()
                }
            }
        }
    }

    private val container = FrameLayout(context).apply {
        alpha = 0f
    }

    private val animatorContainer = CardView(context).apply {
        alpha = 0f
        cardElevation = 0f
        radius = globalSmartNoticeData.value.radius
        setCardBackgroundColor(Color.BLACK)
        this.addView(container, FrameLayout.LayoutParams(-1, -1))
    }

    private val rootView = RelativeLayout(context).apply {
        this.addView(animatorContainer, animatorContainerParams)
    }

    private fun update() {
        try {
            windowManager.updateViewLayout(rootView, noticeLayoutParams)
        } catch (_: Exception) {
        }
    }

    private fun updateAnimatorContainer() {
        animatorContainer.layoutParams = animatorContainerParams
        animatorContainer.requestLayout()
    }

    private fun addView(view: View, params: FrameLayout.LayoutParams) {
        try {
            container.removeAllViews()
            container.addView(view, params)
        } catch (_: Exception) {
        }
    }

    fun removeView(view: View) {
        try {
            container.removeView(view)
        } catch (_: Exception) {
        }
    }

    val handler = Handler(Looper.getMainLooper())
    private var animatorContainerWidthAnimator: ValueAnimator? = null
    private var animatorContainerHeightAnimator: ValueAnimator? = null
    private var animatorContainerAlphaAnimator: ObjectAnimator? = null
    private var rootViewOffsetXAnimator: ValueAnimator? = null
    private var rootViewOffsetYAnimator: ValueAnimator? = null
    private var animatorContainerRadiusAnimator: ObjectAnimator? = null
    private var containerAlphaAnimator: ObjectAnimator? = null

    private fun initData() {
        val localJsonText = sharedPreferences.getString(
            Const.SmartNotice.SMART_NOTICE_LOCAL_DATA,
            null
        )

        val localData = try {
            json.decodeFromString<SmartNoticeData>(
                localJsonText ?: throw TweakException("没有本地数据")
            )
        } catch (_: Exception) {
            smartNoticeDataDefault
        }

        applyData(localData)

        initState()
    }

    private fun applyData(data: SmartNoticeData) {
        globalSmartNoticeData.value = data
        try {
            windowManager.addView(rootView, noticeLayoutParams)
        } catch (_: Exception) {
        }
    }

    /**
     * 初始化所有属性
     */
    private fun initState() {
        if (TweakApplication.shared.getBoolean(Const.SmartNotice.SMART_NOTICE_NOTIFICATION, true)) {
            showStatusNotification()
        }
    }

    private fun cancelAnimators(vararg animator: Animator?) {
        for (item in animator) {
            if (item?.isRunning == true) {
                item.removeAllListeners()
                item.cancel()
            }
        }
    }

    private fun startAnimatorContainerWidthAnimation(
        initValue: Int,
        targetValue: Int,
        delay: Long? = null,
    ) {
        animatorContainerWidthAnimator = ValueAnimator.ofInt(initValue, targetValue)?.apply {
            interpolator = globalSmartNoticeData.value.interpolator.asInterpolator()
            duration = globalSmartNoticeData.value.duration
            addUpdateListener {
                animatorContainerParams.width = it.animatedValue as Int
                updateAnimatorContainer()
            }
            delay?.let(::setStartDelay)
            start()
        }
    }

    private fun startAnimatorContainerHeightAnimation(
        initValue: Int,
        targetValue: Int,
        delay: Long? = null,
    ) {
        animatorContainerHeightAnimator = ValueAnimator.ofInt(initValue, targetValue)?.apply {
            interpolator = globalSmartNoticeData.value.interpolator.asInterpolator()
            duration = globalSmartNoticeData.value.duration
            addUpdateListener {
                animatorContainerParams.height = it.animatedValue as Int
                updateAnimatorContainer()
            }
            delay?.let(::setStartDelay)
            start()
        }
    }

    private fun startAnimatorContainerAlphaAnimation(
        initValue: Float,
        targetValue: Float,
        delay: Long? = null,
    ) {
        animatorContainerAlphaAnimator = ObjectAnimator.ofFloat(
            animatorContainer,
            "alpha",
            initValue,
            targetValue
        ).apply {
            duration = globalSmartNoticeData.value.duration
            delay?.let(::setStartDelay)
            start()
        }
    }

    private fun startContainerAlphaAnimation(
        initValue: Float,
        targetValue: Float,
        delay: Long? = null,
    ) {
        containerAlphaAnimator = ObjectAnimator.ofFloat(
            container,
            "alpha",
            initValue,
            targetValue
        ).apply {
            duration = globalSmartNoticeData.value.duration / 2
            delay?.let(::setStartDelay)
            start()
        }
    }

    private fun startOffsetXAnimation(
        initValue: Int,
        targetValue: Int,
        delay: Long? = null,
    ) {
        rootViewOffsetXAnimator = ValueAnimator.ofInt(initValue, targetValue).apply {
            interpolator = globalSmartNoticeData.value.interpolator.asInterpolator()
            duration = globalSmartNoticeData.value.duration
            addUpdateListener {
                noticeLayoutParams.x = it.animatedValue as Int
                update()
            }
            delay?.let(::setStartDelay)
            start()
        }
    }

    private fun startOffsetYAnimation(
        initValue: Int,
        targetValue: Int,
        delay: Long? = null,
    ) {
        rootViewOffsetYAnimator = ValueAnimator.ofInt(initValue, targetValue).apply {
            interpolator = globalSmartNoticeData.value.interpolator.asInterpolator()
            duration = globalSmartNoticeData.value.duration
            addUpdateListener {
                noticeLayoutParams.y = it.animatedValue as Int
                update()
            }
            delay?.let(::setStartDelay)
            start()
        }
    }

    private fun startAnimatorContainerRadiusAnimation(
        initValue: Float,
        targetValue: Float,
        delay: Long? = null,
    ) {
        animatorContainerRadiusAnimator = ObjectAnimator.ofFloat(
            animatorContainer,
            "radius",
            initValue,
            targetValue
        ).apply {
            duration = globalSmartNoticeData.value.duration
            delay?.let(::setStartDelay)
            start()
        }
    }

    private fun show() {
        try {
            container.removeAllViews()
        } catch (_: Exception) {
        }
        noticeLayoutParams.width = globalSmartNoticeData.value.width
        noticeLayoutParams.height = globalSmartNoticeData.value.height
        try {
            windowManager.addView(rootView, noticeLayoutParams)
        } catch (_: Exception) {
        }
        update()

        animatorContainerParams = RelativeLayout.LayoutParams(
            animatorContainerParams.width,
            animatorContainerParams.height,
        ).apply {
            addRule(RelativeLayout.CENTER_IN_PARENT)
        }
        updateAnimatorContainer()
        // 先取消动画
        cancelAnimators(
            animatorContainerWidthAnimator,
            animatorContainerHeightAnimator,
            animatorContainerAlphaAnimator,
            rootViewOffsetXAnimator,
            rootViewOffsetYAnimator
        )

        startAnimatorContainerWidthAnimation(
            animatorContainerParams.width,
            globalSmartNoticeData.value.width
        )
        startAnimatorContainerHeightAnimation(
            animatorContainerParams.height,
            globalSmartNoticeData.value.height
        )
        startAnimatorContainerAlphaAnimation(animatorContainer.alpha, 1f)
        startOffsetXAnimation(
            noticeLayoutParams.x,
            globalSmartNoticeData.value.x
        )
        startOffsetYAnimation(
            noticeLayoutParams.y,
            globalSmartNoticeData.value.y
        )
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(
            {
                minimize()
            },
            2000
        )
    }

    fun hide() {
        try {
            container.removeAllViews()
        } catch (_: Exception) {
        }
        animatorContainerParams = RelativeLayout.LayoutParams(
            animatorContainerParams.width,
            animatorContainerParams.height,
        ).apply {
            addRule(RelativeLayout.CENTER_IN_PARENT)
        }
        updateAnimatorContainer()
        // 先取消动画
        cancelAnimators(
            animatorContainerWidthAnimator,
            animatorContainerHeightAnimator,
            animatorContainerAlphaAnimator,
            rootViewOffsetXAnimator,
            rootViewOffsetYAnimator
        )

        startAnimatorContainerWidthAnimation(animatorContainerParams.width, 0)
        startAnimatorContainerHeightAnimation(animatorContainerParams.height, 0)
        startAnimatorContainerAlphaAnimation(animatorContainer.alpha, 0f)
        startOffsetXAnimation(
            noticeLayoutParams.x,
            globalSmartNoticeData.value.x
        )
        startOffsetYAnimation(
            noticeLayoutParams.y,
            globalSmartNoticeData.value.y
        )

        animatorContainerAlphaAnimator?.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    try {
                        windowManager.removeView(rootView)
                    } catch (_: Exception) {
                    }
                }
            }
        )
    }

    var expanded = false
    var taskTag: String? = null
        private set
    private val deviceSize = context.getDiveSize()

    /**
     * 弹出组件
     */
    fun toast(
        view: View,
        params: FrameLayout.LayoutParams = FrameLayout.LayoutParams(-1, -1),
        contentSize: (Context, Density, Size, List<CutoutRect>) -> DpSize,
        offset: (Context, Density, Size, List<CutoutRect>) -> DpOffset = { _, _, _, _ -> DpOffset.Zero },
        radius: (Context, Density, Size, List<CutoutRect>) -> Float = { _, _, _, _ -> globalSmartNoticeData.value.radius },
        autoMinimize: Boolean = true,
        delay: Long = globalSmartNoticeData.value.delay,
    ) {
        // 离线时不显示
        if (runningState == SmartNoticeRunningState.OFFLINE || gameModeState || !screenState) {
            return
        }

        if (expanded) {
            return
        }

        try {
            windowManager.addView(rootView, noticeLayoutParams)
        } catch (_: Exception) {
        }

        val size =
            with(density) {
                contentSize(context, this, deviceSize, cutoutList).toSize().roundToIntSize()
            }
        val intOffset = with(density) {
            val offsetDp = offset(context, this, deviceSize, cutoutList)
            IntOffset(offsetDp.x.roundToPx(), offsetDp.y.roundToPx())
        }

        animatorContainerParams = RelativeLayout.LayoutParams(
            animatorContainerParams.width,
            animatorContainerParams.height,
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_TOP)
            addRule(RelativeLayout.CENTER_HORIZONTAL)
        }
        updateAnimatorContainer()
        val targetWidth =
            (size.width * if (globalSmartNoticeData.value.interpolator == SmartNoticeInterpolator.Overshoot) 1.3f else 1f).roundToInt()
        val targetHeight =
            (size.height * if (globalSmartNoticeData.value.interpolator == SmartNoticeInterpolator.Overshoot) 1.3f else 1f).roundToInt()
        if (targetWidth > noticeLayoutParams.width) {
            noticeLayoutParams.width = targetWidth
        }
        if (targetHeight > noticeLayoutParams.height) {
            noticeLayoutParams.height = targetHeight
        }
        update()

        addView(view, params)

        cancelAnimators(
            animatorContainerWidthAnimator,
            animatorContainerHeightAnimator,
            animatorContainerRadiusAnimator,
            animatorContainerAlphaAnimator,
            containerAlphaAnimator,
            rootViewOffsetXAnimator,
            rootViewOffsetYAnimator,
        )

        startAnimatorContainerWidthAnimation(
            animatorContainerParams.width,
            size.width
        )

        startAnimatorContainerHeightAnimation(
            animatorContainerParams.height,
            size.height,
        )

        startContainerAlphaAnimation(
            container.alpha,
            1f,
            globalSmartNoticeData.value.duration / 2
        )

        startAnimatorContainerRadiusAnimation(
            animatorContainer.radius,
            radius(context, density, deviceSize, cutoutList),
        )

        startAnimatorContainerAlphaAnimation(
            animatorContainer.alpha,
            1f
        )

        if (intOffset.x > 0) {
            startOffsetXAnimation(
                noticeLayoutParams.x,
                intOffset.x
            )
        } else {
            startOffsetXAnimation(
                noticeLayoutParams.x,
                globalSmartNoticeData.value.x
            )
        }

        if (intOffset.y > 0) {
            startOffsetYAnimation(
                noticeLayoutParams.y,
                intOffset.y
            )
        } else {
            startOffsetYAnimation(
                noticeLayoutParams.y,
                globalSmartNoticeData.value.y
            )
        }

        animatorContainerWidthAnimator?.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    noticeLayoutParams.width = targetWidth
                    noticeLayoutParams.height = targetHeight
                }
            }
        )

        val display = sharedPreferences.getBoolean(
            Const.SmartNotice.SMART_NOTICE_ALWAYS_SHOW,
            true
        )

        if (isMediaComponent && display) {
            handler.removeCallbacksAndMessages(null)
        }

        // 未开启常显模式则自动最小化
        if (autoMinimize || !display) {
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed(
                {
                    minimize(view)
                },
                delay
            )
        }
    }

    /**
     * @param minimize 强制折叠
     */
    fun minimize(
        view: View? = null,
        minimize: Boolean = false,
        display: Boolean = sharedPreferences.getBoolean(
            Const.SmartNotice.SMART_NOTICE_ALWAYS_SHOW,
            true
        )
    ) {
        taskTag = null
        // 离线时不显示
        if (runningState == SmartNoticeRunningState.OFFLINE || gameModeState || !screenState) {
            return
        }

        // 展开时不折叠
        if (expanded && !minimize) {
            return
        }

        if (!display) {
            hide()
            return
        }

        if (isMediaComponent && !minimize) {
            val plugin = _plugins.value[MusicPlugin::class] as MusicPlugin?
            if (plugin != null && plugin.enableState.value && isMediaComponent) {
                showMedia(plugin.minimizeBinding.root)
            }
            return
        }

        expanded = false
        taskTag = null

        cancelAnimators(
            animatorContainerWidthAnimator,
            animatorContainerHeightAnimator,
            animatorContainerRadiusAnimator,
            containerAlphaAnimator,
            rootViewOffsetXAnimator,
            rootViewOffsetYAnimator,
        )

        startAnimatorContainerWidthAnimation(
            animatorContainerParams.width,
            globalSmartNoticeData.value.width
        )

        startAnimatorContainerHeightAnimation(
            animatorContainerParams.height,
            globalSmartNoticeData.value.height
        )

        startContainerAlphaAnimation(
            container.alpha,
            0f
        )

        startAnimatorContainerRadiusAnimation(
            animatorContainer.radius,
            globalSmartNoticeData.value.radius,
        )

        if (abs(noticeLayoutParams.x - globalSmartNoticeData.value.x) > 0) {
            startOffsetXAnimation(
                noticeLayoutParams.x,
                globalSmartNoticeData.value.x
            )
        }

        if (abs(noticeLayoutParams.y - globalSmartNoticeData.value.y) > 0) {
            startOffsetYAnimation(
                noticeLayoutParams.y,
                globalSmartNoticeData.value.y
            )
        }

        animatorContainerHeightAnimator?.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    view?.also(::removeView)
                    try {
                        container.removeAllViews()
                    } catch (_: Exception) {
                    }
                    noticeLayoutParams.width = if (display) globalSmartNoticeData.value.width else 0
                    noticeLayoutParams.height =
                        if (display) globalSmartNoticeData.value.height else 0
                    update()
                }
            }
        )
    }

    var isMediaComponent = false

    fun showMedia(
        view: View,
        params: FrameLayout.LayoutParams = FrameLayout.LayoutParams(-1, -1),
        offset: (Context, Density, Size, List<CutoutRect>) -> DpOffset = { _, _, _, _ -> DpOffset.Zero },
        radius: (Context, Density, Size, List<CutoutRect>) -> Float = { _, _, _, _ -> globalSmartNoticeData.value.radius },
        contentSize: (Context, Density, Size, List<CutoutRect>) -> DpSize = { _, _, _, _ ->
            DpSize(
                160.dp,
                32.dp
            )
        },
        delay: Long = globalSmartNoticeData.value.delay,
    ) {
        // 离线时不显示
        if (runningState == SmartNoticeRunningState.OFFLINE || gameModeState || !screenState) {
            return
        }

        if (expanded) {
            return
        }

        isMediaComponent = true

        try {
            windowManager.addView(rootView, noticeLayoutParams)
        } catch (_: Exception) {
        }

        val size = with(density) {
            contentSize(context, this, deviceSize, cutoutList).toSize().roundToIntSize()
        }

        val intOffset = with(density) {
            val offsetDp = offset(context, this, deviceSize, cutoutList)
            IntOffset(offsetDp.x.roundToPx(), offsetDp.y.roundToPx())
        }

        animatorContainerParams = RelativeLayout.LayoutParams(
            animatorContainerParams.width,
            animatorContainerParams.height,
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_TOP)
            addRule(RelativeLayout.CENTER_HORIZONTAL)
        }
        updateAnimatorContainer()
        val targetWidth =
            (size.width * if (globalSmartNoticeData.value.interpolator == SmartNoticeInterpolator.Overshoot) 1.3f else 1f).roundToInt()
        val targetHeight =
            (size.height * if (globalSmartNoticeData.value.interpolator == SmartNoticeInterpolator.Overshoot) 1.3f else 1f).roundToInt()
        if (targetWidth > noticeLayoutParams.width) {
            noticeLayoutParams.width = targetWidth
        }
        if (targetHeight > noticeLayoutParams.height) {
            noticeLayoutParams.height = targetHeight
        }
        update()

        addView(view, params)

        cancelAnimators(
            animatorContainerWidthAnimator,
            animatorContainerHeightAnimator,
            animatorContainerRadiusAnimator,
            animatorContainerAlphaAnimator,
            containerAlphaAnimator,
            rootViewOffsetXAnimator,
            rootViewOffsetYAnimator,
        )

        startAnimatorContainerWidthAnimation(
            animatorContainerParams.width,
            size.width
        )

        startAnimatorContainerHeightAnimation(
            animatorContainerParams.height,
            size.height,
        )

        startContainerAlphaAnimation(
            container.alpha,
            1f,
            globalSmartNoticeData.value.duration / 2
        )

        startAnimatorContainerRadiusAnimation(
            animatorContainer.radius,
            radius(context, density, deviceSize, cutoutList),
        )

        startAnimatorContainerAlphaAnimation(
            animatorContainer.alpha,
            1f
        )

        startOffsetXAnimation(
            noticeLayoutParams.x,
            intOffset.x + globalSmartNoticeData.value.x
        )

        startOffsetYAnimation(
            noticeLayoutParams.y,
            intOffset.y + globalSmartNoticeData.value.y
        )

        animatorContainerWidthAnimator?.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    noticeLayoutParams.width = targetWidth
                    noticeLayoutParams.height = targetHeight
                }
            }
        )

        val display = sharedPreferences.getBoolean(
            Const.SmartNotice.SMART_NOTICE_ALWAYS_SHOW,
            true
        )

        if (isMediaComponent && display) {
            handler.removeCallbacksAndMessages(null)
        }

        // 未开启常显模式则自动最小化
        if (!expanded && !display) {
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed(
                {
                    minimize(view)
                },
                delay
            )
        }
    }
}