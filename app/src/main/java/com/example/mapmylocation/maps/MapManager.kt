package com.example.mapmylocation.maps

import android.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

class MapManager {

    private var googleMap: GoogleMap? = null
    private var currentLocationMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var currentPolyline: Polyline? = null

    fun attachGoogleMap(map: GoogleMap) {
        this.googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isCompassEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = false
    }

    fun updateCurrentLocationMarker(locationLatLng: LatLng, title: String = "Your Location") {
        currentLocationMarker?.remove()
        val markerOptions = MarkerOptions()
            .position(locationLatLng)
            .title(title)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        
        currentLocationMarker = googleMap?.addMarker(markerOptions)
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 13f))
    }

    fun updateDestinationMarker(destinationLatLng: LatLng, title: String) {
        destinationMarker?.remove()
        val markerOptions = MarkerOptions()
            .position(destinationLatLng)
            .title(title)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        
        destinationMarker = googleMap?.addMarker(markerOptions)
    }

    fun drawRoutePolyline(points: List<LatLng>) {
        currentPolyline?.remove()
        if (points.isEmpty()) return

        val polylineOptions = PolylineOptions()
            .addAll(points)
            .width(12f)
            .color(Color.parseColor("#2563EB"))
            .geodesic(true)

        currentPolyline = googleMap?.addPolyline(polylineOptions)
        fitBoundsForPoints(points)
    }

    private fun fitBoundsForPoints(points: List<LatLng>) {
        if (points.isEmpty() || googleMap == null) return

        val builder = LatLngBounds.Builder()
        for (point in points) {
            builder.include(point)
        }
        val bounds = builder.build()
        val padding = 120 // pixels padding from border

        try {
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            googleMap?.animateCamera(cameraUpdate)
        } catch (_: Exception) {
            if (points.isNotEmpty()) {
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(points[0], 12f))
            }
        }
    }

    fun clearMap() {
        currentLocationMarker?.remove()
        destinationMarker?.remove()
        currentPolyline?.remove()
        googleMap?.clear()
    }
}
