package com.edgepanel.app.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.TypedValue
import com.edgepanel.app.model.AppItem

/**
 * Resolves a theme attribute (e.g. android.R.attr.selectableItemBackground) into
 * the actual drawable resource ID the current theme points it at. Needed because
 * setBackgroundResource() requires a real @drawable id, not a raw @attr reference.
 */
fun Context.resolveThemeDrawable(attr: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return typedValue.resourceId
}

object AppUtils {

    /** Excluded packages that clutter the apps panel */
    private val EXCLUDED = setOf(
        "com.android.packageinstaller",
        "com.android.providers.settings",
        "com.android.inputmethod.latin"
    )

    fun loadAllApps(context: Context): List<AppItem> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName !in EXCLUDED }
            .map { ri ->
                val ai = ri.activityInfo.applicationInfo
                AppItem(
                    packageName  = ri.activityInfo.packageName,
                    activityName = ri.activityInfo.name,
                    label        = ri.loadLabel(pm).toString(),
                    icon         = ri.loadIcon(pm),
                    isSystemApp  = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun launchApp(context: Context, item: AppItem) {
        val intent = context.packageManager.getLaunchIntentForPackage(item.packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
    }
}
