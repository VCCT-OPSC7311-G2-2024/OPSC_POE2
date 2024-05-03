package com.jesd_opsc_poe.chrono

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso

class ViewTaskActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_view)

        val task = intent.getSerializableExtra("task") as Task
        val helper = HelperClass

        val imageView = findViewById<ImageView>(R.id.imgTask)
        if(task.imageUrl != "NULL") {
            val imageUrl = task.imageUrl
            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.placeholder) // optional placeholder image while loading
                .error(R.drawable.placeholder) // optional error image if loading fails
                .into(imageView)
        }
        val tvDate = findViewById<TextView>(R.id.tvDate)
        tvDate.text = helper.getPrettyDate(task.date!!)

        val tvTimes = findViewById<TextView>(R.id.tvTimes)
        val formattedTimes = task.startTime + " - " + task.endTime
        tvTimes.text = formattedTimes

        val tvDescription = findViewById<TextView>(R.id.tvDescription)
        tvDescription.text = task.description

        val tvClient = findViewById<TextView>(R.id.tvClient)
        tvClient.text = task.clientName

        val tvCategory = findViewById<TextView>(R.id.tvCategory)
        tvCategory.text = task.categoryName

        val tvDuration = findViewById<TextView>(R.id.tvDuration)
        val formattedDuration = task.duration + ":00"
        tvDuration.text = formattedDuration

        val initialHeight = resources.getDimensionPixelSize(R.dimen.initial_height)
        val expandedHeight = resources.getDimensionPixelSize(R.dimen.expanded_height)

        val tvImageHint = findViewById<TextView>(R.id.tvImageHint)

        imageView.setOnClickListener {
            val layoutParams = imageView.layoutParams
            tvImageHint.text = ""
            if (layoutParams.height == initialHeight) {
                // Expand the height to 510dp
                layoutParams.height = expandedHeight
            } else {
                // Restore the initial height
                layoutParams.height = initialHeight
            }
            imageView.layoutParams = layoutParams
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val intent = Intent(this, TimesheetActivity::class.java)
        startActivity(intent)
        finish()
        super.onBackPressed()
    }
}