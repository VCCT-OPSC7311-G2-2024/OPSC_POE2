package com.jesd_opsc_poe.chrono

import android.app.Dialog
import android.content.Context
import android.widget.Button
import android.widget.NumberPicker

class DurationPickerDialog(context: Context, private val onDurationSelected: (hours: Int, minutes: Int) -> Unit) : Dialog(context) {

    private var hoursPicker: NumberPicker
    private var minutesPicker: NumberPicker

    init {
        setContentView(R.layout.dialog_duration_picker)
        setTitle("Select Duration")

        hoursPicker = findViewById(R.id.npHours)
        minutesPicker = findViewById(R.id.npMinutes)

        hoursPicker.minValue = 0
        hoursPicker.maxValue = 23

        minutesPicker.minValue = 0
        minutesPicker.maxValue = 59

        findViewById<Button>(R.id.btnPositive).setOnClickListener {
            val selectedHours = hoursPicker.value
            val selectedMinutes = minutesPicker.value

            onDurationSelected(selectedHours, selectedMinutes)
            dismiss()
        }
        findViewById<Button>(R.id.btnNegative).setOnClickListener {
            dismiss()
        }
    }
}