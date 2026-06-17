package com.edgepanel.app.adapter

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.edgepanel.app.model.PeopleContact

class ContactAdapter(
    private val ctx: Context,
    private val contacts: List<PeopleContact>,
    private val onCall: (PeopleContact) -> Unit,
    private val onMessage: (PeopleContact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val avatar  : ImageView = v.findViewById(android.R.id.icon)
        val name    : TextView  = v.findViewById(android.R.id.text1)
        val callBtn : TextView  = v.findViewById(android.R.id.text2)
        val msgBtn  : TextView  = v.findViewById(android.R.id.button1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val row = LinearLayout(ctx).apply {
            orientation  = LinearLayout.HORIZONTAL
            gravity      = Gravity.CENTER_VERTICAL
            setPadding(24, 16, 24, 16)
            layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
        }
        val av = ImageView(ctx).apply {
            id = android.R.id.icon
            layoutParams = LinearLayout.LayoutParams(96, 96).also { it.marginEnd = 16 }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        val info = LinearLayout(ctx).apply {
            orientation  = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val nm = TextView(ctx).apply {
            id = android.R.id.text1; textSize = 14f
            setTextColor(Color.parseColor("#212121"))
        }
        info.addView(nm)
        val actions = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.topMargin = 6 }
        }
        val callB = TextView(ctx).apply {
            id = android.R.id.text2; text = "📞 Call"
            textSize = 12f; setTextColor(Color.parseColor("#6200EE"))
            setPadding(0, 4, 24, 4)
        }
        val msgB = TextView(ctx).apply {
            id = android.R.id.button1; text = "💬 Message"
            textSize = 12f; setTextColor(Color.parseColor("#03DAC5"))
            setPadding(0, 4, 0, 4)
        }
        actions.addView(callB); actions.addView(msgB)
        info.addView(actions)
        row.addView(av); row.addView(info)
        return VH(row)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val c = contacts[pos]
        h.name.text = c.name
        if (c.photoUri != null) {
            try { h.avatar.setImageURI(Uri.parse(c.photoUri)) }
            catch (_: Exception) { h.avatar.setImageBitmap(makeInitialsBitmap(c.initials, c.name)) }
        } else {
            h.avatar.setImageBitmap(makeInitialsBitmap(c.initials, c.name))
        }
        h.callBtn.setOnClickListener { onCall(c) }
        h.msgBtn.setOnClickListener  { onMessage(c) }
    }

    override fun getItemCount() = contacts.size

    private val avatarColors = listOf(
        0xFF6200EE.toInt(), 0xFF03DAC5.toInt(), 0xFFE91E63.toInt(),
        0xFF4CAF50.toInt(), 0xFFFF5722.toInt(), 0xFF2196F3.toInt()
    )

    private fun makeInitialsBitmap(initials: String, name: String): Bitmap {
        val size   = 96
        val bm     = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        val color  = avatarColors[name.hashCode().and(0x7FFFFFFF) % avatarColors.size]
        val bgP    = Paint().apply { this.color = color; isAntiAlias = true }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgP)
        val txtP = Paint().apply {
            this.color = Color.WHITE; isAntiAlias = true
            textSize   = size * 0.38f; typeface = Typeface.DEFAULT_BOLD
            textAlign  = Paint.Align.CENTER
        }
        val y = size / 2f - (txtP.descent() + txtP.ascent()) / 2f
        canvas.drawText(initials.take(2), size / 2f, y, txtP)
        return bm
    }
}
