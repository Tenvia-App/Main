package app.tenvia

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import app.tenvia.data.TenviaAi
import kotlin.concurrent.thread

/**
 * TEST BUILD: keeps the on-phone AI loaded and warmed up in the background, so the chat answers
 * without a long first wait. Android requires an ongoing notification for this. The AI uses about
 * 1 GB of memory while it is loaded.
 */
class AiService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notification("Tenvia AI is getting ready (a few minutes)")
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFY_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(NOTIFY_ID, notification)
        thread(name = "tenvia-ai-prepare") {
            val ok = TenviaAi.prepare(applicationContext)
            getSystemService(NotificationManager::class.java).notify(
                NOTIFY_ID,
                notification(if (ok) "Tenvia AI is ready, offline" else "Tenvia AI couldn't start. Open the chat to retry."),
            )
        }
        return START_STICKY
    }

    private fun notification(text: String): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "On-phone AI", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Tenvia")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL = "tenvia-ai"
        private const val NOTIFY_ID = 1

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, AiService::class.java)) }
        }
    }
}

/** Starts the AI after the phone starts up and after the app is updated. */
class AiStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            AiService.start(context)
        }
    }
}
