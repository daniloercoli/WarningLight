package com.daniloercoli.warninglight

import android.app.*
import android.content.*
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class WarningForegroundService : Service() {

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (Intent.ACTION_SCREEN_OFF == intent.action) {
                // Re-emetti la notifica full-screen per riaccendere e riportare l’Activity in primo piano
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIF_ID, buildFullscreenNotification())
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        startForeground(NOTIF_ID, buildFullscreenNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(screenOffReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildFullscreenNotification(): Notification {
        val openActivity = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPI = PendingIntent.getActivity(
            this, 0, openActivity,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.warning_light_active)) // es. "Lampeggio a schermo intero"
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM) // o CATEGORY_CALL
            .setFullScreenIntent(fullScreenPI, true)        // <— chiave: può aprire sopra lockscreen
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Warning Light",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche per la luce di avviso"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                // Prova a bypassare DND quando consentito dall’utente:
                setBypassDnd(true)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "warning_light_channel"
        private const val NOTIF_ID = 42
    }
}
