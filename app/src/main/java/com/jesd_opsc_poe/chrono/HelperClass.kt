package com.jesd_opsc_poe.chrono

import java.text.SimpleDateFormat
import java.util.*

class HelperClass {
    companion object {
        fun notAllSpaces(string: String): Boolean {
            val regex = Regex(".*\\S.*")
            return regex.matches(string)
        }

        fun getPrettyDate(dateString: String): String {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            return outputFormat.format(date!!)
        }

    }
}
