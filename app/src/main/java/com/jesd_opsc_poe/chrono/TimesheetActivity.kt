package com.jesd_opsc_poe.chrono

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class TimesheetActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var rvTask: RecyclerView
    private lateinit var adapterTask: TaskAdapter
    private lateinit var rvCategory: RecyclerView
    private lateinit var adapterCategory: CategoryAdapter
    private lateinit var btnGotoInsights: AppCompatButton
    private lateinit var btnGotoTaskEntry: AppCompatButton
    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var btnClearFilter: Button
    private lateinit var btnApplyFilter: Button
    private lateinit var filterStartDate: String
    private lateinit var filterEndDate: String
    private lateinit var defaultStartDateText: String
    private lateinit var defaultEndDateText: String
    private lateinit var tvFilterTime: TextView
    private var taskList: MutableList<Task> = mutableListOf()
    private var categoryList: MutableList<Category> = mutableListOf()
    private var distinctCategoryList: MutableList<Category> = mutableListOf()
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timesheet)

        auth = Firebase.auth
        defaultStartDateText = "Start Date"
        defaultEndDateText = "End Date"
        btnGotoInsights = findViewById(R.id.btnGotoInsights)
        btnGotoTaskEntry = findViewById(R.id.btnGotoTaskEntry)
        btnStartDate = findViewById(R.id.btnStartDate)
        btnEndDate = findViewById(R.id.btnEndDate)
        tvFilterTime = findViewById(R.id.tvFilterTime)

        btnStartDate.setOnClickListener {
            showDatePickerDialog(true)
        }

        btnEndDate.setOnClickListener {
            showDatePickerDialog(false)
        }

        btnGotoInsights.setOnClickListener {
            val intent = Intent(this, InsightsActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnGotoTaskEntry.setOnClickListener {
            val intent = Intent(this, TaskEntryActivity::class.java)
            startActivity(intent)
            finish()
        }
        btnClearFilter = findViewById(R.id.btnClearFilter)
        btnClearFilter.setOnClickListener {
            resetFilters()
        }

        btnApplyFilter = findViewById(R.id.btnApplyFilter)
        btnApplyFilter.setOnClickListener {
            if (btnStartDate.text.toString() != defaultStartDateText) {
                filterStartDate = btnStartDate.text.toString()
            }
            if (btnEndDate.text.toString() != defaultEndDateText) {
                filterEndDate = btnEndDate.text.toString()
            }
            performTaskQuery(filterStartDate, filterEndDate)
        }
        resetFilters()
    }

    private fun performTaskQuery(dateStart: String, dateEnd: String) {
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
                if (dateStart != "INACTIVE") {
                    taskList =
                        taskList.filter { convertToDate(it.date!!) >= convertToDate(dateStart) } as MutableList<Task>
                }
                if (dateEnd != "INACTIVE") {
                    taskList =
                        taskList.filter { convertToDate(it.date!!) <= convertToDate(dateEnd) } as MutableList<Task>
                }

                taskList.sortByDescending { convertToDate(it.date!!) }

                rvTask = findViewById(R.id.rvTasks)
                adapterTask = TaskAdapter(taskList){task ->
                    val intent = Intent(this@TimesheetActivity, ViewTaskActivity::class.java)
                    intent.putExtra("task", task)
                    startActivity(intent)
                    finish()
                }
                rvTask.adapter = adapterTask
                rvTask.layoutManager = LinearLayoutManager(this@TimesheetActivity)
                populateCategoriesRecyclerView()
                setTotalTaskTime()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@TimesheetActivity,
                    "Database Request Failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun convertToDate(dateString: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return format.parse(dateString)!!
    }

    private fun showDatePickerDialog(startDate: Boolean) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this,
            { _, year, monthOfYear, dayOfMonth ->

                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, monthOfYear, dayOfMonth)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate.time)

                if (startDate) {
                    btnStartDate.text = formattedDate
                } else {
                    btnEndDate.text = formattedDate
                }
            }, currentYear, currentMonth, currentDay
        )

        datePickerDialog.show()
    }

    private fun resetFilters() {
        filterStartDate = "INACTIVE"
        filterEndDate = "INACTIVE"
        btnStartDate.text = defaultStartDateText
        btnEndDate.text = defaultEndDateText
        performTaskQuery(filterStartDate, filterEndDate)
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

    private fun populateCategoriesRecyclerView(){
        //this took too much debugging, but it works now T-T
        categoryList.clear()
        distinctCategoryList.clear()
        taskList.forEach{ task ->
            val category = Category(task.categoryName, task.duration)
            val categoryNoTime = Category(task.categoryName, "00:00")
            categoryList.add(category)
            distinctCategoryList.add(categoryNoTime)
        }

        distinctCategoryList = distinctCategoryList.distinctBy { it.categoryName } as MutableList<Category>

        categoryList.forEach{c ->
            distinctCategoryList.forEach{ dc ->
                if (dc.categoryName == c.categoryName){
                    dc.categoryTime = addDurations(dc.categoryTime!!, c.categoryTime!!)
                }
            }
        }

        rvCategory = findViewById(R.id.rvCategories)
        adapterCategory = CategoryAdapter(distinctCategoryList)
        rvCategory.adapter = adapterCategory
        rvCategory.layoutManager = LinearLayoutManager(this@TimesheetActivity, LinearLayoutManager.HORIZONTAL, false)

    }

    private fun setTotalTaskTime(){
        var totalTime = "00:00"
        taskList.forEach{t ->
            totalTime = addDurations(totalTime, t.duration!!)
        }
        val formattedTime = "$totalTime:00"
        tvFilterTime.text = formattedTime
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
        super.onBackPressed()
    }
}