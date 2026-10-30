package com.example.tomtomp

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomtom.sdk.common.Cancellable
import com.tomtom.sdk.common.measures.UnitSystem as SdkUnitSystem
import com.tomtom.sdk.init.TomTomSdk
import com.tomtom.sdk.init.createReverseGeocoder
import com.tomtom.sdk.init.createRoutePlanner
import com.tomtom.sdk.init.createSearch
import com.tomtom.sdk.location.GeoLocation
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.location.LocationProvider
import com.tomtom.sdk.location.mapmatched.MapMatchedLocationProviderFactory
import com.tomtom.sdk.location.simulation.SimulationLocationProvider
import com.tomtom.sdk.location.simulation.strategy.InterpolationStrategy
import com.tomtom.sdk.navigation.DestinationArrivalListener
import com.tomtom.sdk.navigation.GuidanceUpdatedListener
import com.tomtom.sdk.navigation.HorizonUpdatedListener
import com.tomtom.sdk.navigation.NavigationOptions
import com.tomtom.sdk.navigation.ProgressUpdatedListener
import com.tomtom.sdk.navigation.RoutePlan
import com.tomtom.sdk.navigation.UnitSystemType
import com.tomtom.sdk.navigation.guidance.GuidanceAnnouncement
import com.tomtom.sdk.navigation.guidance.InstructionPhase
import com.tomtom.sdk.navigation.guidance.instruction.GuidanceInstruction
import com.tomtom.sdk.navigation.horizon.HorizonOptions
import com.tomtom.sdk.navigation.horizon.elements.generalroadproperties.GeneralRoadPropertiesElementType
import com.tomtom.sdk.navigation.horizon.elements.hazard.HazardElementType
import com.tomtom.sdk.navigation.horizon.elements.speedlimit.SpeedLimitElementType
import com.tomtom.sdk.navigation.horizon.elements.trafficsign.TrafficSignElementType
import com.tomtom.sdk.navigation.horizon.HorizonPosition
import com.tomtom.sdk.navigation.horizon.HorizonSnapshot
import com.tomtom.sdk.navigation.progress.RouteProgress
import com.tomtom.sdk.routing.RoutePlanningCallback
import com.tomtom.sdk.routing.RoutePlanningResponse
import com.tomtom.sdk.routing.RoutingFailure
import com.tomtom.sdk.routing.buildRoutePlanningOptions
import com.tomtom.sdk.routing.options.Itinerary
import com.tomtom.sdk.routing.options.RoutePlanningOptions
import com.tomtom.sdk.routing.options.calculation.AvoidType
import com.tomtom.sdk.routing.options.calculation.RouteType
import com.tomtom.sdk.routing.route.Route
import com.tomtom.sdk.search.SearchCallback
import com.tomtom.sdk.search.SearchResponse
import com.tomtom.sdk.search.autocomplete.AutocompleteCallback
import com.tomtom.sdk.search.autocomplete.AutocompleteResponse
import com.tomtom.sdk.search.buildAutocompleteOptions
import com.tomtom.sdk.search.buildTextSearchOptions
import com.tomtom.sdk.search.common.error.SearchFailure
import com.tomtom.sdk.search.model.result.AutocompleteSegmentBrand
import com.tomtom.sdk.search.model.result.AutocompleteSegmentPlainText
import com.tomtom.sdk.search.model.result.AutocompleteSegmentPoiCategory
import com.tomtom.sdk.search.model.result.SearchResult
import com.tomtom.sdk.search.reversegeocoder.ReverseGeocoderCallback
import com.tomtom.sdk.search.reversegeocoder.ReverseGeocoderResponse
import com.tomtom.sdk.search.reversegeocoder.buildReverseGeocoderOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "MapViewModel"
private val DEFAULT_CENTER = GeoPoint(latitude = 52.2297, longitude = 21.0122)

enum class Mode { Browse, RoutePreview, Navigating }

enum class UnitSystem(val label: String) { Metric("Metryczne"), ImperialUs("Imperialne (US)"), ImperialUk("Imperialne (UK)") }

data class SelectedPlace(val name: String, val address: String?, val point: GeoPoint)

