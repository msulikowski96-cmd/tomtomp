plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val tomtomApiKey: String by project
val tomtomSdkVersion = "2.6.4"

android {
    namespace = "com.example.tomtomp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tomtomp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        ndk {
            // Pass -PphoneOnly to build a smaller APK for physical arm64 phones.
            abiFilters += if (project.hasProperty("phoneOnly")) listOf("arm64-v8a") else listOf("arm64-v8a", "x86_64")
        }

        // Required by the SDK: choose the "complete" flavor (no private credentials needed).
        missingDimensionStrategy("tomtom-sdk-version", "complete")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Signed with the debug key so the APK can be sideloaded; use a real keystore for the Play Store.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildTypes.configureEach {
        buildConfigField("String", "TOMTOM_API_KEY", "\"$tomtomApiKey\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // TomTom Maps and Navigation SDK
    implementation("com.tomtom.sdk:init:$tomtomSdkVersion")
    implementation("com.tomtom.sdk.maps:map-display-compose-premium:$tomtomSdkVersion")
    implementation("com.tomtom.sdk.location:provider-simulation:$tomtomSdkVersion")
    implementation("com.tomtom.sdk.location:provider-map-matched:$tomtomSdkVersion")
    implementation("com.tomtom.sdk.search:search:$tomtomSdkVersion")
    implementation("com.tomtom.sdk.search:reverse-geocoder:$tomtomSdkVersion")
    implementation("com.tomtom.sdk.routing:route-planner:$tomtomSdkVersion")
    implementation("com.tomtom.sdk.navigation:navigation:$tomtomSdkVersion")
    implementation("com.tomtom.sdk.navigation:ui:$tomtomSdkVersion")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
