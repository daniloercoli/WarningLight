package com.daniloercoli.warninglight

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "About"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}