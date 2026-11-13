package com.example.mapmylocation

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mapmylocation.databinding.ActivityMainBinding
import com.example.mapmylocation.location.LocationManager
import com.example.mapmylocation.maps.MapManager
import com.example.mapmylocation.music.MusicManager
import com.example.mapmylocation.routing.RouteRepository
import com.example.mapmylocation.weather.WeatherCategory
import com.example.mapmylocation.weather.WeatherRepository
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var locationManager: LocationManager
    private val mapManager = MapManager()
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var routeRepository: RouteRepository
    private lateinit var musicManager: MusicManager

    private var currentLocation: Location? = null
    private val defaultFallbackLocation = LatLng(48.8566, 2.3522) // Paris as default location if GPS off

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchUserLocation()
        } else {
            Toast.makeText(this, "Location permission denied. Using default location.", Toast.LENGTH_LONG).show()
            useDefaultLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Managers and Repositories
        locationManager = LocationManager(this)
        weatherRepository = WeatherRepository()
        routeRepository = RouteRepository(this)
        musicManager = MusicManager(this)

        setupMusicPlayerCallbacks()
        setupUIListeners()
        setupMapFragment()
    }

    private fun setupMapFragment() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(OnMapReadyCallback { map ->
            mapManager.attachGoogleMap(map)

            if (locationManager.hasLocationPermission()) {
                fetchUserLocation()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        })
    }

    private fun fetchUserLocation() {
        showLoading(true)
        locationManager.getCurrentLocation(
            onSuccess = { loc ->
                showLoading(false)
                currentLocation = loc
                val currentLatLng = LatLng(loc.latitude, loc.longitude)
                mapManager.updateCurrentLocationMarker(currentLatLng, "Your Location")
            },
            onFailure = {
                showLoading(false)
                useDefaultLocation()
            }
        )
    }

    private fun useDefaultLocation() {
        val defaultLoc = Location("default").apply {
            latitude = defaultFallbackLocation.latitude
            longitude = defaultFallbackLocation.longitude
        }
        currentLocation = defaultLoc
        mapManager.updateCurrentLocationMarker(defaultFallbackLocation, "Default Location (Paris)")
    }

    private fun setupUIListeners() {
        binding.btnSearch.setOnClickListener {
            performSearch()
        }

        binding.etDestination.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        binding.btnMyLocation.setOnClickListener {
            currentLocation?.let { loc ->
                val latLng = LatLng(loc.latitude, loc.longitude)
                mapManager.updateCurrentLocationMarker(latLng, "Your Location")
            } ?: fetchUserLocation()
        }

        // Music Control Buttons
        binding.btnPlayPause.setOnClickListener {
            musicManager.togglePlayPause()
        }

        binding.btnStop.setOnClickListener {
            musicManager.stop()
        }

        binding.btnNext.setOnClickListener {
            musicManager.skipNext()
        }

        binding.btnPrev.setOnClickListener {
            musicManager.skipPrevious()
        }
    }

    private fun performSearch() {
        val query = binding.etDestination.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a destination name", Toast.LENGTH_SHORT).show()
            return
        }

        val originLatLng = currentLocation?.let { LatLng(it.latitude, it.longitude) } ?: defaultFallbackLocation

        showLoading(true)
        lifecycleScope.launch {
            // 1. Geocode Destination
            val geocodeResult = routeRepository.geocodeDestination(query)
            geocodeResult.onSuccess { (address, destLatLng) ->
                val destinationName = address.locality ?: address.featureName ?: query
                binding.tvDestinationTitle.text = "Destination: $destinationName"
                mapManager.updateDestinationMarker(destLatLng, destinationName)

                // 2. Fetch Route (Google Directions / OSRM fallback)
                val routeResult = routeRepository.fetchRoute(
                    origin = originLatLng,
                    destination = destLatLng,
                    originName = "Current Location",
                    destinationName = destinationName
                )

                routeResult.onSuccess { routeInfo ->
                    binding.tvDistance.text = "${routeInfo.distanceKm} km"
                    binding.tvDuration.text = "${routeInfo.durationMinutes} mins"
                    mapManager.drawRoutePolyline(routeInfo.polylinePoints)
                }.onFailure {
                    Toast.makeText(this@MainActivity, "Unable to calculate route polyline", Toast.LENGTH_SHORT).show()
                }

                // 3. Fetch Destination Weather
                val weatherResult = weatherRepository.fetchWeatherForCoordinates(
                    lat = destLatLng.latitude,
                    lon = destLatLng.longitude,
                    locationName = destinationName
                )

                weatherResult.onSuccess { weatherData ->
                    updateWeatherUI(weatherData)

                    // 4. Trigger Weather-Based Music Player
                    musicManager.playForWeatherCategory(weatherData.category)
                }.onFailure { e ->
                    Toast.makeText(this@MainActivity, "Weather fetch error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }

                showLoading(false)
            }.onFailure { e ->
                showLoading(false)
                Toast.makeText(this@MainActivity, "Destination error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateWeatherUI(weather: com.example.mapmylocation.weather.WeatherData) {
        binding.tvTemperature.text = "${weather.temperatureC} °C"
        binding.tvWeatherCondition.text = "Condition: ${weather.conditionTitle}"
        binding.tvWeatherDetails.text = "Humidity: ${weather.humidityPercent}% | Wind: ${weather.windSpeedKmH} km/h"

        val iconRes = when (weather.category) {
            WeatherCategory.SUNNY -> R.drawable.ic_weather_sunny
            WeatherCategory.CLOUDY -> R.drawable.ic_weather_cloudy
            WeatherCategory.RAINY -> R.drawable.ic_weather_rainy
            WeatherCategory.STORMY -> R.drawable.ic_weather_stormy
            WeatherCategory.SNOWY -> R.drawable.ic_weather_snowy
            WeatherCategory.UNKNOWN -> R.drawable.ic_weather_sunny
        }
        binding.ivWeatherIcon.setImageResource(iconRes)
    }

    private fun setupMusicPlayerCallbacks() {
        musicManager.onStatusChanged = { status, trackTitle, categoryName, isPlaying ->
            binding.tvMusicCategory.text = "Category: $categoryName"
            binding.tvTrackTitle.text = "Track: $trackTitle"
            binding.tvPlaybackStatus.text = "Status: $status"

            if (isPlaying) {
                binding.btnPlayPause.setIconResource(R.drawable.ic_pause)
            } else {
                binding.btnPlayPause.setIconResource(R.drawable.ic_play)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        musicManager.release()
    }
}
