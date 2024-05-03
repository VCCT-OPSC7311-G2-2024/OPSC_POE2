package com.jesd_opsc_poe.chrono

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.widget.doOnTextChanged
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class TaskEntryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var btnCreateTask: AppCompatButton
    private lateinit var ibtnAddClient: ImageButton
    private lateinit var ibtnAddCategory: ImageButton
    private lateinit var btnSelectDate: AppCompatButton
    private lateinit var btnStartTime: AppCompatButton
    private lateinit var btnEndTime: AppCompatButton
    private lateinit var newClientName: String
    private lateinit var newCategoryName: String
    private lateinit var selectedClient: String
    private lateinit var selectedCategory: String
    private lateinit var clientsDropdown: AutoCompleteTextView
    private lateinit var categoriesDropdown: AutoCompleteTextView
    private lateinit var txtDescription: TextView
    private lateinit var imageView: ImageView
    private lateinit var uri: Uri
    private lateinit var storageRef: FirebaseStorage
    private var imageAttached = false
    private var descriptionChanged = false
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_entry)

        storageRef = FirebaseStorage.getInstance()
        auth = Firebase.auth
        selectedClient =
            "" //initialised empty for 'isNotEmpty' check in ibtnAddCategory.setOnClickListener()
        selectedCategory = ""
        uri =
            Uri.EMPTY //remains empty until user attaches image (used to check if user attached image)

        ibtnAddClient = findViewById(R.id.ibtnAddClient)
        ibtnAddCategory = findViewById(R.id.ibtnAddCategory)

        //clients dropdown is set and user input is blocked
        clientsDropdown = findViewById(R.id.txtSelectClient)
        clientsDropdown.inputType = InputType.TYPE_NULL
        //list of the current user's clients are added to dropdown
        populateClientsDropdown()

        //categories dropdown is set and user input is blocked
        categoriesDropdown = findViewById(R.id.txtSelectCategory)
        categoriesDropdown.inputType = InputType.TYPE_NULL

        ibtnAddClient.setOnClickListener {
            showInputDialog(true)
        }

        ibtnAddCategory.setOnClickListener {
            if (selectedClient.isNotEmpty()) {
                showInputDialog(false)
            } else {
                Toast.makeText(this, "Client not selected", Toast.LENGTH_SHORT).show()
            }
        }

        clientsDropdown.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                //selected client is set to the clicked item in the 'clients' dropdown
                selectedClient = parent.getItemAtPosition(position) as String
                //categories dropdown is cleared when client changes
                categoriesDropdown.text.clear()
                //categories dropdown is populated with categories under the selected client of the current user
                populateCategoriesDropdown()
                Toast.makeText(this, "selected client: $selectedClient", Toast.LENGTH_SHORT).show()
            }

        categoriesDropdown.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                //selected category is set to the clicked item in the 'categories' dropdown
                selectedCategory = parent.getItemAtPosition(position) as String
                Toast.makeText(this, "selected category: $selectedCategory", Toast.LENGTH_SHORT)
                    .show()
            }

        //Handling description validation
        txtDescription = findViewById(R.id.txtDescription)
        txtDescription.doOnTextChanged { text, _, _, _ ->
            descriptionChanged = true
            val l = text!!.length
            if (text.isNotEmpty()) {
                if (HelperClass.notAllSpaces(text.toString())) {
                    if (l > 30) {
                        txtDescription.error = "Max 30 Characters*"
                    } else {
                        txtDescription.error = null
                    }
                } else {
                    txtDescription.error = "Must include characters*"
                }
            } else {
                txtDescription.error = "Required*"
            }
        }

        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnSelectDate.setOnClickListener {
            showDatePickerDialog()
        }

        btnStartTime = findViewById(R.id.btnStartTime)
        btnStartTime.setOnClickListener {
            showTimePickerDialog(this, btnStartTime)
        }

        btnEndTime = findViewById(R.id.btnEndTime)
        btnEndTime.setOnClickListener {
            showTimePickerDialog(this, btnEndTime)
        }

        imageView = findViewById(R.id.imgTaskImage)

        //----------------------------------------------CODE ATTRIBUTION----------------------------------------------
        //Title (YouTube): "Upload Image to Firebase in Android Studio | Upload Image to Firebase Storage Kotlin"
        //Author: "Waseem Shakoor"
        //URL: "https://www.youtube.com/watch?v=VueRFU7ETOc&ab_channel=WaseemShakoor"
        val galleryImage = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) {
            imageView.setImageURI(it)
            imageAttached = true
            uri = it!!
        }
        imageView.setOnClickListener {
            galleryImage.launch("image/*")
        }
        //------------------------------------------END OF CODE ATTRIBUTION-------------------------------------------

        btnCreateTask = findViewById(R.id.btnCreateTask)
        btnCreateTask.setOnClickListener {
            if (selectedClient.isNotEmpty() && selectedCategory.isNotEmpty() && txtDescription.error.isNullOrEmpty() && descriptionChanged
                && btnSelectDate.text != "Select Date" && btnStartTime.text != "Start Time" && btnEndTime.text != "End Time"
            ) {
                if (imageAttached) {
                    addTaskWithImage()
                } else {
                    addTaskWithoutImage()
                }
            } else {
                Toast.makeText(this, "Incorrect or Missing Fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateClientsDropdown() {
        getClientNamesForCurrentUser { clientNames ->
            val clientArrayAdapter = ArrayAdapter(this, R.layout.item_dropdown, clientNames)
            clientsDropdown.setAdapter(clientArrayAdapter)
        }
    }

    private fun populateCategoriesDropdown() {
        getCategoryNamesForSelectedClient { categoryNames ->
            val categoryArrayAdapter = ArrayAdapter(this, R.layout.item_dropdown, categoryNames)
            categoriesDropdown.setAdapter(categoryArrayAdapter)
        }
    }

    private fun addClient() {
        val dbClientsRef = FirebaseDatabase.getInstance().getReference("Clients")
        val query = dbClientsRef.orderByChild("clientKey")
            .equalTo("${auth.currentUser?.email}_${newClientName}")
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Client already exists under current user
                    Toast.makeText(
                        this@TaskEntryActivity,
                        "Client already exists",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    //combined key for client
                    val clientKey = dbClientsRef.push().key
                    val categoryData = mapOf(
                        "clientKey" to "${auth.currentUser?.email}_${newClientName}",
                        "userKey" to auth.currentUser?.email.toString(),
                        "clientName" to newClientName.trim()
                    )
                    clientKey?.let {
                        dbClientsRef.child(it).setValue(categoryData)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this@TaskEntryActivity,
                                    "Client added",
                                    Toast.LENGTH_SHORT
                                ).show()
                                populateClientsDropdown()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this@TaskEntryActivity,
                                    "Failed to add client",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@TaskEntryActivity,
                    "Add client: database error",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun addCategory() {
        val dbCategoriesRef = FirebaseDatabase.getInstance().getReference("Categories")
        val query = dbCategoriesRef.orderByChild("categoryKey")
            .equalTo("${auth.currentUser?.email}_${selectedClient}_$newCategoryName")
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Category already exists for the selected client and current user
                    Toast.makeText(
                        this@TaskEntryActivity,
                        "Category already exists",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val categoryKey = dbCategoriesRef.push().key
                    val categoryData = mapOf(
                        "categoryKey" to "${auth.currentUser?.email}_${selectedClient}_$newCategoryName",
                        "clientKey" to "${auth.currentUser?.email}_${selectedClient}",
                        "userKey" to auth.currentUser?.email.toString(),
                        "categoryName" to newCategoryName.trim()
                    )
                    categoryKey?.let {
                        dbCategoriesRef.child(it).setValue(categoryData)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this@TaskEntryActivity,
                                    "Category added",
                                    Toast.LENGTH_SHORT
                                ).show()
                                populateCategoriesDropdown()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this@TaskEntryActivity,
                                    "Failed to add category",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@TaskEntryActivity,
                    "Add category: database error",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun addTaskWithImage() {
        //----------------------------------------------CODE ATTRIBUTION----------------------------------------------
        //Title (YouTube): "Upload Image to Firebase in Android Studio | Upload Image to Firebase Storage Kotlin"
        //Author: "Waseem Shakoor"
        //URL: "https://www.youtube.com/watch?v=VueRFU7ETOc&ab_channel=WaseemShakoor"
        storageRef.getReference("images").child(System.currentTimeMillis().toString())
            .putFile(uri)
            .addOnSuccessListener { task ->
                task.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener {
                        //------------------------------------------END OF CODE ATTRIBUTION---------------------------
                        val dbTasksRef = FirebaseDatabase.getInstance().getReference("Tasks")
                        val taskKey = dbTasksRef.push().key
                        val taskData = mapOf(
                            "taskKey" to taskKey.toString(),
                            "categoryKey" to "${auth.currentUser?.email}_${selectedClient}_$selectedCategory",
                            "clientKey" to "${auth.currentUser?.email}_${selectedClient}",
                            "userKey" to auth.currentUser?.email.toString(),
                            "categoryName" to selectedCategory,
                            "clientName" to selectedClient,
                            "description" to txtDescription.text.toString(),
                            "date" to btnSelectDate.text,
                            "startTime" to btnStartTime.text,
                            "endTime" to btnEndTime.text,
                            "duration" to calculateTimeDifference(
                                btnStartTime.text.toString(),
                                btnEndTime.text.toString()
                            ),
                            "imageUrl" to it.toString()
                        )
                        dbTasksRef.child(taskKey!!).setValue(taskData)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this@TaskEntryActivity,
                                    "Task added",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val intent = Intent(this, TimesheetActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this@TaskEntryActivity,
                                    "Failed to add task",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val intent = Intent(this, TimesheetActivity::class.java)
                                startActivity(intent)
                                finish()
                            }

                    }
            }
    }

    private fun addTaskWithoutImage() {

        val dbTasksRef = FirebaseDatabase.getInstance().getReference("Tasks")
        val taskKey = dbTasksRef.push().key
        val duration = calculateTimeDifference(btnStartTime.text.toString(), btnEndTime.text.toString())
        val taskData = mapOf(
            "taskKey" to taskKey.toString(),
            "categoryKey" to "${auth.currentUser?.email}_${selectedClient}_$selectedCategory",
            "clientKey" to "${auth.currentUser?.email}_${selectedClient}",
            "userKey" to auth.currentUser?.email.toString(),
            "categoryName" to selectedCategory,
            "clientName" to selectedClient,
            "description" to txtDescription.text.toString(),
            "date" to btnSelectDate.text,
            "startTime" to btnStartTime.text,
            "endTime" to btnEndTime.text,
            "duration" to duration,
            "imageUrl" to "NULL"
        )
        dbTasksRef.child(taskKey!!).setValue(taskData)
            .addOnSuccessListener {
                Toast.makeText(
                    this@TaskEntryActivity,
                    "Task added",
                    Toast.LENGTH_SHORT
                ).show()
                val intent = Intent(this, TimesheetActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this@TaskEntryActivity,
                    "Failed to add task",
                    Toast.LENGTH_SHORT
                ).show()
                val intent = Intent(this, TimesheetActivity::class.java)
                startActivity(intent)
                finish()
            }
    }

    private fun showInputDialog(isClient: Boolean) { //collects data for client (isClient) or category !(isClient)
        val builder = AlertDialog.Builder(this)
        if (isClient) {
            builder.setTitle("New Client")
        } else {
            builder.setTitle("New Category for $selectedClient")
        }
        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            // Handling the input value
            if (isClient) {
                newClientName = input.text.toString()
                if (newClientName.isNotEmpty() && HelperClass.notAllSpaces(newClientName)) {
                    addClient()
                } else {
                    Toast.makeText(this, "Invalid client name", Toast.LENGTH_SHORT).show()
                }
            } else {
                newCategoryName = input.text.toString()
                if (newCategoryName.isNotEmpty() && HelperClass.notAllSpaces(newCategoryName)) {
                    addCategory()
                } else {
                    Toast.makeText(this, "Invalid category name", Toast.LENGTH_SHORT).show()
                }
            }

        }

        builder.setNegativeButton("Cancel") { _, _ ->
            // Handling cancel action
            newClientName = ""
            newCategoryName = ""
        }
        builder.show()
    }

    private fun getClientNamesForCurrentUser(callback: (ArrayList<String>) -> Unit) {

        //database reference to 'Clients' node
        val clientsRef = FirebaseDatabase.getInstance().getReference("Clients")

        //Array to store the names of the current user's clients
        val clientNames: ArrayList<String> = ArrayList()

        //Query that searches the database reference ('Clients' node) for clients, where userRef = current user's email
        clientsRef.orderByChild("userKey").equalTo(auth.currentUser?.email.toString())
            .addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (clientSnapshot in dataSnapshot.children) {
                        //client names are retrieved from the clients and added to the clientNames array
                        val clientName =
                            clientSnapshot.child("clientName").getValue(String::class.java)
                        clientName?.let { clientNames.add(it) }
                    }
                    //returns array
                    callback(clientNames)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    //no clients retrieved, blank array returned
                    callback(clientNames)
                }
            })
    }

    private fun getCategoryNamesForSelectedClient(callback: (ArrayList<String>) -> Unit) {

        //database reference to 'Categories' node
        val clientsRef = FirebaseDatabase.getInstance().getReference("Categories")

        //Array to store the names of the selected client's categories
        val categoryNames: ArrayList<String> = ArrayList()

        //Query that searches the database reference ('Clients' node) for clients, where userRef = current user's email
        clientsRef.orderByChild("clientKey").equalTo("${auth.currentUser?.email}_${selectedClient}")
            .addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (categorySnapshot in dataSnapshot.children) {
                        //category names are retrieved from the categories and added to the categoryNames array
                        val categoryName =
                            categorySnapshot.child("categoryName").getValue(String::class.java)
                        categoryName?.let { categoryNames.add(it) }
                    }
                    //returns array
                    callback(categoryNames)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    //no categories retrieved, blank array returned
                    callback(categoryNames)
                }
            })
    }

    private fun showDatePickerDialog() { //gets the date input from the user and sets the date button's text to the selected date
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->

                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, monthOfYear, dayOfMonth)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate.time)

                btnSelectDate.text = formattedDate

            }, currentYear, currentMonth, currentDay
        )

        datePickerDialog.show()
    }

    private fun showTimePickerDialog(context: Context, button: Button) {
        // Get the current time as the default time for the picker
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        // Create a time picker dialog
        val timePickerDialog = TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                // Format the selected time as "HH:mm"
                val selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)

                // Set the text of the button to the selected time
                button.text = selectedTime
            },
            hour,
            minute,
            true
        )

        // Show the time picker dialog
        timePickerDialog.show()
    }

    private fun calculateTimeDifference(startTime: String, endTime: String): String {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val calendarStart = Calendar.getInstance()
        val calendarEnd = Calendar.getInstance()

        val parsedStartTime = timeFormat.parse(startTime)
        val parsedEndTime = timeFormat.parse(endTime)

        if (parsedStartTime != null) {
            calendarStart.time = parsedStartTime
        }
        if (parsedEndTime != null) {
            calendarEnd.time = parsedEndTime
        }

        // Check if end time is before start time, indicating it's on the next day
        if (calendarEnd.before(calendarStart)) {
            calendarEnd.add(Calendar.DATE, 1) // Add one day to end time
        }

        val differenceInMillis = calendarEnd.timeInMillis - calendarStart.timeInMillis
        val differenceHours = differenceInMillis / (60 * 60 * 1000) // Convert milliseconds to hours
        val differenceMinutes =
            (differenceInMillis / (60 * 1000)) % 60 // Convert milliseconds to remaining minutes

        return String.format("%02d:%02d", differenceHours, differenceMinutes)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val intent = Intent(this, TimesheetActivity::class.java)
        startActivity(intent)
        finish()
        super.onBackPressed()
    }
}