package com.edgepanel.app.overlay

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.edgepanel.app.model.HandleConfig
import kotlin.math.abs
import kotlin.math.sqrt

class EdgeHandleView(
    context: Context,
    private var config: HandleConfig
) : View(context) {

    var onPanelOpen: (() -> Unit)? = null
    var windowManager: WindowManager? = null
    var windowParams: WindowManager.LayoutParams? = null

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val gripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3.5f
        style        = Paint.Style.STROKE
        strokeCap    = Paint.Cap.ROUND
        color        = 0xAAFFFFFF.toInt()
    }
    private val rect  = RectF()
    private val path  = Path()

    private var touchStartX = 0f; private var touchStartY = 0f
    private var initWinY    = 0
    private var isDragging  = false

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val r = config.cornerRadius

        fillPaint.color = config.color
        fillPaint.alpha = (config.transparency * 255f).toInt().coerceIn(40, 255)

        // Shape: flat on the edge side, rounded on the inner side
        path.reset()
        rect.set(0f, 0f, w, h)
        val radii = if (config.isLeft)
            floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f)
        else
            floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r)
        path.addRoundRect(rect, radii, Path.Direction.CW)
        canvas.drawPath(path, fillPaint)

        // Grip dots/lines
        val cx  = w / 2f
        val ll  = w * 0.50f
        val gap = h * 0.085f
        for (i in -2..2) {
            val y = h / 2f + i * gap
            canvas.drawLine(cx - ll / 2f, y, cx + ll / 2f, y, gripPaint)
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = ev.rawX; touchStartY = ev.rawY
                initWinY    = windowParams?.y ?: 0
                isDragging  = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = ev.rawY - touchStartY
                val dx = abs(ev.rawX - touchStartX)
                if (!isDragging && abs(dy) > 14f && abs(dy) > dx) isDragging = true
                if (isDragging) {
                    val p = windowParams ?: return true
                    p.y = initWinY + dy.toInt()
                    try { windowManager?.updateViewLayout(this, p) } catch (_: Exception) {}
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx   = ev.rawX - touchStartX
                val dy   = ev.rawY - touchStartY
                val dist = sqrt(dx * dx + dy * dy)
                if (!isDragging) {
                    if (dist < 22f) {
                        // Tap
                        onPanelOpen?.invoke(); performClick()
                    } else if (abs(dx) > 80f) {
                        // Horizontal swipe
                        val correct = (config.isLeft && dx > 0) || (!config.isLeft && dx < 0)
                        if (correct) { onPanelOpen?.invoke(); performClick() }
                    }
                }
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(ev)
    }

    override fun performClick(): Boolean { super.performClick(); return true }

    fun updateConfig(c: HandleConfig) { config = c; invalidate() }
}
