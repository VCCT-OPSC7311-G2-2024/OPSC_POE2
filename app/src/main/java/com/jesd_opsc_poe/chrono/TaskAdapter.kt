package com.jesd_opsc_poe.chrono

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private val taskList: List<Task>,
    private val onItemClick: (Task) -> Unit
    ) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        val helperClass = HelperClass
        val formattedDuration = task.duration + ":00"
        holder.tvDescription.text = task.description
        holder.tvDuration.text = formattedDuration
        holder.tvClient.text = task.clientName
        holder.tvCategory.text = task.categoryName
        holder.tvDate.text = helperClass.getPrettyDate(task.date!!)
        holder.bind(task)
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDescription: TextView = itemView.findViewById(R.id.tvItemTaskDescription)
        val tvDuration: TextView = itemView.findViewById(R.id.tvItemTaskDuration)
        val tvClient: TextView = itemView.findViewById(R.id.tvItemTaskClient)
        val tvCategory: TextView = itemView.findViewById(R.id.tvItemTaskCategory)
        val tvDate: TextView = itemView.findViewById(R.id.tvItemTaskDate)

        private val lytItem: ConstraintLayout = itemView.findViewById(R.id.lytItemTask)
        fun bind(task: Task) {
            lytItem.setOnClickListener {
                onItemClick(task)
            }
        }

    }
}