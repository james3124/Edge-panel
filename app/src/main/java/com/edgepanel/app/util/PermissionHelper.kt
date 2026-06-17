package com.edgepanel.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionHelper {

    fun hasOverlayPermission(ctx: Context): Boolean =
        Settings.canDrawOverlays(ctx)

    fun hasFreeformPermission(ctx: Context): Boolean {
        return try {
            val pm = ctx.packageManager
            pm.checkPermission(
                "android.permission.FREEFORM_WINDOW_MANAGEMENT",
                ctx.packageName
            ) == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) { false }
    }

    fun hasContactsPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    fun hasLocationPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasPhonePermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    fun adbGrantCommand(packageName: String): String =
        "adb shell pm grant $packageName android.permission.FREEFORM_WINDOW_MANAGEMENT"
}
