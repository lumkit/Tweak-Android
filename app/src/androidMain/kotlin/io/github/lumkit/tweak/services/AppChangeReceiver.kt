package io.github.lumkit.tweak.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.lumkit.tweak.TweakApplication

class AppChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                TweakApplication.updateApps()
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                TweakApplication.updateApps()
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                TweakApplication.updateApps()
            }
        }
    }

}