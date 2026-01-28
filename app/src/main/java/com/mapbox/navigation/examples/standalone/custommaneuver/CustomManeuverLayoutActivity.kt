package com.mapbox.navigation.examples.standalone.custommaneuver

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.updatePaddingRelative
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.compose.DisposableMapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.lifecycle.requireMapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.tripdata.maneuver.model.Maneuver
import com.mapbox.navigation.tripdata.progress.api.MapboxTripProgressApi
import com.mapbox.navigation.tripdata.progress.model.DistanceRemainingFormatter
import com.mapbox.navigation.tripdata.progress.model.EstimatedTimeOfArrivalFormatter
import com.mapbox.navigation.tripdata.progress.model.PercentDistanceTraveledFormatter
import com.mapbox.navigation.tripdata.progress.model.TimeRemainingFormatter
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateValue
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.components.R
import com.mapbox.navigation.ui.components.tripprogress.view.MapboxTripProgressView
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.voice.api.MapboxSpeechApi
import com.mapbox.navigation.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.voice.model.SpeechAnnouncement
import com.mapbox.navigation.voice.model.SpeechError
import com.mapbox.navigation.voice.model.SpeechValue
import com.mapbox.navigation.voice.model.SpeechVolume
import java.util.Locale
import kotlin.math.roundToInt