data class PlannedRoute(val route: Route, val options: RoutePlanningOptions)

data class GuidanceState(
    val maneuver: String = "",
    val road: String = "",
    val distanceToNextMeters: Double? = null,
    val remainingMeters: Double? = null,
    val remainingSeconds: Long? = null,
    val speedKmh: Double? = null,
)

data class HorizonInfo(val offsetMeters: Double, val elementsAhead: List<String>, val onRoad: Boolean)

/** User-facing options for personalization and advanced settings. */
class AppSettings {
    var simulateLocation by mutableStateOf(true)
    var showTraffic by mutableStateOf(true)
    var nightMap by mutableStateOf(false)
    var hillShading by mutableStateOf(false)
    var unitSystem by mutableStateOf(UnitSystem.Metric)
    var language by mutableStateOf(Locale("pl", "PL"))
    var avoidTolls by mutableStateOf(false)
    var avoidMotorways by mutableStateOf(false)
    var routeType by mutableStateOf<RouteType>(RouteType.Fast)
    var maxAlternatives by mutableStateOf(2)
}

class MapViewModel : ViewModel() {
    private val search = TomTomSdk.createSearch()
    private val reverseGeocoder = TomTomSdk.createReverseGeocoder()
    private val routePlanner = TomTomSdk.createRoutePlanner()
    private val navigation = TomTomSdk.navigation

    val settings = AppSettings()

    // Search
    var query by mutableStateOf("")
        private set
    var suggestions by mutableStateOf<List<String>>(emptyList())
        private set
    var results by mutableStateOf<List<SearchResult>>(emptyList())
        private set

    // Map selection / routes
    var selected by mutableStateOf<SelectedPlace?>(null)
        private set
    var routes by mutableStateOf<List<PlannedRoute>>(emptyList())
        private set
    var activeRouteIndex by mutableStateOf(0)
        private set
    var mode by mutableStateOf(Mode.Browse)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    // Navigation / guidance
    var guidance by mutableStateOf(GuidanceState())
        private set
    var arrived by mutableStateOf(false)
        private set
    var locationProvider: LocationProvider by mutableStateOf(TomTomSdk.locationProvider)
        private set
    var routeProgressMeters by mutableStateOf(0.0)
        private set

    // Virtual horizon
    var horizon by mutableStateOf<HorizonInfo?>(null)
        private set

    private var searchJob: Job? = null
    private var pendingSearch: Cancellable? = null
    private var pendingAutocomplete: Cancellable? = null
    private var pendingRoute: Cancellable? = null
    private var currentPosition: GeoPoint? = null

    private val horizonOptions = HorizonOptions(
        elementTypes = listOf(
            SpeedLimitElementType,
            TrafficSignElementType,
            HazardElementType,
            GeneralRoadPropertiesElementType,
        ),
    )

    val activeRoute: PlannedRoute? get() = routes.getOrNull(activeRouteIndex)

    /** Last position known from the location provider, or a default if there is none yet. */
    fun userPosition(): GeoPoint =
        (navigation.locationProvider.lastKnownLocation ?: TomTomSdk.locationProvider.lastKnownLocation)
            ?.position ?: currentPosition ?: DEFAULT_CENTER

    fun hasUserPosition(): Boolean =
        (navigation.locationProvider.lastKnownLocation ?: TomTomSdk.locationProvider.lastKnownLocation) != null

    fun onLocationPermissionGranted() {
        TomTomSdk.locationProvider.enable()
    }

    // ---- Search ----

    fun onQueryChange(text: String) {
        query = text
        searchJob?.cancel()
        pendingAutocomplete?.cancel()
        if (text.length < 2) {
            suggestions = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(250)
            val options = buildAutocompleteOptions(text, userPosition(), settings.language, 6)
            pendingAutocomplete = search.autocompleteSearch(
                options,
                object : AutocompleteCallback {
                    override fun onSuccess(result: AutocompleteResponse) {
                        suggestions = result.results.map { r ->
                            r.segments.joinToString(" ") { segment ->
                                when (segment) {
                                    is AutocompleteSegmentPlainText -> segment.plainText
                                    is AutocompleteSegmentBrand -> segment.brand.name
                                    is AutocompleteSegmentPoiCategory -> segment.matchedAlternativeName.orEmpty()
                                    else -> ""
                                }
                            }.trim()
                        }.filter { it.isNotBlank() }.distinct()
                    }

                    override fun onFailure(failure: SearchFailure) {
                        Log.w(TAG, "Autocomplete failed: ${failure.message}")
                    }
                },
            )
        }
    }

