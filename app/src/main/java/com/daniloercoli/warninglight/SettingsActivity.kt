package com.daniloercoli.warninglight

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Apply window insets to account for the status bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_container)) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val blinkIntervalPref = findPreference<EditTextPreference>("blink_interval")
            blinkIntervalPref?.let {
                updateBlinkIntervalSummary(it, it.text)

                it.setOnPreferenceChangeListener { _, newValue ->
                    updateBlinkIntervalSummary(it, newValue as String)
                    true
                }
            }
        }

        private fun updateBlinkIntervalSummary(preference: EditTextPreference, value: String?) {
            val interval = value?.toIntOrNull() ?: 900
            preference.summary = "$interval ms"
        }
    }
}