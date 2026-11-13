# MapMyLocation – Weather-Based Route and Music Android Application
## Sudeep J S (25MCAR0134)

**MapMyLocation** is a modern Android application built in Kotlin that seamlessly integrates location detection, interactive mapping, destination routing, live weather forecasting, and weather-driven automatic music recommendations.

---

## 🌟 Application Features

1. **Google Maps Integration**:
   - Interactive Google Map with zoom controls, custom markers, and camera animations.
   - Detects current user location using Google Play Services `FusedLocationProviderClient`.
2. **Destination Search & Genuine Route Generation**:
   - Search by address, city, or landmark.
   - Genuine polyline route drawing between origin and destination.
   - Primary **Google Directions API** routing with automatic fallback to **OSRM (Open Source Routing Machine)**.
   - Displays total distance (km) and estimated travel duration (minutes).
3. **Live Weather Forecasting (Open-Meteo API)**:
   - Retrieves real-time weather metrics for destination coordinates without requiring an API key.
   - Displays temperature (°C), condition description, humidity (%), and wind speed (km/h).
   - Weather condition classification into 5 main categories: `SUNNY`, `CLOUDY`, `RAINY`, `STORMY`, and `SNOWY`.
4. **Weather-Based Automatic Music Player**:
   - Automatically recommends and plays music tracks tailored to the destination weather condition:
     - **Sunny** $\rightarrow$ Energetic Music
     - **Cloudy** $\rightarrow$ Chill Music
     - **Rainy** $\rightarrow$ Relaxing Music
     - **Stormy** $\rightarrow$ Calm Instrumental Music
     - **Snowy** $\rightarrow$ Peaceful Music
   - Media controls: **Play**, **Pause**, **Resume**, **Stop**, **Next**, and **Previous**.
   - Live track title, weather category badge, and playback status indicator.

---

## 🛠️ Technology Stack

| Component | Technology / Library |
|---|---|
| **IDE** | Android Studio |
| **Language** | Kotlin |
| **UI Framework** | XML Layouts & Material Design 3 |
| **Minimum SDK** | API 24 (Android 7.0) |
| **Target / Compile SDK** | API 37 |
| **Maps SDK** | `com.google.android.gms:play-services-maps:19.0.0` |
| **Location** | `com.google.android.gms:play-services-location:21.3.0` |
| **Routing** | Google Directions API + OSRM Fallback Engine |
| **Weather API** | Open-Meteo REST API (`https://api.open-meteo.com/`) |
| **Networking** | Retrofit 2 + Gson Converter + OkHttp |
| **Audio Playback** | Android `MediaPlayer` |

---

## 📁 Project Folder Structure

```
app/src/main/
├── java/com/example/mapmylocation/
│   ├── MainActivity.kt                 # Main Dashboard Controller
│   ├── maps/
│   │   └── MapManager.kt               # Google Maps setup, markers, polyline drawing
│   ├── location/
│   │   └── LocationManager.kt          # FusedLocationProviderClient & runtime permissions
│   ├── weather/
│   │   ├── WeatherApi.kt               # Retrofit client for Open-Meteo API
│   │   ├── WeatherModels.kt            # Weather JSON models & WeatherCategory enum
│   │   └── WeatherRepository.kt        # Weather fetcher & WMO code classifier
│   ├── routing/
│   │   ├── DirectionsApi.kt            # Google Directions & OSRM Retrofit services
│   │   ├── PolylineDecoder.kt          # Google Polyline decoder utility
│   │   ├── RouteModels.kt             # Route response DTOs and RouteInfo model
│   │   └── RouteRepository.kt         # Address Geocoding & Route calculation engine
│   └── music/
│       ├── MusicRecommendation.kt     # Weather-to-audio track mapping
│       └── MusicManager.kt             # Android MediaPlayer lifecycle & control wrapper
├── res/
│   ├── drawable/                       # Vector icons (weather, search, location, media player)
│   ├── layout/
│   │   └── activity_main.xml           # Dashboard XML layout with Material3 cards
│   └── raw/                            # Audio tracks (sunny, cloudy, rainy, calm, peaceful)
└── AndroidManifest.xml
```

---

## 🔑 API Configuration & Setup Instructions

### 1. Google Maps API Key Setup
1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Select your project (e.g., `Experiment11GoogleMaps`, Project ID: `xenon-set-513206-d3`).
3. Enable **Maps SDK for Android** under **APIs & Services**.
4. Create an API Key under **Credentials**.
5. Open `app/src/main/AndroidManifest.xml` and replace `YOUR_GOOGLE_MAPS_API_KEY_HERE` with your key:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_ACTUAL_GOOGLE_MAPS_API_KEY" />
   ```

### 2. Weather & Routing APIs
- **Weather (Open-Meteo)**: No API key required. Works automatically for any latitude/longitude coordinates.
- **Routing**: Features a dual-provider model. If Google Directions API key is restricted or unbilled, it automatically falls back to OSRM (Open Source Routing Machine).

---

## 🧪 Testing Scenarios

1. **Launch & Permissions**: Open the application and grant location permissions when prompted. The Google Map will automatically zoom to your current location.
2. **Destination Search**: Type a city name (e.g. `Paris`, `New York`, `London`) in the top search bar and tap **Search**.
3. **Route & Map Marker**: Verify destination marker placement and genuine blue route polyline drawing on the map.
4. **Distance & Duration**: Check Route Information card for distance in kilometers and estimated travel time in minutes.
5. **Weather Retrieval**: Confirm live weather details (Temperature, Humidity, Wind speed, and Condition Icon) update for the destination.
6. **Weather-Based Music Selection**: Observe automatic audio track selection matching the weather condition (e.g. Sunny $\rightarrow$ Energetic, Rainy $\rightarrow$ Relaxing).
7. **Music Controls**: Use **Play/Pause**, **Stop**, **Next**, and **Previous** buttons to verify playback control and status label updates.
