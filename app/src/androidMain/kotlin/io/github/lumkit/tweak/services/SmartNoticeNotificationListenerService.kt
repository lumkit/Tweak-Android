package io.github.lumkit.tweak.services

import android.app.Notification
import android.graphics.Bitmap
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.services.SmartNoticeService.Companion.postNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SmartNoticeNotificationListenerService : NotificationListenerService() {

    private var noticeIoScope: CoroutineScope? = null
    private val json = Json {
        isLenient = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override fun onCreate() {
        super.onCreate()
        noticeIoScope = CoroutineScope(Dispatchers.IO)
    }

    override fun onDestroy() {
        super.onDestroy()
        noticeIoScope?.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        super.onNotificationPosted(sbn, rankingMap)
        noticeIoScope?.launch {
            val switch = TweakApplication.shared.getBoolean(
                Const.SmartNotice.SMART_NOTICE_SWITCH,
                false
            )
            val observe = TweakApplication.shared.getBoolean(
                Const.SmartNotice.Observe.SMART_NOTICE_OBSERVE_NOTIFICATION,
                true
            )
            val filterText = TweakApplication.shared.getString(
                Const.SmartNotice.SMART_NOTICE_NOTIFICATION_FILTER,
                null
            ) ?: Const.SmartNotice.NOTIFICATION_FILTER_DEFAULT
            val filter = json.decodeFromString<List<String>>(filterText)

            if (sbn != null && rankingMap != null) {
                filter.filter {
                    sbn.packageName == it && switch && observe
                }.forEach { _ ->
                    postNotification(sbn)
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        super.onNotificationRemoved(sbn, rankingMap)
        val switch = TweakApplication.shared.getBoolean(
            Const.SmartNotice.SMART_NOTICE_SWITCH,
            false
        )
        val observe = TweakApplication.shared.getBoolean(
            Const.SmartNotice.Observe.SMART_NOTICE_OBSERVE_NOTIFICATION,
            true
        )
    }
}