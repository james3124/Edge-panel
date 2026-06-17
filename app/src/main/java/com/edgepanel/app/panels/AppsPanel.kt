package com.edgepanel.app.panels

import android.content.Context
import android.graphics.Color
import android.text.Editable; import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edgepanel.app.adapter.AppGridAdapter
import com.edgepanel.app.model.AppItem
import com.edgepanel.app.overlay.FloatingWindowController
import com.edgepanel.app.util.AppUtils
import com.edgepanel.app.util.PrefsManager
import kotlinx.coroutines.*

class AppsPanel(
    context: Context,
    private val floatingCtrl: FloatingWindowController
) : LinearLayout(context) {

    private val prefs   = PrefsManager(context)
    private val scope   = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var adapter: AppGridAdapter
    private val recycler: RecyclerView
    private val search: EditText
    private val progress: ProgressBar
    private var allApps: List<AppItem> = emptyList()

    init {
        orientation = VERTICAL

        // ── Search bar ────────────────────────────────────────────────────────
        search = EditText(context).apply {
            hint    = "Search apps…"
            textSize = 14f
            setPadding(40, 24, 40, 24)
            setBackgroundColor(Color.parseColor("#EEEEEE"))
            setSingleLine()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        addView(search)
        addView(View(context).apply {
            setBackgroundColor(Color.parseColor("#E0E0E0"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 1)
        })

        // ── Progress ──────────────────────────────────────────────────────────
        progress = ProgressBar(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also { it.topMargin = 24 }
        }
        addView(progress)

        // ── Grid ──────────────────────────────────────────────────────────────
        recycler = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 3)
            layoutParams  = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }
        addView(recycler)

        loadApps()

        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = adapter.filter(s.toString())
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    private fun loadApps() {
        scope.launch {
            progress.visibility = VISIBLE
            allApps = withContext(Dispatchers.IO) { AppUtils.loadAllApps(context) }

            val pinned = prefs.getPinnedApps()
            allApps.forEach { it.isPinned = it.packageName in pinned }
            val sorted = allApps.sortedWith(
                compareByDescending<AppItem> { it.isPinned }.thenBy { it.label.lowercase() }
            )

            adapter = AppGridAdapter(
                context, sorted,
                onTap      = { launchApp(it) },
                onLongPress = { togglePin(it) }
            )
            recycler.adapter = adapter
            progress.visibility = GONE
        }
    }

    private fun launchApp(item: AppItem) {
        floatingCtrl.launch(item)
    }

    private fun togglePin(item: AppItem) {
        val pins = prefs.getPinnedApps().toMutableSet()
        if (item.isPinned) pins.remove(item.packageName) else pins.add(item.packageName)
        prefs.savePinnedApps(pins)
        loadApps()
        Toast.makeText(
            context,
            if (!item.isPinned) "Pinned ${item.label}" else "Unpinned ${item.label}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}
