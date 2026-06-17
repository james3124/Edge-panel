package com.edgepanel.app.model

data class WeatherData(
    val city: String,
    val tempCelsius: Double,
    val description: String,
    val humidity: Int,
    val iconCode: String,
    val feelsLike: Double,
    val windSpeed: Double
) {
    val tempF: Double get() = tempCelsius * 9 / 5 + 32
    val emoji: String get() = when {
        iconCode.startsWith("01") -> if (iconCode.endsWith("d")) "☀️" else "🌙"
        iconCode.startsWith("02") -> "⛅"
        iconCode.startsWith("03") || iconCode.startsWith("04") -> "☁️"
        iconCode.startsWith("09") || iconCode.startsWith("10") -> "🌧️"
        iconCode.startsWith("11") -> "⛈️"
        iconCode.startsWith("13") -> "❄️"
        iconCode.startsWith("50") -> "🌫️"
        else -> "🌡️"
    }
}
