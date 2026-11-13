package com.example.mapmylocation.music

import com.example.mapmylocation.R
import com.example.mapmylocation.weather.WeatherCategory

data class TrackInfo(
    val resId: Int,
    val title: String,
    val categoryName: String,
    val weatherCategory: WeatherCategory
)

object MusicRecommendation {

    private val musicMap: Map<WeatherCategory, List<TrackInfo>> = mapOf(
        WeatherCategory.SUNNY to listOf(
            TrackInfo(R.raw.sunny_1, "Sunny Beats - Energetic Track 1", "Energetic Music", WeatherCategory.SUNNY),
            TrackInfo(R.raw.sunny_2, "Summer Vibes - Energetic Track 2", "Energetic Music", WeatherCategory.SUNNY)
        ),
        WeatherCategory.CLOUDY to listOf(
            TrackInfo(R.raw.cloudy_1, "Cloudy Drift - Chill Track 1", "Chill Music", WeatherCategory.CLOUDY),
            TrackInfo(R.raw.cloudy_2, "Overcast Lounge - Chill Track 2", "Chill Music", WeatherCategory.CLOUDY)
        ),
        WeatherCategory.RAINY to listOf(
            TrackInfo(R.raw.rainy_1, "Raindrops & Coffee - Relaxing Track 1", "Relaxing Music", WeatherCategory.RAINY),
            TrackInfo(R.raw.rainy_2, "Gentle Shower - Relaxing Track 2", "Relaxing Music", WeatherCategory.RAINY)
        ),
        WeatherCategory.STORMY to listOf(
            TrackInfo(R.raw.calm_1, "Thunder Shelter - Calm Instrumental 1", "Calm Instrumental", WeatherCategory.STORMY),
            TrackInfo(R.raw.calm_2, "Deep Waves - Calm Instrumental 2", "Calm Instrumental", WeatherCategory.STORMY)
        ),
        WeatherCategory.SNOWY to listOf(
            TrackInfo(R.raw.peaceful_1, "Winter Glow - Peaceful Track 1", "Peaceful Music", WeatherCategory.SNOWY),
            TrackInfo(R.raw.peaceful_2, "Snowflake Waltz - Peaceful Track 2", "Peaceful Music", WeatherCategory.SNOWY)
        ),
        WeatherCategory.UNKNOWN to listOf(
            TrackInfo(R.raw.sunny_1, "Default Highway - Ambient Track", "Ambient Music", WeatherCategory.UNKNOWN)
        )
    )

    fun getTracksForWeather(category: WeatherCategory): List<TrackInfo> {
        return musicMap[category] ?: musicMap[WeatherCategory.UNKNOWN]!!
    }

    fun getRecommendedTrack(category: WeatherCategory, index: Int = 0): TrackInfo {
        val tracks = getTracksForWeather(category)
        val validIndex = index.coerceIn(0, tracks.size - 1)
        return tracks[validIndex]
    }
}
