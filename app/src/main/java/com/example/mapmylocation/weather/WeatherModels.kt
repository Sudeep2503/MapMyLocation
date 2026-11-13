package com.example.mapmylocation.weather

import com.google.gson.annotations.SerializedName

/**
 * Weather condition classification categories.
 */
enum class WeatherCategory {
    SUNNY,
    CLOUDY,
    RAINY,
    STORMY,
    SNOWY,
    UNKNOWN
}

/**
 * Domain model representing simplified weather state.
 */
data class WeatherData(
    val temperatureC: Double,
    val weatherCode: Int,
    val conditionTitle: String,
    val category: WeatherCategory,
    val humidityPercent: Int,
    val windSpeedKmH: Double,
    val locationName: String
)

/**
 * Open-Meteo API Response wrapper.
 */
data class OpenMeteoResponse(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("current_weather") val currentWeather: CurrentWeatherDto?,
    @SerializedName("hourly") val hourly: HourlyDto?
)

data class CurrentWeatherDto(
    @SerializedName("temperature") val temperature: Double,
    @SerializedName("windspeed") val windSpeed: Double,
    @SerializedName("weathercode") val weatherCode: Int,
    @SerializedName("is_day") val isDay: Int,
    @SerializedName("time") val time: String?
)

data class HourlyDto(
    @SerializedName("time") val time: List<String>?,
    @SerializedName("relative_humidity_2m") val relativeHumidity2m: List<Int>?
)
