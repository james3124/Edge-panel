package com.edgepanel.app.overlay

import android.animation.*
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.*
import android.view.animation.*
import android.widget.*
import com.edgepanel.app.model.HandleConfig
import com.edgepanel.app.panels.*
import com.edgepanel.app.util.PrefsManager

class PanelView(context: Context, private val cfg: HandleConfig) : FrameLayout(context) {

    var onDismiss: (() -> Unit)? = null
    var isPanelVisible = false
    var wmRef: WindowManager? = null
    var paramsRef: WindowManager.LayoutParams? = null

    private val backdrop: View
    private val container: LinearLayout
    private lateinit val tabBar: LinearLayout
    private lateinit val content: FrameLayout
    private val floatingCtrl = FloatingWindowController(context)
    private val dm = context.resources.displayMetrics
    private val panelW = (dm.widthPixels * 0.78f).toInt()

    private val panels: List<Pair<String, View>> by lazy {
        val visible = PrefsManager(context).getVisiblePanels()
        buildList {
            if ("Apps"    in visible) add("Apps"    to AppsPanel(context, floatingCtrl))
            if ("Tasks"   in visible) add("Tasks"   to TasksPanel(context))
            if ("People"  in visible) add("People"  to PeoplePanel(context))
            if ("Weather" in visible) add("Weather" to WeatherPanel(context))
        }
    }

    init {
        // ── Backdrop ──────────────────────────────────────────────────────────
        backdrop = View(context).apply {
            setBackgroundColor(Color.BLACK)
            alpha = 0f
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setOnClickListener { hide() }
        }
        addView(backdrop)

        // ── Sliding container ─────────────────────────────────────────────────
        container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            elevation = 32f
            val lp = LayoutParams(panelW, LayoutParams.MATCH_PARENT)
            lp.gravity = if (cfg.isLeft) Gravity.START else Gravity.END
            layoutParams = lp
        }
        addView(container)

        buildHeader()
        buildTabBar()
        buildContentArea()
        visibility = View.GONE
    }

    private fun buildHeader() {
        val h = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(cfg.color)
            setPadding(56, 0, 24, 0)
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 168)
        }
        h.addView(TextView(context).apply {
            text     = "Edge Panel"
            textSize = 19f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        container.addView(h)
    }

    private fun buildTabBar() {
        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Color.parseColor("#EDE7F6"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 116)
        }
        tabBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        scroll.addView(tabBar)
        container.addView(scroll)
        container.addView(View(context).apply {
            setBackgroundColor(Color.parseColor("#CE93D8"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
        })
    }

    private fun buildContentArea() {
        content = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        container.addView(content)
        panels.forEachIndexed { i, (name, _) -> tabBar.addView(makeTab(name, i)) }
        if (panels.isNotEmpty()) switchTab(0)
    }

    private fun makeTab(label: String, index: Int) = TextView(context).apply {
        text = label; textSize = 13f
        setPadding(52, 0, 52, 0)
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
        setOnClickListener { switchTab(index) }
        applyTabStyle(false)
    }

    private fun TextView.applyTabStyle(selected: Boolean) {
        setTextColor(if (selected) cfg.color else Color.parseColor("#757575"))
        setBackgroundColor(if (selected) Color.parseColor("#F3E5F5") else Color.TRANSPARENT)
        typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun switchTab(index: Int) {
        content.removeAllViews()
        content.addView(panels[index].second)
        for (i in 0 until tabBar.childCount)
            (tabBar.getChildAt(i) as? TextView)?.applyTabStyle(i == index)
    }

    // ── Show / Hide ───────────────────────────────────────────────────────────
    fun show() {
        if (isPanelVisible) return
        isPanelVisible = true

        // Make window interactive
        paramsRef?.let { p ->
            p.flags = p.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            try { wmRef?.updateViewLayout(this, p) } catch (_: Exception) {}
        }
        visibility = View.VISIBLE

        val startX = if (cfg.isLeft) -panelW.toFloat() else panelW.toFloat()
        container.translationX = startX

        ValueAnimator.ofFloat(startX, 0f).apply {
            duration = 300; interpolator = DecelerateInterpolator(2.2f)
            addUpdateListener { container.translationX = it.animatedValue as Float }
            start()
        }
        ValueAnimator.ofFloat(0f, 0.45f).apply {
            duration = 300
            addUpdateListener { backdrop.alpha = it.animatedValue as Float }
            start()
        }
    }

    fun hide() {
        if (!isPanelVisible) return
        isPanelVisible = false
        val endX = if (cfg.isLeft) -panelW.toFloat() else panelW.toFloat()

        ValueAnimator.ofFloat(0f, endX).apply {
            duration = 260; interpolator = AccelerateInterpolator(2f)
            addUpdateListener { container.translationX = it.animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                    // Make window non-interactive again
                    paramsRef?.let { p ->
                        p.flags = p.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        try { wmRef?.updateViewLayout(this@PanelView, p) } catch (_: Exception) {}
                    }
                }
            })
            start()
        }
        ValueAnimator.ofFloat(0.45f, 0f).apply {
            duration = 260
            addUpdateListener { backdrop.alpha = it.animatedValue as Float }
            start()
        }
        onDismiss?.invoke()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent) = isPanelVisible
}
