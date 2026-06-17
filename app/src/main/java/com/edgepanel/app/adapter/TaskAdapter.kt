package com.edgepanel.app.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.edgepanel.app.model.Task

class TaskAdapter(
    private val ctx: Context,
    private val tasks: MutableList<Task>,
    private val onChange: () -> Unit
) : RecyclerView.Adapter<TaskAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val check : CheckBox  = v.findViewById(android.R.id.checkbox)
        val label : TextView  = v.findViewById(android.R.id.text1)
        val delete: ImageView = v.findViewById(android.R.id.icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val row = LinearLayout(ctx).apply {
            orientation  = LinearLayout.HORIZONTAL
            gravity      = Gravity.CENTER_VERTICAL
            setPadding(24, 18, 24, 18)
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
        }
        val cb  = CheckBox(ctx).apply {
            id = android.R.id.checkbox
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val lbl = TextView(ctx).apply {
            id       = android.R.id.text1
            textSize = 14f
            setTextColor(Color.parseColor("#212121"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = 12 }
        }
        val del = ImageView(ctx).apply {
            id = android.R.id.icon
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.parseColor("#B0BEC5"))
            layoutParams = LinearLayout.LayoutParams(56, 56)
        }
        row.addView(cb); row.addView(lbl); row.addView(del)
        return VH(row)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val task = tasks[pos]
        h.check.isChecked = task.isDone
        h.label.text      = task.title
        h.label.paintFlags = if (task.isDone)
            h.label.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else
            h.label.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        h.label.setTextColor(if (task.isDone) Color.parseColor("#9E9E9E") else Color.parseColor("#212121"))

        h.check.setOnCheckedChangeListener(null)
        h.check.setOnCheckedChangeListener { _, checked ->
            task.isDone = checked; notifyItemChanged(pos); onChange()
        }
        h.delete.setOnClickListener {
            tasks.removeAt(pos); notifyItemRemoved(pos); onChange()
        }
    }

    override fun getItemCount() = tasks.size
}
