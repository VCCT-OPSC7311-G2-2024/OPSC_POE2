package com.jesd_opsc_poe.chrono

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.widget.doOnTextChanged
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var tvLogIn: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var confirm: EditText
    private lateinit var inputEmail: String
    private lateinit var inputPassword: String
    private lateinit var inputConfirm: String
    private lateinit var btnRegister: AppCompatButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        tvLogIn = findViewById(R.id.tvLogin)
        btnRegister = findViewById(R.id.btnRegister)

        tvLogIn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        auth = Firebase.auth

        auth.signOut()
        Global.dailyGoal.max = null
        Global.dailyGoal.min = null

        email = findViewById(R.id.txtRegisterEmail)
        password = findViewById(R.id.txtRegisterPassword)
        confirm = findViewById(R.id.txtRegisterConfirmPassword)
        var emailChanged = false
        var passwordChanged = false
        var confirmChanged = false

        //Live input validation for email
        email.doOnTextChanged { text, _, _, _ ->
            emailChanged = true
            if (text!!.isNotEmpty()) {
                if (HelperClass.notAllSpaces(text.toString())) {
                    email.error = null
                } else {
                    email.error = "Must include characters*"
                }
            } else {
                email.error = "Required*"
            }
        }

        //Live input validation for password
        password.doOnTextChanged { text, _, _, _ ->
            passwordChanged = true
            val l = text!!.length
            if (text.isNotEmpty()) {
                if (HelperClass.notAllSpaces(text.toString())) {
                    if (l < 6) {
                        password.error = "Min 6 Characters"
                    } else {
                        password.error = null
                    }
                } else {
                    password.error = "Must include characters*"
                }
            } else {
                password.error = "Required*"
            }
        }

        //Live input validation for password confirmation
        confirm.doOnTextChanged { text, _, _, _ ->
            confirmChanged = true
            inputPassword = password.text.toString()
            if (text!!.isNotEmpty()) {
                if (HelperClass.notAllSpaces(text.toString())) {
                    if (text.toString() != inputPassword) {
                        confirm.error = "Passwords do not Match"
                    } else {
                        confirm.error = null
                    }
                } else {
                    confirm.error = "Must include characters*"
                }
            } else {
                confirm.error = "Required*"
            }
        }



        btnRegister.setOnClickListener {

            //Latest user inputs are taken
            inputEmail = email.text.toString()
            inputPassword = password.text.toString()
            inputConfirm = confirm.text.toString()

            //validation checks if there are no errors and that the values have been changed by the user to trigger errors.
            if (emailChanged && passwordChanged && confirmChanged && email.error == null && password.error == null && confirm.error == null) {
                //Registration Process is started
                performRegistration()
            } else {
                // If input validation fails
                Toast.makeText(this, "Incorrect or Missing Fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performRegistration() { //Registration process creates a firebase auth user and adds a user reference to firebase real-time database
        auth.createUserWithEmailAndPassword(inputEmail, inputPassword)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    //getting user data from fbAuth to store in fbRD
                    val newUserUid = auth.currentUser?.uid
                    val newUserEmail = auth.currentUser?.email

                    //map containing the key and value to store under the UID
                    val newUserDetails = mapOf(
                        "email" to newUserEmail
                    )

                    //Database reference to the Users node
                    val dbUsersRef = FirebaseDatabase.getInstance().getReference("Users")

                    //Query that inserts the new user into the database (currently not checking for success or failure)
                    newUserUid?.let {
                        dbUsersRef.child(it).setValue(newUserDetails)
                    }

                    //User receives success message and is redirected to TimesheetActivity
                    Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, TimesheetActivity::class.java)
                    startActivity(intent)

                    //This activity is closed, when the user tries to return, they will be taken to the login activity
                    finish()

                } else {
                    // If registration process fails
                    Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
        super.onBackPressed()
    }
}
