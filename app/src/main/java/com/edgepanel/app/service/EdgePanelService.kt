package com.edgepanel.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.edgepanel.app.R
import com.edgepanel.app.model.HandleConfig
import com.edgepanel.app.overlay.EdgeHandleView
import com.edgepanel.app.overlay.PanelView
import com.edgepanel.app.util.PrefsManager

class EdgePanelService : Service() {

    companion object {
        const val CHANNEL_ID  = "edge_panel_channel"
        const val NOTIF_ID    = 101
        const val ACTION_STOP = "ACTION_STOP_SERVICE"
        var isRunning = false
    }

    private lateinit var wm: WindowManager
    private lateinit var prefs: PrefsManager
    private var handleView: EdgeHandleView? = null
    private var panelView: PanelView? = null
    private var panelParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        wm    = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = PrefsManager(this)
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
        setupOverlay()
    }

    private fun setupOverlay() {
        val cfg = prefs.getHandleConfig()
        addPanelView(cfg)
        addHandleView(cfg)
    }

    private fun addPanelView(cfg: HandleConfig) {
        val pv = PanelView(this, cfg)

        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.TOP or Gravity.START
        panelParams = p
        panelView   = pv

        pv.wmRef     = wm
        pv.paramsRef = p
        pv.visibility = View.GONE
        wm.addView(pv, p)
    }

    private fun addHandleView(cfg: HandleConfig) {
        val hv = EdgeHandleView(this, cfg)
        hv.onPanelOpen = { togglePanel() }

        val p = WindowManager.LayoutParams(
            cfg.width, cfg.height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        p.gravity = if (cfg.isLeft) Gravity.START or Gravity.CENTER_VERTICAL
                    else             Gravity.END   or Gravity.CENTER_VERTICAL
        p.y = cfg.yOffset

        hv.windowManager = wm
        hv.windowParams  = p
        handleView = hv
        wm.addView(hv, p)
    }

    private fun togglePanel() {
        val pv = panelView ?: return
        if (pv.isPanelVisible) pv.hide() else pv.show()
    }

    /** Call after changing handle config in settings */
    fun refreshHandle() {
        handleView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        addHandleView(prefs.getHandleConfig())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handleView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panelView?.let  { try { wm.removeView(it) } catch (_: Exception) {} }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID, "Edge Panel Service", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps Edge Panel running in the background"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val stopPi = PendingIntent.getService(
            this, 0,
            Intent(this, EdgePanelService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Edge Panel Active")
            .setContentText("Swipe from the edge to open your panel")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true).setSilent(true)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPi)
            .build()
    }
}
