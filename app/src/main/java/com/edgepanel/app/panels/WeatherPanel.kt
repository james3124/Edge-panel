package com.edgepanel.app.panels

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.*
import android.widget.*
import com.edgepanel.app.api.WeatherService
import com.edgepanel.app.model.WeatherData
import com.edgepanel.app.util.PrefsManager
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class WeatherPanel(context: Context) : ScrollView(context) {

    private val prefs = PrefsManager(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val root  = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private lateinit var emojiView  : TextView
    private lateinit var tempView   : TextView
    private lateinit var descView   : TextView
    private lateinit var detailsView: TextView
    private lateinit var cityView   : TextView
    private lateinit var statusView : TextView

    init {
        addView(root)
        buildUI()
        fetchWeather()
    }

    private fun buildUI() {
        root.setPadding(40, 48, 40, 48)

        // City row
        val cityRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        cityView = TextView(context).apply {
            text = prefs.getWeatherCity(); textSize = 18f
            setTextColor(Color.parseColor("#6200EE"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val refreshBtn = TextView(context).apply {
            text = "↻"; textSize = 22f
            setTextColor(Color.parseColor("#6200EE"))
            setPadding(16, 0, 0, 0)
            setOnClickListener { fetchWeather() }
        }
        cityRow.addView(cityView); cityRow.addView(refreshBtn)
        root.addView(cityRow)

        // Big emoji
        emojiView = TextView(context).apply {
            text = "…"; textSize = 72f; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 32 }
        }
        root.addView(emojiView)

        // Temperature
        tempView = TextView(context).apply {
            textSize = 52f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#212121"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 8 }
        }
        root.addView(tempView)

        // Description
        descView = TextView(context).apply {
            textSize = 16f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#616161"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 8 }
        }
        root.addView(descView)

        // Divider
        root.addView(View(context).apply {
            setBackgroundColor(Color.parseColor("#E0E0E0"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).also { it.topMargin = 32; it.bottomMargin = 24 }
        })

        // Details
        detailsView = TextView(context).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#757575"))
            lineHeight = (textSize * 1.8f).toInt()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        root.addView(detailsView)

        // Status / error
        statusView = TextView(context).apply {
            textSize = 12f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#B0BEC5"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 32 }
        }
        root.addView(statusView)

        // City input row
        root.addView(View(context).apply {
            setBackgroundColor(Color.parseColor("#E0E0E0"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).also { it.topMargin = 32; it.bottomMargin = 16 }
        })
        root.addView(TextView(context).apply {
            text = "Change city"; textSize = 12f
            setTextColor(Color.parseColor("#9E9E9E"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        val inputRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.topMargin = 8 }
        }
        val cityInput = EditText(context).apply {
            hint = "City name"; textSize = 14f
            setText(prefs.getWeatherCity())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val goBtn = TextView(context).apply {
            text = "→"; textSize = 20f
            setTextColor(Color.parseColor("#6200EE"))
            setPadding(20, 0, 0, 0)
            setOnClickListener {
                val city = cityInput.text.toString().trim()
                if (city.isNotEmpty()) {
                    prefs.setWeatherCity(city); cityView.text = city; fetchWeather()
                }
            }
        }
        inputRow.addView(cityInput); inputRow.addView(goBtn)
        root.addView(inputRow)

        // API key input
        root.addView(TextView(context).apply {
            text = "OpenWeatherMap API key"; textSize = 12f
            setTextColor(Color.parseColor("#9E9E9E"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.topMargin = 20 }
        })
        val keyInput = EditText(context).apply {
            hint = "Paste your API key…"; textSize = 13f
            setText(prefs.getWeatherApiKey())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.topMargin = 6 }
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    prefs.setWeatherApiKey(text.toString().trim())
                    fetchWeather()
                }
            }
        }
        root.addView(keyInput)
    }

    private fun fetchWeather() {
        val key  = prefs.getWeatherApiKey()
        val city = prefs.getWeatherCity()
        if (key.isBlank()) {
            statusView.text = "ℹ️ Add your OpenWeatherMap API key above
(free at openweathermap.org)"
            emojiView.text  = "🌡️"; tempView.text = "--°"; descView.text = ""; detailsView.text = ""
            return
        }
        statusView.text = "Fetching…"
        scope.launch {
            val result = WeatherService.fetchWeather(city, key)
            result.onSuccess { showWeather(it) }
            result.onFailure { statusView.text = "⚠️ ${it.message}" }
        }
    }

    private fun showWeather(d: WeatherData) {
        val useMet = prefs.useMetric()
        emojiView.text  = d.emoji
        tempView.text   = if (useMet) "%.0f°C".format(d.tempCelsius) else "%.0f°F".format(d.tempF)
        descView.text   = d.description
        cityView.text   = d.city
        detailsView.text = """
💧 Humidity: ${d.humidity}%
🌡 Feels like: ${if (useMet) "%.0f°C".format(d.feelsLike) else "%.0f°F".format(d.feelsLike * 9/5 + 32)}
💨 Wind: %.1f m/s""".trimIndent().format(d.windSpeed)
        val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        statusView.text = "Updated ${fmt.format(Date())}"
    }

    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); scope.cancel() }
}
