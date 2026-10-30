package com.example.tomtomp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.tomtomp.MapViewModel
import com.example.tomtomp.R
import com.example.tomtomp.Mode
import com.tomtom.sdk.init.TomTomSdk
import com.tomtom.sdk.location.GeoBounds
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.MapLocationInfrastructure
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.camera.CameraTrackingMode
import com.tomtom.sdk.map.display.camera.InitialCameraOptions
import com.tomtom.sdk.map.display.compose.ExperimentalMapComposableApi
import com.tomtom.sdk.map.display.compose.TomTomMap
import com.tomtom.sdk.map.display.compose.model.MapDisplayInfrastructure
import com.tomtom.sdk.map.display.compose.model.MarkerData
import com.tomtom.sdk.map.display.compose.nodes.CurrentLocationMarker
import com.tomtom.sdk.map.display.compose.nodes.Marker
import com.tomtom.sdk.map.display.compose.nodes.Routes
import com.tomtom.sdk.map.display.compose.nodes.Traffic
import com.tomtom.sdk.map.display.compose.properties.MarkerProperties
import com.tomtom.sdk.map.display.compose.state.rememberMapViewState
import com.tomtom.sdk.map.display.compose.state.rememberTrafficState
import com.tomtom.sdk.map.display.image.ImageFactory
import com.tomtom.sdk.map.display.route.RouteOptions
import com.tomtom.sdk.map.display.style.StyleMode
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private val WARSAW = GeoPoint(latitude = 52.2297, longitude = 21.0122)

@OptIn(ExperimentalMapComposableApi::class)
@Composable
fun MapScreen(viewModel: MapViewModel, modifier: Modifier = Modifier) {
    val settings = viewModel.settings
    val scope = rememberCoroutineScope()

    // The map picks up a new infrastructure when the active location provider changes
    // (default provider while browsing, map-matched provider during guidance).
    val infrastructure = remember(viewModel.locationProvider) {
        MapDisplayInfrastructure(sdkContext = TomTomSdk.sdkContext) {
            locationInfrastructure = MapLocationInfrastructure {
                locationProvider = viewModel.locationProvider
            }
        }
    }
    val mapViewState = rememberMapViewState(
        initialCameraOptions = InitialCameraOptions.LocationBased(position = WARSAW, zoom = 12.0),
    )
    val pinImage = remember { ImageFactory.fromResource(R.drawable.ic_pin) }
    val trafficState = rememberTrafficState(
        showTrafficFlow = settings.showTraffic,
        showTrafficIncidents = settings.showTraffic,
    )

    // Map style, relief and language follow the personalization settings.
    LaunchedEffect(settings.nightMap) {
        mapViewState.styleState.styleMode = if (settings.nightMap) StyleMode.DARK else StyleMode.MAIN
    }
    LaunchedEffect(settings.hillShading) { mapViewState.styleState.showHillShading = settings.hillShading }
    LaunchedEffect(settings.language) { mapViewState.locale = settings.language }

    // Camera behaviour per mode.
    LaunchedEffect(viewModel.mode) {
        mapViewState.cameraState.trackingMode = when (viewModel.mode) {
            Mode.Navigating -> CameraTrackingMode.FollowRouteDirection
            else -> CameraTrackingMode.None
        }
    }
    LaunchedEffect(viewModel.selected) {
        viewModel.selected?.let { place ->
            mapViewState.cameraState.animateCamera(
                CameraOptions(position = place.point, zoom = 15.0),
            )
        }
    }
    LaunchedEffect(viewModel.routes) {
        val route = viewModel.activeRoute?.route ?: return@LaunchedEffect
        if (viewModel.mode == Mode.RoutePreview && route.geometry.size > 1) {
            mapViewState.cameraState.animateCamera(
                factory = { lookAt(GeoBounds(route.geometry), null, null) },
            )
        }
    }

    Box(modifier.fillMaxSize()) {
        TomTomMap(
            state = mapViewState,
            infrastructure = infrastructure,
            modifier = Modifier.fillMaxSize(),
            onMapLongClick = { point -> if (viewModel.mode != Mode.Navigating) viewModel.selectPoint(point) },
        ) {
            CurrentLocationMarker()
            Traffic(state = trafficState)

            viewModel.selected?.let { place ->
                Marker(
                    data = MarkerData(geoPoint = place.point) { tag = "selected" },
                    properties = MarkerProperties(pinImage = pinImage) { balloonText = place.name },
                )
            }

            if (viewModel.routes.isNotEmpty()) {
                val options = remember(viewModel.routes, viewModel.activeRouteIndex, viewModel.routeProgressMeters) {
                    viewModel.routes.mapIndexed { index, planned ->
                        val active = index == viewModel.activeRouteIndex
                        RouteOptions(
                            geometry = planned.route.geometry,
                            color = if (active) RouteOptions.DEFAULT_COLOR else ALTERNATIVE_ROUTE_COLOR,
                            departureMarkerVisible = active,
                            destinationMarkerVisible = active,
                            tag = index.toString(),
                        )
                    }
                }
                Routes(
                    options = options,
                    onClick = { clicked -> clicked.tag?.toIntOrNull()?.let(viewModel::selectRoute) },
                )
            }
        }

        MapOverlays(
            viewModel = viewModel,
            onRecenter = {
                scope.launch {
                    mapViewState.cameraState.animateCamera(
                        CameraOptions(position = viewModel.userPosition(), zoom = 15.0),
                    )
                }
            },
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

private val ALTERNATIVE_ROUTE_COLOR = androidx.compose.ui.graphics.Color(0xFF8E9AA8).toArgb()
