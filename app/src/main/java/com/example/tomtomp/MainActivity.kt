package com.example.tomtomp

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tomtom.sdk.init.TomTomSdk
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.MapLocationInfrastructure
import com.tomtom.sdk.map.display.camera.InitialCameraOptions
import com.tomtom.sdk.map.display.compose.TomTomMap
import com.tomtom.sdk.map.display.compose.model.MapDisplayInfrastructure
import com.tomtom.sdk.map.display.compose.state.rememberMapViewState

private val WARSAW = GeoPoint(latitude = 52.2297, longitude = 21.0122)
private const val INITIAL_ZOOM = 12.0

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TomTomApp
        setContent {
            val state by app.sdkState.collectAsState()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (val s = state) {
                    SdkState.Initializing -> CircularProgressIndicator()
                    is SdkState.Failed -> Text("SDK init failed: ${s.message}")
                    SdkState.Ready -> MapScreen()
                }
            }
        }
    }
}

@Composable
private fun MapScreen() {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* the map picks up the location provider once permission is granted */ }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    val infrastructure = remember {
        MapDisplayInfrastructure(sdkContext = TomTomSdk.sdkContext) {
            locationInfrastructure = MapLocationInfrastructure {
                locationProvider = TomTomSdk.locationProvider
            }
        }
    }
    val mapViewState = rememberMapViewState(
        initialCameraOptions = InitialCameraOptions.LocationBased(
            position = WARSAW,
            zoom = INITIAL_ZOOM,
        ),
    )

    TomTomMap(
        state = mapViewState,
        infrastructure = infrastructure,
        modifier = Modifier.fillMaxSize(),
    )
}
