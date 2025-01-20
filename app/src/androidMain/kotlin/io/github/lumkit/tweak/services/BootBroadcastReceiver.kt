package io.github.lumkit.tweak.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.lumkit.tweak.common.util.autoStartService

class BootBroadcastReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        println("接收广播: ${intent.action}")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                autoStartService(KeepAliveService::class.java)
            }
        }
    }

}