/**
 * This example demonstrates a customized MapboxManeuverView that displays a fullscreen
 * maneuver list when expanded.
 *
 * ## Integration Guide
 *
 * This example shows how to achieve the fullscreen maneuver behavior in your own app.
 * The solution consists of three files:
 *
 * 1. **mapbox_maneuver_layout.xml** - custom layout so support fullscreen layout
 * 2. **MapboxManeuverViewExtensions.kt** - extension function for state binding
 * 3. **FullscreenMapboxManeuverView.kt** - compose wrapper (optional)
 *
 * ### Usage in Jetpack Compose
 *
 * ```kotlin
 * FullscreenMapboxManeuverView(
 *     maneuvers = maneuvers,
 *     onExpanded = { analytics.track("ManeuverListExpanded") },
 *     onCollapsed = { analytics.track("ManeuverListCollapsed") }
 * )
 * ```
 *
 * ### Usage with AndroidView
 *
 * ```kotlin
 * AndroidView(
 *     factory = { context -> MapboxManeuverView(context) },
 *     update = { maneuverView ->
 *         maneuverView.renderManeuvers(maneuvers)
 *         maneuverView.bindFullscreenState(
 *             isExpanded = isExpanded,
 *             onExpansionToggled = { setExpanded(it) }
 *         )
 *     }
 * )
 * ```
 *
 * ### Behavior
 * - **Collapsed**: shows next 2 upcoming maneuvers (default SDK behavior)
 * - **Expanded**: maneuver list fills screen height, covering trip progress view as an overlay
 * - **Toggle**: tap header to toggle between expanded and collapsed
 * - **Close button**: Appears at bottom when expanded, collapses list on click
 * - **Back button**: Pressing back when expanded collapses the list
 *
 * ---
 *
 * This example is based on the JetpackComposeActivity.
 *
 * Before running the example make sure you have put your access_token in the correct place
 * inside [app/src/main/res/values/mapbox_access_token.xml].
 *
 * The example assumes that you have granted location permissions.
 *
 * How to use this example:
 * - Long press on the map to add a waypoint
 * - The guidance will start to the selected destination
 * - Tap the maneuver view header to toggle fullscreen expansion (covers trip progress as an overlay)
 * - Tap the header again or use the "Close" button to collapse the list
 * - You can use buttons to mute/unmute voice instructions, recenter the camera, or show the route overview.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class CustomManeuverLayoutActivity : AppCompatActivity() {

    private var navigationCamera: NavigationCamera? = null
    private var viewportDataSource: MapboxNavigationViewportDataSource? = null

    private val pixelDensity by lazy { resources.displayMetrics.density }
    private val overviewPadding by lazy {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            EdgeInsets(
                30.0 * pixelDensity,
                380.0 * pixelDensity,
                110.0 * pixelDensity,
                20.0 * pixelDensity
            )
        } else {
            EdgeInsets(
                140.0 * pixelDensity,
                40.0 * pixelDensity,
                120.0 * pixelDensity,
                40.0 * pixelDensity
            )
        }
    }
    private val followingPadding by lazy {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            EdgeInsets(
                30.0 * pixelDensity,
                380.0 * pixelDensity,
                110.0 * pixelDensity,
                40.0 * pixelDensity
            )
        } else {
            EdgeInsets(
                180.0 * pixelDensity,
                40.0 * pixelDensity,
                150.0 * pixelDensity,
                40.0 * pixelDensity
            )
        }
    }

    private lateinit var maneuverApi: MapboxManeuverApi
    private lateinit var tripProgressApi: MapboxTripProgressApi
    private lateinit var routeLineApi: MapboxRouteLineApi
    private lateinit var routeLineView: MapboxRouteLineView
    private val routeArrowApi = MapboxRouteArrowApi()
    private lateinit var routeArrowView: MapboxRouteArrowView
    private val isVoiceInstructionsMuted = mutableStateOf<Boolean?>(null)
    private lateinit var speechApi: MapboxSpeechApi
    private lateinit var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer

    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        speechApi.generate(voiceInstructions, speechCallback)
    }

    private val speechCallback =
        MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { expected ->
            expected.fold(
                { error ->
                    voiceInstructionsPlayer.play(
                        error.fallback,
                        voiceInstructionsPlayerCallback
                    )
                },
                { value ->
                    voiceInstructionsPlayer.play(
                        value.announcement,
                        voiceInstructionsPlayerCallback
                    )
                }
            )
        }

    private val voiceInstructionsPlayerCallback =
        MapboxNavigationConsumer<SpeechAnnouncement> { value ->
            speechApi.clean(value)
        }

    private val navigationLocationProvider = NavigationLocationProvider()

    private val locationObserver = object : LocationObserver {
        var firstLocationUpdateReceived = false

        override fun onNewRawLocation(rawLocation: Location) {
            // not handled
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            navigationLocationProvider.changePosition(
                location = enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )

            viewportDataSource?.onLocationChanged(enhancedLocation)
            viewportDataSource?.evaluate()

            if (!firstLocationUpdateReceived) {
                navigationCamera?.let { navigationCamera ->
                    firstLocationUpdateReceived = true
                    navigationCamera.requestNavigationCameraToOverview(
                        stateTransitionOptions = NavigationCameraTransitionOptions.Builder()
                            .maxDuration(0)
                            .build()
                    )
                }
            }
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        viewportDataSource?.onRouteProgressChanged(routeProgress)
        viewportDataSource?.evaluate()

        mapboxMap?.style?.let { style ->
            val maneuverArrowResult = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
            routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)
        }

        maneuvers.value = maneuverApi.getManeuvers(routeProgress).getValueOrElse { emptyList() }
        tripProgress.value = tripProgressApi.getTripProgress(routeProgress)
    }

    private val routesObserver = RoutesObserver { routeUpdateResult ->
        if (routeUpdateResult.navigationRoutes.isEmpty()) {
            mapboxMap?.style?.let { style ->
                routeLineApi.clearRouteLine { value ->
                    routeLineView.renderClearRouteLineValue(
                        style,
                        value
                    )
                }
                routeArrowView.render(style, routeArrowApi.clearArrows())
            }

            viewportDataSource?.clearRouteData()
            viewportDataSource?.evaluate()
        } else {
            routeLineApi.setNavigationRoutes(
                routeUpdateResult.navigationRoutes
            ) { value ->
                mapboxMap?.style?.let { style ->
                    routeLineView.renderRouteDrawData(style, value)
                }
            }

            viewportDataSource?.onRouteChanged(routeUpdateResult.navigationRoutes.first())
            viewportDataSource?.evaluate()
        }
    }

    private val mapboxNavigation by requireMapboxNavigation(
        onResumedObserver = object : MapboxNavigationObserver {
            @SuppressLint("MissingPermission")
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.registerRoutesObserver(routesObserver)
                mapboxNavigation.registerLocationObserver(locationObserver)
                mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)

                mapboxNavigation.startTripSession()
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.unregisterRoutesObserver(routesObserver)
                mapboxNavigation.unregisterLocationObserver(locationObserver)
                mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
            }
        },
        onInitialize = this::initNavigation
    )

    private val addedWaypoints = mutableListOf<Point>()
    private var mapboxMap: MapboxMap? = null

    private val tripProgress = mutableStateOf<TripProgressUpdateValue?>(null)
    private val maneuvers = mutableStateOf(emptyList<Maneuver>())
    private val isFollowingState = mutableStateOf(false)

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Box {
                MapView()
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .zIndex(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    MapboxManeuverView()
                    MapboxSoundButton()
                    MapboxCameraButton()
                }
                MapboxTripProgressView()
            }
        }

        val distanceFormatterOptions = DistanceFormatterOptions.Builder(this).build()

        maneuverApi = MapboxManeuverApi(
            MapboxDistanceFormatter(distanceFormatterOptions)
        )

        tripProgressApi = MapboxTripProgressApi(
            TripProgressUpdateFormatter.Builder(this)
                .distanceRemainingFormatter(
                    DistanceRemainingFormatter(distanceFormatterOptions)
                )
                .timeRemainingFormatter(
                    TimeRemainingFormatter(this)
                )
                .percentRouteTraveledFormatter(
                    PercentDistanceTraveledFormatter()
                )
                .estimatedTimeOfArrivalFormatter(
                    EstimatedTimeOfArrivalFormatter(this, TimeFormat.NONE_SPECIFIED)
                )
                .build()
        )

        speechApi = MapboxSpeechApi(
            this,
            Locale.US.language
        )
        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
            this,
            Locale.US.language
        )

        val mapboxRouteLineViewOptions = MapboxRouteLineViewOptions.Builder(this)
            .routeLineBelowLayerId("road-label-navigation")
            .build()

        routeLineApi = MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build())
        routeLineView = MapboxRouteLineView(mapboxRouteLineViewOptions)

        val routeArrowOptions = RouteArrowOptions.Builder(this).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)
    }

    override fun onDestroy() {
        super.onDestroy()
        maneuverApi.cancel()
        routeLineApi.cancel()
        routeLineView.cancel()
        speechApi.cancel()
        voiceInstructionsPlayer.shutdown()
    }

    private fun initNavigation() {
        MapboxNavigationApp.setup(
            NavigationOptions.Builder(this)
                .build()
        )
    }

    private fun addWaypoint(destination: Point) {
        val originLocation = navigationLocationProvider.lastLocation ?: return
        val originPoint = Point.fromLngLat(originLocation.longitude, originLocation.latitude)

        if (addedWaypoints.isEmpty()) {
            addedWaypoints.add(originPoint)
        }

        addedWaypoints.add(destination)

        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(this)
                .coordinatesList(addedWaypoints)
                .bearingsList(
                    originLocation.bearing?.let { bearing ->
                        buildList {
                            add(
                                Bearing.builder()
                                    .angle(bearing)
                                    .degrees(45.0)
                                    .build()
                            )
                            repeat(addedWaypoints.size - 1) { add(null) }
                        }
                    }
                )
                .layersList(
                    buildList {
                        add(mapboxNavigation.getZLevel())
                        repeat(addedWaypoints.size - 1) { add(null) }
                    }
                )
                .build(),
            object : NavigationRouterCallback {
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
                    // no impl
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    // no impl
                }

                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: String
                ) {
                    setRouteAndStartNavigation(routes)
                }
            }
        )
    }

    private fun setRouteAndStartNavigation(routes: List<NavigationRoute>) {
        mapboxNavigation.setNavigationRoutes(routes)

        voiceInstructionsPlayer.volume(SpeechVolume(1f))
        isVoiceInstructionsMuted.value = false

        navigationCamera?.requestNavigationCameraToOverview()
    }

    private fun clearRouteAndStopNavigation() {
        mapboxNavigation.setNavigationRoutes(emptyList())
        addedWaypoints.clear()

        isVoiceInstructionsMuted.value = null
        maneuvers.value = emptyList()
        tripProgress.value = null
    }

    @Composable
    private fun MapView() {
        MapboxMap(
            onMapLongClickListener = OnMapLongClickListener { point ->
                addWaypoint(point)
                true
            },
            style = { MapStyle(NavigationStyles.NAVIGATION_DAY_STYLE) },
            compass = {},
        ) {
            DisposableMapEffect(Unit) { mapView ->
                mapboxMap = mapView.mapboxMap

                mapView.location.apply {
                    setLocationProvider(navigationLocationProvider)
                    locationPuck = LocationPuck2D(
                        bearingImage = ImageHolder.from(
                            R.drawable.mapbox_navigation_puck_icon
                        )
                    )
                    puckBearingEnabled = true
                    enabled = true
                }

                val viewportDataSource = MapboxNavigationViewportDataSource(mapView.mapboxMap)
                    .also { viewportDataSource = it }
                val navigationCamera = NavigationCamera(
                    mapView.mapboxMap,
                    mapView.camera,
                    viewportDataSource
                ).also { navigationCamera = it }
                mapView.camera.addCameraAnimationsLifecycleListener(
                    NavigationBasicGesturesHandler(navigationCamera)
                )
                navigationCamera.registerNavigationCameraStateChangeObserver { navigationCameraState ->
                    isFollowingState.value = when (navigationCameraState) {
                        NavigationCameraState.TRANSITION_TO_FOLLOWING,
                        NavigationCameraState.FOLLOWING -> true

                        NavigationCameraState.TRANSITION_TO_OVERVIEW,
                        NavigationCameraState.OVERVIEW,
                        NavigationCameraState.IDLE -> false
                    }
                }
                viewportDataSource.overviewPadding = overviewPadding
                viewportDataSource.followingPadding = followingPadding

                mapView.mapboxMap.getStyle { style ->
                    routeLineView.initializeLayers(style)
                }

                onDispose {
                    this@CustomManeuverLayoutActivity.navigationCamera = null
                    this@CustomManeuverLayoutActivity.viewportDataSource = null
                    this@CustomManeuverLayoutActivity.mapboxMap = null
                }
            }
        }
    }

    @Composable
    private fun MapboxManeuverView() {
        FullscreenMapboxManeuverView(
            maneuvers = maneuvers.value,
            onExpanded = {
                android.util.Log.d("ManeuverView", "Maneuver list expanded")
            },
            onCollapsed = {
                android.util.Log.d("ManeuverView", "Maneuver list collapsed")
            }
        )
    }

    @Composable
    private fun MapboxSoundButton() {
        val muted = isVoiceInstructionsMuted.value ?: return
        Image(
            modifier = Modifier
                .padding(top = 8.dp, end = 16.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(colorResource(R.color.colorSurface))
                .clickable {
                    voiceInstructionsPlayer.volume(SpeechVolume(if (muted) 1f else 0f))
                    isVoiceInstructionsMuted.value = !muted
                }
                .padding(16.dp),
            painter = painterResource(
                if (muted) {
                    R.drawable.mapbox_ic_sound_off
                } else {
                    R.drawable.mapbox_ic_sound_on
                },
            ),
            contentDescription = null,
        )
    }

    @Composable
    private fun MapboxCameraButton() {
        Image(
            modifier = Modifier
                .padding(top = 8.dp, end = 16.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(colorResource(R.color.colorSurface))
                .clickable {
                    if (isFollowingState.value) {
                        navigationCamera?.requestNavigationCameraToOverview()
                    } else {
                        navigationCamera?.requestNavigationCameraToFollowing()
                    }
                }
                .padding(16.dp),
            painter = painterResource(
                if (isFollowingState.value) {
                    R.drawable.mapbox_ic_route_overview
                } else {
                    R.drawable.mapbox_ic_recenter
                },
            ),
            contentDescription = null,
        )
    }

    @Composable
    private fun BoxScope.MapboxTripProgressView() {
        val tripProgress = tripProgress.value ?: return
        AndroidView(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(64.dp),
            factory = { context ->
                MapboxTripProgressView(context).apply {
                    updatePaddingRelative(start = (12 * pixelDensity).roundToInt())
                }
            },
            update = { it.render(tripProgress) },
        )
        Image(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(64.dp)
                .clip(CircleShape)
                .clickable { clearRouteAndStopNavigation() }
                .padding(12.dp),
            painter = painterResource(android.R.drawable.ic_delete),
            contentDescription = null,
        )
    }
}
