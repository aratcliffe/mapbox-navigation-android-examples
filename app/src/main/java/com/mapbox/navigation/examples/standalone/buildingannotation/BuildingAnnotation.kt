package com.mapbox.navigation.examples.standalone.buildingannotation

import androidx.annotation.ColorInt
import androidx.compose.runtime.Composable
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.DisposableMapEffect
import com.mapbox.maps.extension.compose.MapboxMapComposable

/**
 * A Composable function that displays a single building annotation on the map.
 *
 * This composable manages the lifecycle of a single building annotation using DisposableMapEffect.
 * It follows the SDK pattern for singular annotations (like CircleAnnotation).
 *
 * Usage:
 * ```
 * MapboxMap {
 *     BuildingAnnotation(
 *         points = buildingPoints,
 *         fillExtrusionColor = 0xFF3489F9.toInt(),
 *         fillExtrusionHeight = 50.0
 *     )
 * }
 * ```
 *
 * @param points The list of coordinates defining the building footprint polygon
 * @param fillExtrusionColor The color of the building extrusion (ARGB Int, default: hsl(214, 94%, 59%) = #3489F9)
 * @param fillExtrusionOpacity The opacity of the building extrusion (0.0-1.0, default: 0.8)
 * @param fillExtrusionHeight The height of the building extrusion in meters (default: 50.0)
 * @param fillExtrusionBase The base elevation of the building extrusion in meters (default: 0.0)
 */
@Composable
@MapboxMapComposable
fun BuildingAnnotation(
    points: List<Point>,
    @ColorInt fillExtrusionColor: Int = 0xFF3489F9.toInt(),
    fillExtrusionOpacity: Double = 0.8,
    fillExtrusionHeight: Double = 50.0,
    fillExtrusionBase: Double = 0.0
) {
    DisposableMapEffect(points, fillExtrusionColor, fillExtrusionOpacity, fillExtrusionHeight, fillExtrusionBase) { mapView ->
        val manager = BuildingAnnotationManager(mapView)

        val annotation = BuildingAnnotationOptions(
            points = points,
            fillExtrusionColor = fillExtrusionColor,
            fillExtrusionOpacity = fillExtrusionOpacity,
            fillExtrusionHeight = fillExtrusionHeight,
            fillExtrusionBase = fillExtrusionBase
        )
        manager.annotations = listOf(annotation)

        onDispose {
            manager.annotations = emptyList()
        }
    }
}
