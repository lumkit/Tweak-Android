package io.github.lumkit.tweak.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.model.Const

class SmartNoticeNotificationListenerService: NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        super.onNotificationPosted(sbn, rankingMap)
        if (TweakApplication.shared.getBoolean(Const.SmartNotice.SMART_NOTICE_SWITCH, false)) {
//            println("sbn = [${sbn}], rankingMap = [${rankingMap}]")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        super.onNotificationRemoved(sbn, rankingMap)
        if (TweakApplication.shared.getBoolean(Const.SmartNotice.SMART_NOTICE_SWITCH, false)) {
//            println("sbn = [${sbn}], rankingMap = [${rankingMap}]")
        }
    }
}