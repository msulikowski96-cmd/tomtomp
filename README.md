# tomtomp

Android app (Kotlin, Jetpack Compose) built on the TomTom Maps and Navigation SDK 2.6.4.

## Features (mapped to the SDK guides)
- **Map display (Compose):** `TomTomMap`, markers, routes, traffic flow/incidents, day/night style, hill shading, camera animations, long-press to pick a point.
- **Location:** `TomTomSdk.locationProvider`, current-location marker, permission handling, map-matched provider during guidance, simulation provider for demo drives.
- **Search:** autocomplete suggestions and text search biased to the user's position; reverse geocoding on long-press.
- **Routing:** route planning with up to 2 alternatives, route type, avoid tolls/motorways, route selection on the map.
- **Navigation:** `TomTomNavigation` turn-by-turn guidance (instructions, distance to maneuver, remaining distance/time, arrival), camera follows the route.
- **Virtual horizon:** `HorizonUpdatedListener` shows upcoming road elements (speed limits, traffic signs, hazards, road properties).
- **Personalization:** language, units, route preferences, map style.
- **Advanced:** SDK failures flow (`TomTomSdk.failures`), simulation toggle.

Not included: offline maps (NDS / `getOfflineRegionStore`) and spoken guidance (no TTS engine is wired up); the app uses the online-with-local-cache configuration.

## Setup
1. Get an API key at https://my.tomtom.com (Maps and Navigation SDK for Android).
2. Put it in `~/.gradle/gradle.properties` as `tomtomApiKey=YOUR_KEY` (or edit `gradle.properties`).
3. Open in Android Studio (JDK 17) and run on an arm64-v8a / x86_64 device (Vulkan 1.0, API 26+), or `./gradlew :app:assembleDebug`.

Telemetry consent is hard-coded to `TelemetryOff` in `TomTomApp`; add a real consent prompt before shipping.
