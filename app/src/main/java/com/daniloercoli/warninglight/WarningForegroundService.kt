package com.daniloercoli.warninglight

import android.app.*
import android.content.*
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class WarningForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createChannels()

        // 1) Avvia il servizio in foreground con NOTIF "silenziosa"
        startForeground(ONGOING_ID, buildOngoingNotification())

        // 2) Ascolta spegnimento schermo per emettere FSI solo quando serve
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onDestroy() {
        unregisterReceiver(screenOffReceiver)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?) = null

    // === NOTIFICHE ===

    private fun buildOngoingNotification(): Notification {
        // NOTIF di servizio: non deve “saltare”. Canale IMPORTANCE_LOW, nessuna full-screen.
        return NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.warning_light_active)) // es. "Lampeggio attivo"
            .setContentIntent(makeOpenAppPendingIntent())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun makeOpenAppPendingIntent(): PendingIntent {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = "com.daniloercoli.warninglight.ACTION_OPEN_FROM_NOTIFICATION"
        }
        return PendingIntent.getActivity(
            this,
            100,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildWakeNotification(): Notification {
        // NOTIF che riaccende e lancia MainActivity sopra lockscreen
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPI = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_WAKE)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.warning_light_active))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM) // o CALL
            .setFullScreenIntent(fullScreenPI, true)
            .setAutoCancel(true)
            .build()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            val ongoing = NotificationChannel(
                CHANNEL_ONGOING, "Service", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifica di servizio"
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }

            val wake = NotificationChannel(
                CHANNEL_WAKE, "Wake/Full-screen", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Riaccende lo schermo e mostra l'Activity"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true) // se consentito
            }

            nm.createNotificationChannel(ongoing)
            nm.createNotificationChannel(wake)
        }
    }

    // === TRIGGER FULL-SCREEN QUANDO SI SPEGNE LO SCHERMO ===
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (Intent.ACTION_SCREEN_OFF == intent.action) {
                // Emetti solo ora la full-screen
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(WAKE_ID, buildWakeNotification())
            }
        }
    }

    companion object {
        private const val CHANNEL_ONGOING = "warning_light_service"
        private const val CHANNEL_WAKE = "warning_light_wake"
        const val ONGOING_ID = 42
        const val WAKE_ID = 43
    }
}
