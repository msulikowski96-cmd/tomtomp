# tomtomp

Android app (Kotlin, Jetpack Compose) using the TomTom Maps and Navigation SDK 2.6.4.

## Setup
1. Get an API key at https://my.tomtom.com (Maps and Navigation SDK for Android).
2. Put it in `~/.gradle/gradle.properties` as `tomtomApiKey=YOUR_KEY` (or edit `gradle.properties`).
3. Open in Android Studio (JDK 17, AGP 8.13.2) and run on an arm64-v8a / x86_64 device or emulator (Vulkan 1.0, API 26+).

The SDK is initialized once in `TomTomApp` (off the main thread); `MainActivity` shows the map
centered on Warsaw and requests location permission. `TomTomSdk.navigation`, `createSearch()`
and `createRoutePlanner()` are available after initialization for further features.
