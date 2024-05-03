package com.jesd_opsc_poe.chrono

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.jakewharton.threetenabp.AndroidThreeTen
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.LocalDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min


class GraphActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)
        auth = Firebase.auth
        AndroidThreeTen.init(this)
        val lineChart: LineChart = findViewById(R.id.lineChart)
        val lsTimePeriods = listOf<String>("Last 10 days", "Last 20 days", "Last 30 days")

        val spinner = findViewById<Spinner>(R.id.spinTimePeriod)
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, lsTimePeriods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        var selectedItem: String? = null
        var noOfDays: Int = 9

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                selectedItem = p0?.selectedItem.toString()
                updateLineChart(selectedItem!!,noOfDays)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { // Handle the case when nothing is selected
                selectedItem = null
            }
        }
    }

    private fun convertTimeToFloat(time: String?): Float {
        val parts = time!!.split(":")
        val hours = parts.get(0).toInt()
        val minutes = parts.get(1).toInt()
        val floatTime = hours.plus(((minutes.toFloat()) / 60))
        return floatTime
    }

    private fun calculateDailyTotalHours(tasks: Map<String, Task>?, desiredDate: String?): DailyTotal {
        var totalHours = 0f
        if (tasks != null) {
            for (task in tasks.values) {
                if (task.date == desiredDate) {
                    totalHours += convertTimeToFloat(task.duration!!)
                }
            }
        }

        return DailyTotal(totalHours, desiredDate)
    }

    private fun getDatesInRange(startDate: String, lastDate: String): List<String> {
        val dates = mutableListOf<String>()
        var currentDate = LocalDate.parse(startDate)
        var endDate = LocalDate.parse(lastDate)

        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
            currentDate = currentDate.plusDays(1)
        }

        return dates
    }

    private fun minusDaysFromCurrentDate(days: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val resultDate: Date = calendar.time
        return dateFormat.format(resultDate)
    }


    private fun updateLineChart(selectedItem: String, num: Int) {

        val totalHrsEntries : MutableList<Entry> = mutableListOf()
        val minGoalEntries : MutableList<Entry> = mutableListOf()
        val maxGoalEntries : MutableList<Entry> = mutableListOf()
        val lineChart: LineChart = findViewById(R.id.lineChart)
        val xAxisLine: XAxis = lineChart.xAxis
        var noOfDays = num

        xAxisLine.position = XAxis.XAxisPosition.BOTTOM
        xAxisLine.setDrawGridLines(false)
        xAxisLine.axisMaximum = noOfDays.toFloat()
        xAxisLine.axisMinimum = 1F
        xAxisLine.resetAxisMinimum()
        xAxisLine.resetAxisMaximum()

        val yAxisLine: YAxis = lineChart.axisLeft
        yAxisLine.setDrawGridLines(false)
        lineChart.axisRight.isEnabled = false
        yAxisLine.axisMaximum = 24F
        yAxisLine.axisMinimum = 0F
        val description: Description = lineChart.description
        description.text = ""



        when(selectedItem){
            "Last 10 days" -> {
                noOfDays = 9
            }
            "Last 20 days" -> {
                noOfDays = 19
            }
            "Last 30 days" -> {
                noOfDays = 29
            }
        }

        var currentUser = auth.currentUser
        var userKey: String? = ""
        if (currentUser != null) {
            userKey = currentUser.email
        } else {
            // No user is signed in
        }
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate: Date = calendar.time
        var startDate : String = minusDaysFromCurrentDate(noOfDays)
        var endDate : String=  dateFormat.format(todayDate)
        var dates : List<String> = getDatesInRange(startDate, endDate)

        val dbTasksref = FirebaseDatabase.getInstance().getReference("Tasks")
        var allTasks : HashMap<String, Task>?
        val dbGoalsRef = FirebaseDatabase.getInstance().getReference("DailyGoals")

        dbGoalsRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                minGoalEntries.clear()
                maxGoalEntries.clear()
                val allGoals = snapshot.getValue<HashMap<String, DailyGoal>>()
                val userGoals : Map<String, DailyGoal>? = allGoals?.filterValues { it.userKey == userKey }
                var dailyGoals : MutableList<DailyGoal> = mutableListOf()

                for(date in dates){
                    val matchingObj = userGoals!!.filterValues { it.date == date }
                    if(matchingObj.isEmpty()) {
                        dailyGoals.add(DailyGoal("00:00:00","00:00:00",date))
                    }
                    else{
                        dailyGoals.add(userGoals!!.filterValues { it.date == date}.entries.last().value)
                    }
                }

                var i: Float = 1F
                for (dailyGoal in dailyGoals) {
                    minGoalEntries.add(Entry(i, convertTimeToFloat(dailyGoal.min)))
                    i++
                }

                var j: Float = 1F
                for(dailyGoal in dailyGoals){
                    maxGoalEntries.add(Entry(j, convertTimeToFloat(dailyGoal.max)))
                    j++
                }


            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("fail", "onCancelled : ${error.toException()}")
            }

        })

        dbTasksref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                totalHrsEntries.clear()
                allTasks = snapshot.getValue<HashMap<String, Task>>()
                val userMap: Map<String, Task>? =
                    allTasks?.filterValues { it.userKey == userKey }

                var dailyTotals : MutableList<DailyTotal> = mutableListOf<DailyTotal>()
                for(date in dates){
                    dailyTotals.add(calculateDailyTotalHours(userMap, date))
                }
                if (dailyTotals != null) {
                    var i: Float = 1F
                    for (dailyTotal in dailyTotals) {

                        totalHrsEntries.add(Entry(i, dailyTotal.time))
                        i++
                    }

                    //----------------------------------------------CODE ATTRIBUTION----------------------------------------------
                    //Title (Youtube): "001 Introduction : MP Android Chart Tutorial"
                    //Author: "Sarthi Technology"
                    //URL: "https://www.youtube.com/watch?v=N-_X6G1KgAY&list=PLFh8wpMiEi89LcBupeftmAcgDKCeC24bJ"



                    val totalHrsDataset = LineDataSet(totalHrsEntries, "Total")
                    val minGoalDataset = LineDataSet(minGoalEntries, "Min Goal")
                    val maxGoalDataset = LineDataSet(maxGoalEntries, "Max Goal")

                    totalHrsDataset.color = Color.RED
                    totalHrsDataset.setDrawCircles(false)
                    totalHrsDataset.setDrawValues(false)
                    totalHrsDataset.isVisible = true

                    minGoalDataset.color = Color.BLUE
                    minGoalDataset.setDrawCircles(false)
                    minGoalDataset.setDrawValues(false)
                    minGoalDataset.isVisible = true

                    maxGoalDataset.color = Color.BLACK
                    maxGoalDataset.setDrawCircles(false)
                    maxGoalDataset.setDrawValues(false)
                    maxGoalDataset.isVisible = true

                    var graphData = LineData(totalHrsDataset,minGoalDataset, maxGoalDataset)
                    lineChart.data = graphData

                    lineChart.isEnabled = true
                    lineChart.invalidate()
                    lineChart.refreshDrawableState()

                }


            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("fail", "onCancelled : ${error.toException()}")
            }
        })
    }
}