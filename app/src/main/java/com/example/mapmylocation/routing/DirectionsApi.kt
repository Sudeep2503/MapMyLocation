package com.example.mapmylocation.routing

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleDirectionsApi {

    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") apiKey: String
    ): Response<GoogleDirectionsResponse>

    companion object {
        const val BASE_URL = "https://maps.googleapis.com/"
    }
}

interface OsrmRoutingApi {

    @GET("route/v1/driving/{coords}")
    suspend fun getRoute(
        @Path("coords") coordinates: String, // "lon1,lat1;lon2,lat2"
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "polyline"
    ): Response<OsrmResponse>

    companion object {
        const val BASE_URL = "https://router.project-osrm.org/"
    }
}