    fun submitSearch(text: String = query) {
        if (text.isBlank()) return
        query = text
        suggestions = emptyList()
        pendingSearch?.cancel()
        pendingSearch = search.search(
            buildTextSearchOptions(query = text, geoBias = userPosition(), language = settings.language, limit = 10),
            object : SearchCallback {
                override fun onSuccess(result: SearchResponse) {
                    results = result.results
                    if (result.results.isEmpty()) message = "Brak wyników dla „$text”"
                }

                override fun onFailure(failure: SearchFailure) {
                    message = "Błąd wyszukiwania: ${failure.message}"
                }
            },
        )
    }

    fun selectResult(result: SearchResult) {
        val place = result.place
        selected = SelectedPlace(
            name = place.name.ifBlank { "Wybrane miejsce" },
            address = place.address?.freeformAddress,
            point = place.coordinate,
        )
        results = emptyList()
        clearRoutes()
    }

    fun clearSearch() {
        query = ""
        suggestions = emptyList()
        results = emptyList()
    }

    // ---- Reverse geocoding ----

    fun selectPoint(point: GeoPoint) {
        clearRoutes()
        selected = SelectedPlace("Wybrany punkt", null, point)
        reverseGeocoder.reverseGeocode(
            buildReverseGeocoderOptions(point, settings.language),
            object : ReverseGeocoderCallback {
                override fun onSuccess(result: ReverseGeocoderResponse) {
                    val place = result.places.firstOrNull()?.place ?: return
                    selected = SelectedPlace(
                        name = place.address?.streetNameAndNumber ?: place.name.ifBlank { "Wybrany punkt" },
                        address = place.address?.freeformAddress,
                        point = point,
                    )
                }

                override fun onFailure(failure: SearchFailure) {
                    Log.w(TAG, "Reverse geocoding failed: ${failure.message}")
                }
            },
        )
    }

    fun clearSelection() {
        selected = null
        clearRoutes()
    }

    // ---- Routing ----

    fun planRoute() {
        val destination = selected?.point ?: return
        pendingRoute?.cancel()
        val avoids = buildSet {
            if (settings.avoidTolls) add(AvoidType.TollRoads)
            if (settings.avoidMotorways) add(AvoidType.Motorways)
        }
        val options = buildRoutePlanningOptions(
            itinerary = Itinerary(origin = userPosition(), destination = destination),
            maxAlternatives = settings.maxAlternatives,
            routeType = settings.routeType,
            avoids = avoids,
            language = settings.language,
        )
        message = "Wyznaczanie trasy…"
        pendingRoute = routePlanner.planRoute(
            options,
            object : RoutePlanningCallback {
                override fun onSuccess(result: RoutePlanningResponse) {
                    routes = result.routes.map { PlannedRoute(it, options) }
                    activeRouteIndex = 0
                    mode = Mode.RoutePreview
                    message = null
                }

                override fun onFailure(failure: RoutingFailure) {
                    message = "Nie udało się wyznaczyć trasy: ${failure.message}"
                }
            },
        )
    }

    fun selectRoute(index: Int) {
        if (index in routes.indices) activeRouteIndex = index
    }

    fun clearRoutes() {
        pendingRoute?.cancel()
        routes = emptyList()
        activeRouteIndex = 0
        if (mode == Mode.RoutePreview) mode = Mode.Browse
    }

    // ---- Navigation ----

    private val progressListener = ProgressUpdatedListener { progress: RouteProgress ->
        routeProgressMeters = progress.distanceAlongRoute.inMeters()
        guidance = guidance.copy(
            remainingMeters = progress.remainingDistance.inMeters(),
            remainingSeconds = progress.remainingTime.inWholeSeconds,
        )
    }

