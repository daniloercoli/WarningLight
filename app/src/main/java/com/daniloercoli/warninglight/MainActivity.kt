package com.daniloercoli.warninglight

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {
    private lateinit var blinkingView: View
    private lateinit var blinkAnimator: ValueAnimator
    private var blinkColor: Int =
        Color.RED  // Define blinkColor at class level with a default value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ensureNotificationsThenStartService()

        // Configura la finestra per rimanere sopra la schermata di blocco
        setupWindowFlags()

        setContentView(R.layout.activity_main)

        blinkingView = findViewById(R.id.blinkingView)
        setupBlinkingAnimation()

        // Enable the app bar to show the settings icon
        setSupportActionBar(findViewById(R.id.toolbar))

        // Hide system UI
        hideSystemUI()

        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitDialog()
                }
            })
    }

    private fun setupWindowFlags() {
        // Evita lo spegnimento per timeout mentre l’activity è visibile
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onResume() {
        super.onResume()
        updateBlinkingViewSettings()
        hideSystemUI()

        // Riapplica i flag della finestra quando l'app torna in primo piano
        setupWindowFlags()
    }

    private fun setupBlinkingAnimation() {
        blinkAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animator ->
                val alpha = animator.animatedValue as Float
                blinkingView.setBackgroundColor(adjustAlpha(blinkColor, alpha))
            }
        }
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt()
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun updateBlinkingViewSettings() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        blinkColor =
            prefs.getString("blink_color", "#FF0000")?.let { Color.parseColor(it) } ?: Color.RED
        val interval = prefs.getString("blink_interval", "900")?.toLongOrNull() ?: 900L

        blinkAnimator.duration = interval
        blinkAnimator.start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Pulisci i flag quando l'activity viene distrutta
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            @Suppress("DEPRECATION")
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun ensureNotificationsThenStartService() { // <-- nuovo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (granted) {
                // anche le notifiche di sistema dell’app devono essere attive
                if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                    startWarningService()
                } else {
                    // porta l’utente alle impostazioni se ha disattivato le notifiche
                    openNotificationSettings()
                }
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001
                )
            }
        } else {
            startWarningService()
        }
    }

    private fun startWarningService() {
        ContextCompat.startForegroundService(
            this, Intent(this, WarningForegroundService::class.java)
        )
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    private fun showExitDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name))
            .setMessage("Vuoi chiudere davvero l'app?")
            .setPositiveButton("Chiudi") { _, _ ->
                // 1) Ferma il ForegroundService (e quindi la notifica full-screen)
                stopService(Intent(this, WarningForegroundService::class.java))
                // 2) Chiudi davvero l’app rimuovendo il task dallo switcher
                finishAndRemoveTask()
            }
            .setNegativeButton("Annulla", null)
            .setCancelable(true)
            .show()
    }

}