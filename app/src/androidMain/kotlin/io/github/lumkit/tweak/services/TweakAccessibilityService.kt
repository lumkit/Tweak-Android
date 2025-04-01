package io.github.lumkit.tweak.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.lumkit.tweak.ui.screen.notice.model.ChargePlugin
import io.github.lumkit.tweak.ui.screen.notice.model.MusicPlugin
import io.github.lumkit.tweak.ui.screen.notice.model.ScreenUnlockPlugin
import io.github.lumkit.tweak.ui.screen.notice.model.VolumePlugin
import io.github.lumkit.tweak.ui.view.SmartNoticeFactory

class TweakAccessibilityService : AccessibilityService() {

    companion object {
        var isRunning by mutableStateOf(false)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

    }

    private var smartNoticeFactory: SmartNoticeFactory? = null

    override fun onInterrupt() {
        isRunning = false
        smartNoticeFactory?.apply {
            hide()
            SmartNoticeFactory.removeAllPlugins()
            unregister()
        }
        smartNoticeFactory?.apply {
            this.onStop()
            this.onDestroy()
        }
        smartNoticeFactory = null
    }

    override fun onDestroy() {
        onInterrupt()
        super.onDestroy()
    }

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_VIEW_FOCUSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }

        isRunning = true

        smartNoticeFactory = SmartNoticeFactory(
            context = this,
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        ).apply {
            register()
            this.onCreate()
            this.onStart()
            this.onResume()

            SmartNoticeFactory.installPlugins(
                mapOf(
                    // 充电消息插件
                    ChargePlugin::class to ChargePlugin(this),
                    // 屏幕解锁插件
                    ScreenUnlockPlugin::class to ScreenUnlockPlugin(this),
                    // 响铃模式插件
                    VolumePlugin::class to VolumePlugin(this),
                    // 音乐插件
                    MusicPlugin::class to MusicPlugin(this),
                )
            )
        }
    }
}