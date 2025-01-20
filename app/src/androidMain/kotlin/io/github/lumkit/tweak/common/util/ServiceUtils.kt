package io.github.lumkit.tweak.common.util

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build

object ServiceUtils {
    fun isServiceRunning(context: Context, serviceName: String): Boolean {
        val myManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        myManager.getRunningServices(30).forEach {
            val className = it.service.className
            if (className == serviceName) {
                return true
            }
        }
        return false
    }

    fun startForegroundService(context: Context, intent: Intent){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        }else {
            context.startService(intent)
        }
    }

    @SuppressLint("ImplicitSamInstance")
    fun stopForegroundService(context: Context, clazz: Class<*>){
        context.stopService(Intent(context, clazz))
    }
}