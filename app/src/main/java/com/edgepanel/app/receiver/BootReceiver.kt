package com.edgepanel.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.edgepanel.app.service.EdgePanelService
import com.edgepanel.app.util.PrefsManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") return

        val prefs = PrefsManager(context)
        if (prefs.isServiceEnabled() && Settings.canDrawOverlays(context)) {
            val svc = Intent(context, EdgePanelService::class.java)
            context.startForegroundService(svc)
        }
    }
}
