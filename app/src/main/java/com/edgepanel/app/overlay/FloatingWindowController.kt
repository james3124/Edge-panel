package com.edgepanel.app.overlay

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.*
import android.widget.*
import com.edgepanel.app.model.AppItem
import com.edgepanel.app.model.FloatingWindow
import com.edgepanel.app.model.WindowState
import com.edgepanel.app.util.PermissionHelper

class FloatingWindowController(private val ctx: Context) {

    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val dm = ctx.resources.displayMetrics
    private val chromes = mutableMapOf<String, View>()
    private val windows = mutableMapOf<String, FloatingWindow>()

    fun launch(item: AppItem) {
        if (PermissionHelper.hasFreeformPermission(ctx) && item.isSystemApp) {
            launchFreeform(item)
        } else {
            launchNormal(item)
        }
    }

    private fun launchFreeform(item: AppItem) {
        val w    = (dm.widthPixels  * 0.74f).toInt()
        val h    = (dm.heightPixels * 0.66f).toInt()
        val left = (dm.widthPixels  - w) / 2
        val top  = (dm.heightPixels - h) / 5
        val bounds = Rect(left, top, left + w, top + h)

        try {
            val intent = Intent().apply {
                setClassName(item.packageName, item.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                         Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                         Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            val opts = ActivityOptions.makeBasic().apply { launchBounds = bounds }
            ctx.startActivity(intent, opts.toBundle())

            windows[item.packageName] = FloatingWindow(
                id = item.packageName, packageName = item.packageName,
                label = item.label, bounds = bounds
            )
            addChrome(item, bounds)
        } catch (_: Exception) { launchNormal(item) }
    }

    private fun launchNormal(item: AppItem) {
        ctx.packageManager.getLaunchIntentForPackage(item.packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(it)
        }
    }

    private fun addChrome(item: AppItem, bounds: Rect) {
        val CHROME_H = 88
        val chrome = buildChrome(item, bounds, CHROME_H)

        val p = WindowManager.LayoutParams(
            bounds.width(), CHROME_H,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bounds.left; y = bounds.top - CHROME_H
        }

        wm.addView(chrome, p)
        chromes[item.packageName] = chrome
        installDrag(chrome, p)
    }

    private fun buildChrome(item: AppItem, bounds: Rect, chromeH: Int): LinearLayout {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#263238"))
            setPadding(20, 0, 12, 0)
            gravity = Gravity.CENTER_VERTICAL

            // App icon
            addView(ImageView(ctx).apply {
                setImageDrawable(item.icon)
                layoutParams = LinearLayout.LayoutParams(60, 60).also { it.marginEnd = 14 }
            })

            // Title
            addView(TextView(ctx).apply {
                text = item.label; textSize = 13f; setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            // Window buttons
            mapOf("─" to "min", "□" to "max", "✕" to "close").forEach { (sym, act) ->
                addView(TextView(ctx).apply {
                    text = sym; textSize = 16f; setTextColor(Color.WHITE)
                    setPadding(20, 0, 20, 0); gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(80, LinearLayout.LayoutParams.MATCH_PARENT)
                    setBackgroundResource(android.R.attr.selectableItemBackground)
                    setOnClickListener { onWindowAction(item, act) }
                })
            }
        }
    }

    private fun installDrag(view: View, p: WindowManager.LayoutParams) {
        var sx = 0f; var sy = 0f; var ipx = 0; var ipy = 0
        view.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> { sx = ev.rawX; sy = ev.rawY; ipx = p.x; ipy = p.y; true }
                MotionEvent.ACTION_MOVE -> {
                    p.x = ipx + (ev.rawX - sx).toInt()
                    p.y = ipy + (ev.rawY - sy).toInt()
                    try { wm.updateViewLayout(view, p) } catch (_: Exception) {}
                    true
                }
                else -> false
            }
        }
    }

    private fun onWindowAction(item: AppItem, action: String) {
        when (action) {
            "close" -> removeChrome(item.packageName)
            "min"   -> {
                val win = windows[item.packageName] ?: return
                win.state = WindowState.MINIMIZED
                chromes[item.packageName]?.visibility = View.INVISIBLE
            }
            "max"   -> {
                val win = windows[item.packageName] ?: return
                if (win.state == WindowState.MAXIMIZED) {
                    win.state = WindowState.NORMAL
                } else {
                    win.state = WindowState.MAXIMIZED
                    relaunchWithBounds(item, Rect(0, 0, dm.widthPixels, dm.heightPixels))
                }
            }
        }
    }

    private fun relaunchWithBounds(item: AppItem, bounds: Rect) {
        try {
            val intent = Intent().apply {
                setClassName(item.packageName, item.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            val opts = ActivityOptions.makeBasic().apply { launchBounds = bounds }
            ctx.startActivity(intent, opts.toBundle())
        } catch (_: Exception) {}
    }

    fun removeChrome(packageName: String) {
        chromes[packageName]?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        chromes.remove(packageName); windows.remove(packageName)
    }
}
