package com.edgepanel.app.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.edgepanel.app.model.AppItem
import com.edgepanel.app.util.resolveThemeDrawable

class AppGridAdapter(
    private val ctx: Context,
    private var items: List<AppItem>,
    private val onTap: (AppItem) -> Unit,
    private val onLongPress: (AppItem) -> Unit
) : RecyclerView.Adapter<AppGridAdapter.VH>() {

    private var filtered = items.toMutableList()

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView  = v.findViewById(android.R.id.icon)
        val label: TextView  = v.findViewById(android.R.id.text1)
        val badge: TextView  = v.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val cell = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                ctx.resources.displayMetrics.let { (it.widthPixels * 0.78f / 3).toInt() }
            )
            setPadding(8, 20, 8, 20)
            isClickable  = true
            isFocusable  = true
            setBackgroundResource(ctx.resolveThemeDrawable(android.R.attr.selectableItemBackground))
        }

        val ico = ImageView(ctx).apply {
            id = android.R.id.icon
            layoutParams = LinearLayout.LayoutParams(96, 96)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val lbl = TextView(ctx).apply {
            id = android.R.id.text1
            textSize  = 11f
            maxLines  = 2
            gravity   = Gravity.CENTER
            setTextColor(Color.parseColor("#212121"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 8 }
        }
        val badge = TextView(ctx).apply {
            id = android.R.id.text2
            textSize  = 9f
            setTextColor(Color.parseColor("#6200EE"))
            gravity   = Gravity.CENTER
            visibility = View.GONE
        }
        cell.addView(ico); cell.addView(lbl); cell.addView(badge)
        return VH(cell)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val app = filtered[pos]
        h.icon.setImageDrawable(app.icon)
        h.label.text = app.label
        if (app.isSystemApp) {
            h.badge.text       = "SYS"
            h.badge.visibility = View.VISIBLE
        } else {
            h.badge.visibility = View.GONE
        }
        if (app.isPinned) {
            h.label.typeface = Typeface.DEFAULT_BOLD
        } else {
            h.label.typeface = Typeface.DEFAULT
        }
        h.itemView.setOnClickListener      { onTap(app) }
        h.itemView.setOnLongClickListener  { onLongPress(app); true }
    }

    override fun getItemCount() = filtered.size

    fun filter(query: String) {
        filtered = if (query.isBlank()) items.toMutableList()
                   else items.filter { it.label.contains(query, ignoreCase = true) }.toMutableList()
        notifyDataSetChanged()
    }

    fun updateList(newItems: List<AppItem>) {
        items    = newItems
        filtered = newItems.toMutableList()
        notifyDataSetChanged()
    }
}
