package com.example.mapmylocation.weather

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true,
        @Query("hourly") hourly: String = "relative_humidity_2m"
    ): Response<OpenMeteoResponse>

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
    }
}
