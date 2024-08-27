package com.daniloercoli.warninglight

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import yuku.ambilwarna.AmbilWarnaDialog

class ColorPickerPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    private var currentColor: Int = 0xFFFF0000.toInt() // Default to red
    private lateinit var colorView: View

    init {
        layoutResource = R.layout.preference_color_picker
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        colorView = holder.findViewById(R.id.color_view)
        updateColorView()
    }

    override fun onClick() {
        AmbilWarnaDialog(context, currentColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog) {}

            override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                currentColor = color
                updateColorView()
                persistString(String.format("#%06X", 0xFFFFFF and color))
            }
        }).show()
    }

    private fun updateColorView() {
        colorView.setBackgroundColor(currentColor)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        currentColor = parseColor(getPersistedString(currentColor.toHexString()))
    }

    private fun parseColor(colorString: String): Int {
        return try {
            android.graphics.Color.parseColor(colorString)
        } catch (e: IllegalArgumentException) {
            0xFFFF0000.toInt() // Default to red if parsing fails
        }
    }

    private fun Int.toHexString(): String {
        return String.format("#%06X", 0xFFFFFF and this)
    }
}