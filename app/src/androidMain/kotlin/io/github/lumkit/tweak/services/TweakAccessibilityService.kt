package io.github.lumkit.tweak.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.startSmartNotice
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.stopSmartNotice
import kotlinx.coroutines.runBlocking

class TweakAccessibilityService : AccessibilityService() {

    companion object {
        var windowManager: WindowManager? = null
        var mediaSessionManager: MediaSessionManager? = null
        var componentName: ComponentName? = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

    }

    override fun onInterrupt() {
        windowManager = null
        mediaSessionManager = null
    }

    override fun onServiceConnected() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        componentName = ComponentName(
            this@TweakAccessibilityService,
            SmartNoticeNotificationListenerService::class.java
        )
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        val info = AccessibilityServiceInfo()
        info.eventTypes =
            AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_VIEW_FOCUSED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info

        if (SmartNoticeService.isRunning()) {
            runBlocking {
                stopSmartNotice(true)
            }
            startSmartNotice()
        }
    }
}