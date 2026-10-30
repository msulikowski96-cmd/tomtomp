package com.example.tomtomp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tomtomp.ui.MapScreen
import com.example.tomtomp.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TomTomApp
        setContent {
            MaterialTheme {
                val state by app.sdkState.collectAsState()
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (val s = state) {
                        SdkState.Initializing -> CircularProgressIndicator()
                        is SdkState.Failed -> Text("Błąd inicjalizacji SDK: ${s.message}")
                        SdkState.Ready -> AppContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun AppContent() {
    // Created only once the SDK is ready, because it builds search/routing/navigation objects.
    val viewModel: MapViewModel = viewModel()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) viewModel.onLocationPermissionGranted()
    }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onLocationPermissionGranted()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("🗺") }, label = { Text("Mapa") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("⚙") }, label = { Text("Ustawienia") })
            }
        },
    ) { padding ->
        when (tab) {
            0 -> MapScreen(viewModel, Modifier.padding(padding))
            else -> SettingsScreen(viewModel, Modifier.padding(padding))
        }
    }
}
