package com.mapbox.navigation.examples.standalone.buildingannotation

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.DisposableMapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.style.GenericStyle
import com.mapbox.maps.extension.compose.style.rememberStyleState
import com.mapbox.maps.extension.compose.style.styleImportsConfig
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.lifecycle.requireMapboxNavigation
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Demonstrates how to highlight destination buildings using Mapbox Standard style.
 *
 * This example shows building highlight at final destination using Jetpack Compose
 * with inline building selection logic (no extension functions).
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class BuildingAnnotationActivity : AppCompatActivity() {

    private val routeCoordinates = listOf(
        Point.fromLngLat(-122.4192, 37.7627),
        Point.fromLngLat(-122.4183502, 37.7653577),
        Point.fromLngLat(-122.4145371, 37.7657253),
    )

    /**
     * Controls whether to simulate the route or use real location updates.
     */
    private val enableRouteSimulation = true

    /**
     * Replay observer that keeps the replayer in sync with route progress.
     */
    private lateinit var replayProgressObserver: ReplayProgressObserver

    /**
     * State for showing/hiding the start button.
     */
    private val showStartButton = mutableStateOf(true)

    /**
     * State for building points to display with BuildingAnnotation.
     * When set, the BuildingAnnotation appears in the composition.
     * When null, the annotation is removed.
     */
    private data class BuildingData(
        val points: List<Point>,
        val height: Double
    )

    private val buildingData = mutableStateOf<BuildingData?>(null)

    /**
     * Coroutine scope for async building queries.
     */
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * [NavigationLocationProvider] provides location updates from Navigation SDK to Maps SDK.
     */
    private val navigationLocationProvider = NavigationLocationProvider()

    /**
     * Route line options
     */
    private val routeLineOptions: MapboxRouteLineViewOptions by lazy {
        MapboxRouteLineViewOptions.Builder(this)
            .routeLineBelowLayerId("road-label-navigation")
            .build()
    }

    private val routeLineApiOptions: MapboxRouteLineApiOptions by lazy {
        MapboxRouteLineApiOptions.Builder().build()
    }

    /**
     * Renders route lines on the map.
     */
    private val routeLineView by lazy {
        MapboxRouteLineView(routeLineOptions)
    }

    /**
     * Generates route line updates.
     */
    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(routeLineApiOptions)
    }

    /**
     * Gets notified with location updates.
     */
    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
            // Not needed in this example
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            navigationLocationProvider.changePosition(
                enhancedLocation,
                locationMatcherResult.keyPoints,
            )
            // Update camera to follow the user
            updateCamera(
                Point.fromLngLat(
                    enhancedLocation.longitude,
                    enhancedLocation.latitude
                ),
                enhancedLocation.bearing
            )
        }
    }

    /**
     * Updates route lines when routes change.
     */
    private val routesObserver: RoutesObserver = RoutesObserver { routeUpdateResult ->
        routeLineApi.setNavigationRoutes(
            routeUpdateResult.navigationRoutes
        ) { value ->
            mapboxMap?.style?.apply {
                routeLineView.renderRouteDrawData(this, value)
            }
        }
    }

    /**
     * Handles arrival events for building highlighting.
     */
    private val arrivalObserver: ArrivalObserver = object : ArrivalObserver {
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            val destination = extractLegDestination(routeProgress) ?: return
            queryAndHighlightBuilding(destination)
        }

        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
            // Remove annotation by clearing state
            buildingData.value = null
        }

        override fun onWaypointArrival(routeProgress: RouteProgress) {
            // Not handling waypoint arrivals in this example
        }
    }

    private val mapboxNavigation: MapboxNavigation by requireMapboxNavigation(
        onResumedObserver = object : MapboxNavigationObserver {
            @SuppressLint("MissingPermission")
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.registerRoutesObserver(routesObserver)
                mapboxNavigation.registerArrivalObserver(arrivalObserver)
                mapboxNavigation.registerLocationObserver(locationObserver)

                if (enableRouteSimulation) {
                    replayProgressObserver = ReplayProgressObserver(mapboxNavigation.mapboxReplayer)
                    mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
                    mapboxNavigation.startReplayTripSession()
                } else {
                    mapboxNavigation.startTripSession()
                }
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.unregisterRoutesObserver(routesObserver)
                mapboxNavigation.unregisterArrivalObserver(arrivalObserver)
                mapboxNavigation.unregisterLocationObserver(locationObserver)

                if (enableRouteSimulation) {
                    mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
                    mapboxNavigation.mapboxReplayer.finish()
                }
            }
        },
        onInitialize = this::initNavigation
    )

    private var mapboxMap: com.mapbox.maps.MapboxMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MapView()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        routeLineView.cancel()
        routeLineApi.cancel()
    }

    @Composable
    private fun MapView() {
        val mapState = rememberMapState()
        val mapViewportState = rememberMapViewportState {
            setCameraOptions {
                center(routeCoordinates.first())
                zoom(15.0)
                pitch(0.0)
                bearing(0.0)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapState = mapState,
                mapViewportState = mapViewportState,
            ) {
                GenericStyle(
                    style = "mapbox://styles/mapbox/standard",
                    styleState = rememberStyleState {
                        styleImportsConfig = styleImportsConfig {
                            importConfig("basemap") {
                                config("show3dObjects", Expression.literal(false))
                            }
                        }
                    }
                )

                // Add BuildingAnnotation when data is available
                buildingData.value?.let { data ->
                    BuildingAnnotation(
                        points = data.points,
                        fillExtrusionHeight = data.height
                    )
                }

                DisposableMapEffect(Unit) { mapView ->
                    mapboxMap = mapView.mapboxMap

                    // Set up location provider
                    mapView.location.apply {
                        setLocationProvider(navigationLocationProvider)
                        enabled = true
                    }

                    onDispose {
                        mapboxMap = null
                    }
                }
            }

            // Start button
            if (showStartButton.value) {
                Button(
                    onClick = {
                        showStartButton.value = false
                        fetchRoute()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A90E2)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Start Navigation", color = Color.White)
                }
            }
        }
    }

    private fun updateCamera(point: Point, bearing: Double? = null) {
        mapboxMap?.let { map ->
            val mapAnimationOptions = MapAnimationOptions.Builder().duration(1500L).build()
            map.easeTo(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(17.0)
                    .bearing(bearing)
                    .pitch(45.0)
                    .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                    .build(),
                mapAnimationOptions
            )
        }
    }

    private fun initNavigation() {
        MapboxNavigationApp.setup(
            NavigationOptions.Builder(this)
                .build()
        )

        if (enableRouteSimulation) {
            replayOriginLocation()
        }
    }

    private fun fetchRoute() {
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(this)
                .alternatives(false)
                .coordinatesList(routeCoordinates)
                .layersList(listOf(mapboxNavigation.getZLevel(), null, null))
                .build(),

            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    @RouterOrigin routerOrigin: String
                ) {
                    mapboxNavigation.setNavigationRoutes(routes)
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    Log.d(LOG_TAG, "onFailure: $reasons")
                }

                override fun onCanceled(
                    routeOptions: RouteOptions,
                    @RouterOrigin routerOrigin: String
                ) {
                    Log.d(LOG_TAG, "onCanceled")
                }
            }
        )
    }

    private fun replayOriginLocation() {
        with(mapboxNavigation.mapboxReplayer) {
            play()
            pushEvents(
                listOf(
                    ReplayRouteMapper.mapToUpdateLocation(
                        Date().time.toDouble(),
                        routeCoordinates.first()
                    )
                )
            )
            playFirstLocation()
            playbackSpeed(3.0)
        }
    }

    private fun extractLegDestination(routeProgress: RouteProgress): Point? {
        val legProgress = routeProgress.currentLegProgress ?: return null
        val legDestination = legProgress.legDestination ?: return null
        return legDestination.location
    }

    /**
     * Queries for building features at the destination and highlights the first one found.
     *
     * This method queries rendered features from the buildings featureset
     * and displays them using the BuildingAnnotation composable.
     */
    private fun queryAndHighlightBuilding(destination: Point) {
        coroutineScope.launch {
            val mapboxMap = mapboxMap ?: return@launch

            try {
                val features = mapboxMap.queryBuildings(destination)

                val firstFeature = features.firstOrNull()

                if (firstFeature != null) {
                    val points = firstFeature.geometry.toPoints()
                    if (points != null) {
                        val estHeightValue = firstFeature.properties.opt("est_height")
                        val heightValue = firstFeature.properties.opt("height")

                        val height = when (estHeightValue) {
                            is Number -> estHeightValue.toDouble()
                            else -> null
                        } ?: when (heightValue) {
                            is Number -> heightValue.toDouble()
                            else -> null
                        } ?: 50.0

                        buildingData.value = BuildingData(points, height)
                    }
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to query building", e)
            }
        }
    }

    private companion object {
        val LOG_TAG: String = BuildingAnnotationActivity::class.java.simpleName
    }
}
