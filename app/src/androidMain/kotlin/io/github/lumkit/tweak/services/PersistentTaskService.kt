package io.github.lumkit.tweak.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.common.shell.module.UpdateEngineClient
import io.github.lumkit.tweak.common.shell.provide.ReusableShells
import io.github.lumkit.tweak.model.Const
import io.github.lumkit.tweak.ui.screen.ScreenRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PersistentTaskService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_UPDATE_PROGRESS = "update_progress"
        const val ACTION_UPDATE_SUCCESS = "update_success"
        const val ACTION_UPDATE_CANCEL = "update_cancel"

        const val NOTIFICATION_ID_PROGRESS = 201
        const val NOTIFICATION_ID_SUCCESS = 202
        const val NOTIFICATION_ID_CANCEL = 203

        fun Context.progress(progress: Float, @StringRes statusStringRes: Int) {
            val intent = Intent(this, PersistentTaskService::class.java)
            intent.action = ACTION_UPDATE_PROGRESS
            intent.putExtra("progress", progress)
            intent.putExtra("res", statusStringRes)
            startService(intent)
        }


    }

    private val updateEngineClient = UpdateEngineClient()

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()

        initService()

        CoroutineScope(Dispatchers.IO).launch {
            if (ReusableShells.checkRoot()) {
                startUpdateEngineClientFollow()
            }
        }
    }

    private fun initService() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        initNotification()
    }

    private fun initNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = Const.Notification.NOTIFICATION_CHANNEL_ID_DEFAULT
            val channelName = Const.Notification.NOTIFICATION_CHANNEL_NAME_DEFAULT
            val channel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startUpdateEngineClientFollow() {
        updateEngineClient.watch()
        updateEngineClient.follow()
    }

    @SuppressLint("DefaultLocale")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE_PROGRESS -> {
                val progress = intent.getFloatExtra("progress", 0f) * 100f
                val resStatus = intent.getIntExtra("res", R.string.text_status_idle)

                if (resStatus == R.string.text_status_idle) {
                    return START_STICKY
                }

                val deepLinkUri = "${Const.Navigation.DEEP_LINE}/${ScreenRoute.VAB_UPDATE}".toUri()
                val deepLinkIntent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    deepLinkIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val contentText = if (resStatus == R.string.text_status_downloading) {
                    String.format(
                        "%s: %.2f%s",
                        getString(resStatus),
                        progress,
                        "%"
                    )
                } else {
                    getString(resStatus)
                }

                val builder = NotificationCompat.Builder(
                    this,
                    Const.Notification.NOTIFICATION_CHANNEL_ID_DEFAULT
                )
                    .setContentTitle(getString(R.string.text_vab_updater))
                    .setContentText(contentText)
                    .setSmallIcon(R.mipmap.ic_tweak_logo_round)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)

                if (resStatus == R.string.text_status_downloading) {
                    builder.setProgress(100, progress.toInt(), false)
                        .setAutoCancel(false)
                } else {
                    builder.setAutoCancel(true)
                }

                notificationManager.notify(NOTIFICATION_ID_PROGRESS, builder.build())
            }
        }
        return START_STICKY
    }
}