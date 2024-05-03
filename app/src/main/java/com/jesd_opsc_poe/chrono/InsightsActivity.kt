package com.jesd_opsc_poe.chrono

import android.content.Intent
import android.os.Bundle
import android.renderscript.Sampler.Value
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import android.widget.TextView
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.jakewharton.threetenabp.AndroidThreeTen
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import kotlin.math.roundToInt
import java.util.Locale

class InsightsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var btnGotoYourActivity: AppCompatButton
    private var taskList: MutableList<Task> = mutableListOf()
    private var taskListToday: MutableList<Task> = mutableListOf()
    private lateinit var totalTime: String
    private lateinit var txtMin: TextView
    private lateinit var txtMax: TextView
    private lateinit var txtStatus: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvTaskTime: TextView
    private val suffix = ":00"

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insights)
        AndroidThreeTen.init(this);

        auth = Firebase.auth
        btnGotoYourActivity = findViewById(R.id.btnGotoYourActivity)

        val btnMinGoal = findViewById<AppCompatButton>(R.id.btnMinGoal)
        val btnMaxGoal = findViewById<AppCompatButton>(R.id.btnMaxGoal)
        val btnGraph = findViewById<Button>(R.id.btnGraph)

        txtMin = findViewById(R.id.tvMinGoal)
        txtMax = findViewById(R.id.tvMaxGoal)
        txtStatus = findViewById(R.id.tvGoalStatus)
        tvTaskTime = findViewById(R.id.tvYourTimeToday)
        tvTitle = findViewById(R.id.tvGoalTitle)

        dailyStreak()

        btnGraph.setOnClickListener(){

            val intent = Intent(this, GraphActivity::class.java)
            startActivity(intent)
        }

        var min : String? = null
        var max : String? = null

        btnMinGoal.setOnClickListener {
            val durationPickerDialog = DurationPickerDialog(this) { hours, minutes ->
                Global.dailyGoal.min = "$hours:$minutes"
                val t = Global.dailyGoal.min + suffix
                txtMin.text = t
                min = t
                calculateGoal()
            }
            durationPickerDialog.show()
        }

        btnMaxGoal.setOnClickListener {
            val durationPickerDialog = DurationPickerDialog(this) { hours, minutes ->
                Global.dailyGoal.max = "$hours:$minutes"
                val t = Global.dailyGoal.max + suffix
                txtMax.text = t
                max = t
                calculateGoal()
                writeGoalToFB(DailyGoal(min,max,getTodaysDate(),auth.currentUser?.email.toString()))
            }
            durationPickerDialog.show()
        }

        calculateGoal()

        btnGotoYourActivity.setOnClickListener {
            val intent = Intent(this, TimesheetActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun checkIfGoalMet() {
        if (!(txtMin.text == "Not Set" || txtMax.text == "Not Set")) {
            totalTime = "$totalTime:00"
            if(isDurationInRange(txtMin.text.toString(), txtMax.text.toString(), totalTime)){
                val msg = "Goal met"
                txtStatus.text = msg
            }else{
                val msg = "Goal not met"
                txtStatus.text = msg
            }
        }
    }

    private fun setExistingGoalTimes(
        suffix: String,
        txtMin: TextView,
        txtMax: TextView
    ) {
        if (!Global.dailyGoal.min.isNullOrEmpty()) {
            val t = Global.dailyGoal.min + suffix
            txtMin.text = t
        }

        if (!Global.dailyGoal.max.isNullOrEmpty()) {
            val t = Global.dailyGoal.max + suffix
            txtMax.text = t
        }
    }

    private fun calculateGoal() {
        val database = FirebaseDatabase.getInstance()
        val tasksRef = database.getReference("Tasks")

        val query = tasksRef.orderByChild("userKey").equalTo(auth.currentUser!!.email.toString())

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                taskList.clear()
                for (taskSnapshot in dataSnapshot.children) {
                    val task = taskSnapshot.getValue(Task::class.java)
                    task?.let {
                        taskList.add(it)
                    }
                }
                taskListToday.clear()
                taskList.forEach { t ->
                    if (t.date.equals(getCurrentFormattedDate())) {
                        taskListToday.add(t)
                    }
                }
                totalTime = "00:00"
                taskListToday.forEach { t ->
                    totalTime = addDurations(totalTime, t.duration!!)
                }
                val goalTitle = "Daily Goal: " + getCurrentPrettyDate()
                tvTitle.text = goalTitle

                val yourTimeMsg = "Your time today - $totalTime$suffix"
                tvTaskTime.text = yourTimeMsg

                setExistingGoalTimes(suffix, txtMin, txtMax)
                checkIfGoalMet()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@InsightsActivity,
                    "Database Request Failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun getCurrentPrettyDate(): String {
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(currentDate)
    }

    private fun getCurrentFormattedDate(): String {
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(currentDate)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val intent = Intent(this, TimesheetActivity::class.java)
        startActivity(intent)
        finish()
        super.onBackPressed()
    }

    private fun addDurations(time1: String, time2: String): String {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val calendar1 = Calendar.getInstance()
        calendar1.time = timeFormat.parse(time1)!!

        val calendar2 = Calendar.getInstance()
        calendar2.time = timeFormat.parse(time2)!!

        calendar1.add(Calendar.HOUR_OF_DAY, calendar2.get(Calendar.HOUR_OF_DAY))
        calendar1.add(Calendar.MINUTE, calendar2.get(Calendar.MINUTE))

        return timeFormat.format(calendar1.time)
    }

    private fun isDurationInRange(startTime: String, endTime: String, durationToCheck: String): Boolean {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val start = formatter.parse(startTime)
        val end = formatter.parse(endTime)
        val toCheck = formatter.parse(durationToCheck)

        return if (toCheck != null) {
            !toCheck.before(start) && !toCheck.after(end)
        }else{
            false
        }
    }

    private fun writeGoalToFB(dailyGoal : DailyGoal){
        val database = FirebaseDatabase.getInstance()
        val databaseRef: DatabaseReference = database.reference
        if(dailyGoal.min != null && dailyGoal.max != null) {
            val dailyGoalRef: DatabaseReference = databaseRef.child("DailyGoals").push()
            dailyGoalRef.setValue(dailyGoal) { databaseError, _ ->
                    if (databaseError != null) {
                        Log.e("Firebase", "Error writing data: ${databaseError.message}")
                    } else {
                        Log.d("Firebase", "Data written successfully")
                    }
                }
            }
        }


    private fun getTodaysDate() : String{
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate: Date = calendar.time
        return dateFormat.format(todayDate)
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

    private fun dailyStreak(){
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
        var startDate : String = minusDaysFromCurrentDate(6)
        var endDate : String=  dateFormat.format(todayDate)
        var dates : List<String> = getDatesInRange(startDate, endDate)
        var allTasks : HashMap<String, Task>?
        var dailyGoals : MutableList<DailyGoal> = mutableListOf()
        var dateGoalsAchieved : MutableList<LocalDate> = mutableListOf()
        val dbTasksRef = FirebaseDatabase.getInstance().getReference("Tasks")
        val dbGoalsRef = FirebaseDatabase.getInstance().getReference("DailyGoals")

        dbGoalsRef.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val allGoals = snapshot.getValue<HashMap<String, DailyGoal>>()
                val userGoals : Map<String, DailyGoal>? = allGoals?.filterValues { it.userKey == userKey }


                for(date in dates){
                    val matchingObj = userGoals!!.filterValues { it.date == date }
                    if(!matchingObj.isNullOrEmpty()) {

                        dailyGoals.add(userGoals!!.filterValues { it.date == date}.entries.last().value)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("fail", "onCancelled : ${error.toException()}")
            }

        })

        dbTasksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allTasks = snapshot.getValue<HashMap<String, Task>>()
                val userMap: Map<String, Task>? =
                    allTasks?.filterValues { it.userKey == userKey }


                var dailyTotals : MutableList<DailyTotal> = mutableListOf<DailyTotal>()
                for(date in dates){
                    dailyTotals.add(calculateDailyTotalHours(userMap, date))
                }


                    var i = 0
                    for (dailyGoal in dailyGoals) {
                        if (convertTimeToFloat(dailyGoal.min) > 0)
                            if (dailyTotals[i].time >= convertTimeToFloat(dailyGoal.min)) {
                                dateGoalsAchieved.add(LocalDate.parse(dailyGoal.date))
                            }

                    }

                //----------------------------------------------CODE ATTRIBUTION----------------------------------------------
                //Title (Youtube): "How to create a Step Counter/Pedometer in Android Studio (Kotlin 2020)"
                //Author: "Indently"
                //URL: "https://www.youtube.com/watch?app=desktop&v=WSx2a99kPY4"

                val circularProgressBar = findViewById<CircularProgressBar>(R.id.circularProgressBar)
                val tvDailyCount = findViewById<TextView>(R.id.tvDailyCount)

                if(dailyGoals.isNotEmpty()) {
                    circularProgressBar.progress =dailyGoals.count().toFloat()
                }
                else{
                    circularProgressBar.progress = 0F
                }


                tvDailyCount.text = circularProgressBar.progress.roundToInt().toString()
                circularProgressBar.apply {
                    setProgressWithAnimation(progress, 1000)
                    progressMax = 7f
                    startAngle = 0f
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("fail", "onCancelled : ${error.toException()}")
            }

        })

    }






}
