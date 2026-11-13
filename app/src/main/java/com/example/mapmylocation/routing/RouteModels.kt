package com.example.mapmylocation.routing

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName

/**
 * Domain model containing route metrics and map points.
 */
data class RouteInfo(
    val distanceKm: Double,
    val durationMinutes: Int,
    val polylinePoints: List<LatLng>,
    val originName: String,
    val destinationName: String,
    val destinationLat: Double,
    val destinationLng: Double
)

/**
 * Google Directions API Response Models
 */
data class GoogleDirectionsResponse(
    @SerializedName("routes") val routes: List<GoogleRoute>?,
    @SerializedName("status") val status: String?
)

data class GoogleRoute(
    @SerializedName("overview_polyline") val overviewPolyline: GooglePolyline?,
    @SerializedName("legs") val legs: List<GoogleLeg>?
)

data class GooglePolyline(
    @SerializedName("points") val points: String?
)

data class GoogleLeg(
    @SerializedName("distance") val distance: GoogleTextValue?,
    @SerializedName("duration") val duration: GoogleTextValue?
)

data class GoogleTextValue(
    @SerializedName("text") val text: String?,
    @SerializedName("value") val value: Long? // meters / seconds
)

/**
 * OSRM API Response Models (Open Source Routing Machine Fallback)
 */
data class OsrmResponse(
    @SerializedName("code") val code: String?,
    @SerializedName("routes") val routes: List<OsrmRoute>?
)

data class OsrmRoute(
    @SerializedName("geometry") val geometry: String?,
    @SerializedName("distance") val distanceMeters: Double?,
    @SerializedName("duration") val durationSeconds: Double?
)
