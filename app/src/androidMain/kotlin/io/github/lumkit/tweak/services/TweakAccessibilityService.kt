package io.github.lumkit.tweak.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.startSmartNotice
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.stopSmartNotice
import kotlinx.coroutines.runBlocking

class TweakAccessibilityService: AccessibilityService() {

    companion object {
        var windowManager: WindowManager? = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        println(event)
    }

    override fun onInterrupt() {
        windowManager = null
    }

    override fun onServiceConnected() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_VIEW_FOCUSED
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