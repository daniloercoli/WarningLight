package com.daniloercoli.warninglight

// MainActivity.kt
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {
    private lateinit var blinkingView: View
    private lateinit var blinkAnimator: ValueAnimator
    private var blinkColor: Int = Color.RED  // Define blinkColor at class level with a default value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        blinkingView = findViewById(R.id.blinkingView)
        setupBlinkingAnimation()

        // Enable the app bar to show the settings icon
        setSupportActionBar(findViewById(R.id.toolbar))

        // Hide system UI
        hideSystemUI()
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onResume() {
        super.onResume()
        updateBlinkingViewSettings()
        hideSystemUI()
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
        blinkColor = prefs.getString("blink_color", "#FF0000")?.let { Color.parseColor(it) } ?: Color.RED
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
}