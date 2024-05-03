package com.jesd_opsc_poe.chrono

data class Task(val taskKey: String? = null,
                val categoryKey: String? = null,
                val clientKey: String? = null,
                val userKey: String? = null,
                val categoryName: String? = null,
                val clientName: String? = null,
                val description: String? = null,
                val date : String? = null,
                val startTime: String? = null,
                val endTime: String? = null,
                val duration: String? = null,
                val imageUrl: String? = null
) : java.io.Serializable
