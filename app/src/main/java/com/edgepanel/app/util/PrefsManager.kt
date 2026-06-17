package com.edgepanel.app.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.edgepanel.app.model.HandleConfig
import com.edgepanel.app.model.Task

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("edge_panel_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // ── Service ──────────────────────────────────────────────────────────────
    fun isServiceEnabled(): Boolean = prefs.getBoolean("service_enabled", false)
    fun setServiceEnabled(v: Boolean) = prefs.edit().putBoolean("service_enabled", v).apply()

    // ── Handle config ────────────────────────────────────────────────────────
    fun getHandleConfig(): HandleConfig {
        val json = prefs.getString("handle_config", null) ?: return HandleConfig()
        return try { gson.fromJson(json, HandleConfig::class.java) } catch (e: Exception) { HandleConfig() }
    }
    fun saveHandleConfig(config: HandleConfig) =
        prefs.edit().putString("handle_config", gson.toJson(config)).apply()

    // ── Tasks ────────────────────────────────────────────────────────────────
    fun getTasks(): MutableList<Task> {
        val json = prefs.getString("tasks", null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<Task>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) { mutableListOf() }
    }
    fun saveTasks(tasks: List<Task>) =
        prefs.edit().putString("tasks", gson.toJson(tasks)).apply()

    // ── Pinned contacts ──────────────────────────────────────────────────────
    fun getPinnedContactIds(): Set<String> =
        prefs.getStringSet("pinned_contacts", emptySet()) ?: emptySet()
    fun savePinnedContactIds(ids: Set<String>) =
        prefs.edit().putStringSet("pinned_contacts", ids).apply()

    // ── Pinned apps ──────────────────────────────────────────────────────────
    fun getPinnedApps(): Set<String> =
        prefs.getStringSet("pinned_apps", emptySet()) ?: emptySet()
    fun savePinnedApps(apps: Set<String>) =
        prefs.edit().putStringSet("pinned_apps", apps).apply()

    // ── Weather ──────────────────────────────────────────────────────────────
    fun getWeatherCity(): String = prefs.getString("weather_city", "London") ?: "London"
    fun setWeatherCity(city: String) = prefs.edit().putString("weather_city", city).apply()
    fun getWeatherApiKey(): String = prefs.getString("weather_api_key", "") ?: ""
    fun setWeatherApiKey(key: String) = prefs.edit().putString("weather_api_key", key).apply()
    fun useMetric(): Boolean = prefs.getBoolean("weather_metric", true)
    fun setMetric(v: Boolean) = prefs.edit().putBoolean("weather_metric", v).apply()

    // ── Visible panels ───────────────────────────────────────────────────────
    fun getVisiblePanels(): Set<String> =
        prefs.getStringSet("visible_panels", setOf("Apps","Tasks","People","Weather")) ?: setOf("Apps","Tasks","People","Weather")
    fun setVisiblePanels(panels: Set<String>) =
        prefs.edit().putStringSet("visible_panels", panels).apply()
}
