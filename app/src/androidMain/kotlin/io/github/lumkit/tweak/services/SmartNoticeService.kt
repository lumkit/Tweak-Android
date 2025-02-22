package io.github.lumkit.tweak.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SmartNoticeService: Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
    }



}