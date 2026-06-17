package com.edgepanel.app.api

import com.edgepanel.app.model.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object WeatherService {

    private val client = OkHttpClient()

    suspend fun fetchWeather(city: String, apiKey: String): Result<WeatherData> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.openweathermap.org/data/2.5/weather" +
                    "?q=${city.trim()}&appid=$apiKey&units=metric"
                val req  = Request.Builder().url(url).build()
                val body = client.newCall(req).execute().use { it.body?.string() }
                    ?: return@withContext Result.failure(Exception("Empty response"))

                val json  = JSONObject(body)
                if (json.has("cod") && json.getInt("cod") != 200) {
                    return@withContext Result.failure(Exception(json.optString("message", "API error")))
                }

                val main    = json.getJSONObject("main")
                val weather = json.getJSONArray("weather").getJSONObject(0)
                val wind    = json.optJSONObject("wind")

                Result.success(WeatherData(
                    city        = json.optString("name", city),
                    tempCelsius = main.getDouble("temp"),
                    description = weather.getString("description").replaceFirstChar { it.uppercase() },
                    humidity    = main.getInt("humidity"),
                    iconCode    = weather.getString("icon"),
                    feelsLike   = main.getDouble("feels_like"),
                    windSpeed   = wind?.optDouble("speed", 0.0) ?: 0.0
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
