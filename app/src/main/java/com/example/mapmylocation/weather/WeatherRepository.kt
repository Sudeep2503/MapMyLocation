package com.example.mapmylocation.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRepository {

    private val weatherApi: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl(WeatherApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

    suspend fun fetchWeatherForCoordinates(
        lat: Double,
        lon: Double,
        locationName: String = "Destination"
    ): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            val response = weatherApi.getForecast(latitude = lat, longitude = lon)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val current = body.currentWeather
                val temp = current?.temperature ?: 20.0
                val code = current?.weatherCode ?: 0
                val wind = current?.windSpeed ?: 5.0
                val humidity = body.hourly?.relativeHumidity2m?.firstOrNull() ?: 50

                val category = classifyWeatherCode(code)
                val description = getWeatherDescription(code)

                val data = WeatherData(
                    temperatureC = temp,
                    weatherCode = code,
                    conditionTitle = description,
                    category = category,
                    humidityPercent = humidity,
                    windSpeedKmH = wind,
                    locationName = locationName
                )
                Result.success(data)
            } else {
                Result.failure(Exception("Weather API returned HTTP error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun classifyWeatherCode(code: Int): WeatherCategory {
        return when (code) {
            0 -> WeatherCategory.SUNNY
            1, 2, 3, 45, 48 -> WeatherCategory.CLOUDY
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherCategory.RAINY
            71, 73, 75, 77, 85, 86 -> WeatherCategory.SNOWY
            95, 96, 99 -> WeatherCategory.STORMY
            else -> WeatherCategory.SUNNY
        }
    }

    fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear Sky (Sunny)"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Light Drizzle"
            61, 63, 65 -> "Rain Showers"
            71, 73, 75 -> "Snowfall"
            80, 81, 82 -> "Heavy Rain"
            95, 96, 99 -> "Thunderstorm"
            else -> "Clear Weather"
        }
    }
}
