package com.edgepanel.app.panels

import android.content.Context
import android.graphics.Color
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edgepanel.app.adapter.TaskAdapter
import com.edgepanel.app.model.Task
import com.edgepanel.app.util.PrefsManager

class TasksPanel(context: Context) : LinearLayout(context) {

    private val prefs   = PrefsManager(context)
    private val tasks   = prefs.getTasks()
    private lateinit var adapter: TaskAdapter

    init {
        orientation = VERTICAL

        // ── Header with counter ───────────────────────────────────────────────
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(32, 24, 24, 16)
            gravity     = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        val title = TextView(context).apply {
            text = "My Tasks"; textSize = 16f
            setTextColor(Color.parseColor("#6200EE"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        val clearBtn = TextView(context).apply {
            text = "Clear done"; textSize = 12f
            setTextColor(Color.parseColor("#B0BEC5"))
            setOnClickListener { clearDone() }
        }
        header.addView(title); header.addView(clearBtn)
        addView(header)

        // ── RecyclerView ──────────────────────────────────────────────────────
        val recycler = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            layoutParams  = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }
        adapter = TaskAdapter(context, tasks) { saveTasks() }
        recycler.adapter = adapter
        addView(recycler)

        // ── Add task row ──────────────────────────────────────────────────────
        val addRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(24, 16, 16, 24)
            gravity     = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#F3E5F5"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        val input = EditText(context).apply {
            hint     = "Add a task…"; textSize = 14f
            setBackgroundColor(Color.TRANSPARENT)
            setSingleLine()
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        val addBtn = TextView(context).apply {
            text = "＋"; textSize = 22f
            setTextColor(Color.parseColor("#6200EE"))
            setPadding(20, 0, 8, 0)
            setOnClickListener {
                val txt = input.text.toString().trim()
                if (txt.isNotEmpty()) {
                    tasks.add(0, Task(title = txt))
                    adapter.notifyItemInserted(0)
                    saveTasks(); input.text.clear()
                    recycler.scrollToPosition(0)
                }
            }
        }
        addRow.addView(input); addRow.addView(addBtn)
        addView(addRow)
    }

    private fun saveTasks()    = prefs.saveTasks(tasks)
    private fun clearDone() {
        val it = tasks.iterator()
        var pos = 0
        while (it.hasNext()) {
            if (it.next().isDone) { it.remove(); adapter.notifyItemRemoved(pos) } else pos++
        }
        saveTasks()
    }
}