    private val guidanceListener = object : GuidanceUpdatedListener {
        override fun onInstructionsChanged(instructions: List<GuidanceInstruction>) {
            val next = instructions.firstOrNull() ?: return
            guidance = guidance.copy(
                maneuver = next.javaClass.simpleName.removeSuffix("GuidanceInstruction").removeSuffix("Impl"),
                road = next.nextSignificantRoad?.name ?: next.intersectionName.orEmpty(),
            )
        }

        override fun onAnnouncementGenerated(announcement: GuidanceAnnouncement, shouldPlay: Boolean) {
            // Voice playback requires a TTS engine integration; text is surfaced through guidance state.
        }

        override fun onDistanceToNextInstructionChanged(
            distance: com.tomtom.quantity.Distance,
            instructions: List<GuidanceInstruction>,
            currentPhase: InstructionPhase,
        ) {
            guidance = guidance.copy(distanceToNextMeters = distance.inMeters())
        }
    }

    private val arrivalListener = DestinationArrivalListener { arrived = true }

    private val horizonListener = object : HorizonUpdatedListener {
        private var snapshot: HorizonSnapshot? = null

        override fun onSnapshotUpdated(options: HorizonOptions, snapshot: HorizonSnapshot) {
            this.snapshot = snapshot
        }

        override fun onPositionUpdated(options: HorizonOptions, position: HorizonPosition) {
            val snap = snapshot ?: return
            val elements = snap.elementsAhead(position, com.tomtom.quantity.Distance.meters(1000.0))
                .map { it.javaClass.simpleName.removeSuffix("Impl") }
            horizon = HorizonInfo(position.offset.inMeters(), elements, position.isOnRoad)
        }

        override fun onHorizonReset(options: HorizonOptions) {
            snapshot = null
            horizon = null
        }
    }

    fun startNavigation() {
        val planned = activeRoute ?: return
        arrived = false
        guidance = GuidanceState()

        navigation.preferredLanguage = settings.language
        navigation.unitSystem = when (settings.unitSystem) {
            UnitSystem.Metric -> UnitSystemType.Fixed(SdkUnitSystem.Metric)
            UnitSystem.ImperialUs -> UnitSystemType.Fixed(SdkUnitSystem.US)
            UnitSystem.ImperialUk -> UnitSystemType.Fixed(SdkUnitSystem.UK)
        }

        val source: LocationProvider = if (settings.simulateLocation) {
            SimulationLocationProvider.create(
                strategy = InterpolationStrategy(planned.route.geometry.map { GeoLocation(it) }),
            )
        } else {
            TomTomSdk.locationProvider
        }
        navigation.locationProvider = source
        source.enable()

        navigation.addProgressUpdatedListener(progressListener)
        navigation.addGuidanceUpdatedListener(guidanceListener)
        navigation.addDestinationArrivalListener(arrivalListener)
        navigation.addHorizonUpdatedListener(horizonOptions, horizonListener)

        navigation.start(NavigationOptions(RoutePlan(planned.route, planned.options)))

        // The map follows the map-matched location while guidance is running.
        locationProvider = MapMatchedLocationProviderFactory.create(navigation)
        locationProvider.enable()
        mode = Mode.Navigating
    }

    fun stopNavigation() {
        if (mode != Mode.Navigating) return
        navigation.stop()
        navigation.removeProgressUpdatedListener(progressListener)
        navigation.removeGuidanceUpdatedListener(guidanceListener)
        navigation.removeDestinationArrivalListener(arrivalListener)
        navigation.removeHorizonUpdatedListener(horizonListener)
        navigation.locationProvider.disable()
        locationProvider.disable()
        locationProvider = TomTomSdk.locationProvider
        horizon = null
        routeProgressMeters = 0.0
        arrived = false
        mode = Mode.Browse
        clearRoutes()
    }

    fun dismissMessage() {
        message = null
    }

    override fun onCleared() {
        if (mode == Mode.Navigating) stopNavigation()
        search.close()
        reverseGeocoder.close()
        routePlanner.close()
    }
}
