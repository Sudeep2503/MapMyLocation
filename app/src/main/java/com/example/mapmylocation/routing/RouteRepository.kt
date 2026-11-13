package com.example.mapmylocation.routing

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RouteRepository(private val context: Context) {

    private val googleDirectionsApi: GoogleDirectionsApi by lazy {
        Retrofit.Builder()
            .baseUrl(GoogleDirectionsApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleDirectionsApi::class.java)
    }

    private val osrmRoutingApi: OsrmRoutingApi by lazy {
        Retrofit.Builder()
            .baseUrl(OsrmRoutingApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OsrmRoutingApi::class.java)
    }

    /**
     * Geocodes a destination string into LatLng coordinates.
     */
    suspend fun geocodeDestination(destinationQuery: String): Result<Pair<Address, LatLng>> = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCoroutine { continuation ->
                    geocoder.getFromLocationName(destinationQuery, 1) { addresses ->
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val latLng = LatLng(address.latitude, address.longitude)
                            continuation.resume(Result.success(Pair(address, latLng)))
                        } else {
                            continuation.resume(Result.failure(Exception("Destination not found: '$destinationQuery'")))
                        }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(destinationQuery, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val latLng = LatLng(address.latitude, address.longitude)
                    Result.success(Pair(address, latLng))
                } else {
                    Result.failure(Exception("Destination not found: '$destinationQuery'"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculates route between origin and destination coordinates.
     * Uses Google Directions API first, falling back to OSRM API if necessary.
     */
    suspend fun fetchRoute(
        origin: LatLng,
        destination: LatLng,
        originName: String = "Current Location",
        destinationName: String = "Destination",
        googleApiKey: String = ""
    ): Result<RouteInfo> = withContext(Dispatchers.IO) {
        // 1. Try Google Directions API if key provided
        if (googleApiKey.isNotBlank() && googleApiKey != "YOUR_GOOGLE_MAPS_API_KEY_HERE") {
            try {
                val originStr = "${origin.latitude},${origin.longitude}"
                val destStr = "${destination.latitude},${destination.longitude}"
                val response = googleDirectionsApi.getDirections(originStr, destStr, googleApiKey)
                if (response.isSuccessful && response.body() != null) {
                    val googleResp = response.body()!!
                    if (googleResp.status == "OK" && !googleResp.routes.isNullOrEmpty()) {
                        val route = googleResp.routes[0]
                        val polylineStr = route.overviewPolyline?.points ?: ""
                        val points = PolylineDecoder.decode(polylineStr)

                        val leg = route.legs?.firstOrNull()
                        val distMeters = leg?.distance?.value ?: 0L
                        val durSecs = leg?.duration?.value ?: 0L

                        val distKm = distMeters / 1000.0
                        val durMins = (durSecs / 60.0).toInt().coerceAtLeast(1)

                        val routeInfo = RouteInfo(
                            distanceKm = String.format(Locale.US, "%.1f", distKm).toDouble(),
                            durationMinutes = durMins,
                            polylinePoints = points,
                            originName = originName,
                            destinationName = destinationName,
                            destinationLat = destination.latitude,
                            destinationLng = destination.longitude
                        )
                        return@withContext Result.success(routeInfo)
                    }
                }
            } catch (_: Exception) {
                // Fall back to OSRM on error
            }
        }

        // 2. Try OSRM Routing API (Open Source, No Key Required)
        try {
            val osrmCoords = "${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}"
            val osrmResp = osrmRoutingApi.getRoute(osrmCoords)
            if (osrmResp.isSuccessful && osrmResp.body() != null) {
                val body = osrmResp.body()!!
                if (body.code == "Ok" && !body.routes.isNullOrEmpty()) {
                    val route = body.routes[0]
                    val points = PolylineDecoder.decode(route.geometry ?: "")
                    val distKm = (route.distanceMeters ?: 0.0) / 1000.0
                    val durMins = ((route.durationSeconds ?: 0.0) / 60.0).toInt().coerceAtLeast(1)

                    val routeInfo = RouteInfo(
                        distanceKm = String.format(Locale.US, "%.1f", distKm).toDouble(),
                        durationMinutes = durMins,
                        polylinePoints = if (points.isNotEmpty()) points else listOf(origin, destination),
                        originName = originName,
                        destinationName = destinationName,
                        destinationLat = destination.latitude,
                        destinationLng = destination.longitude
                    )
                    return@withContext Result.success(routeInfo)
                }
            }
        } catch (_: Exception) {
            // Fall back to direct estimation
        }

        // 3. Fallback: Direct Haversine Distance & Straight Polyline
        val distKm = calculateHaversineDistanceKm(origin, destination)
        val durMins = (distKm / 50.0 * 60.0).toInt().coerceAtLeast(1) // Average 50 km/h driving speed
        val routeInfo = RouteInfo(
            distanceKm = String.format(Locale.US, "%.1f", distKm).toDouble(),
            durationMinutes = durMins,
            polylinePoints = listOf(origin, destination),
            originName = originName,
            destinationName = destinationName,
            destinationLat = destination.latitude,
            destinationLng = destination.longitude
        )
        Result.success(routeInfo)
    }

    private fun calculateHaversineDistanceKm(start: LatLng, end: LatLng): Double {
        val r = 6371.0 // Earth radius in kilometers
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon2 = Math.toRadians(end.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
