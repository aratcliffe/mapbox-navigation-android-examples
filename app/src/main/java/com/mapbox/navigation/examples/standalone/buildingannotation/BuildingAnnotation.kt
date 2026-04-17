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
 * @param labelText Optional text label to display above the building, elevated to the rooftop (default: null)
 * @param textColor Color of the label text in day light preset (ARGB Int, default: hsl(0,0%,25%) = #404040)
 * @param textColorNight Color of the label text in night light preset (ARGB Int).
 *   Falls back to [textColor] if not provided.
 * @param textSize Size of the label text in sp (default: 16.0)
 * @param textFont Font stack for the label (default: ["DIN Pro Medium", "Arial Unicode MS Regular"])
 */
@Composable
@MapboxMapComposable
fun BuildingAnnotation(
    points: List<Point>,
    @ColorInt fillExtrusionColor: Int = 0xFF3489F9.toInt(),
    fillExtrusionOpacity: Double = 0.8,
    fillExtrusionHeight: Double = 50.0,
    fillExtrusionBase: Double = 0.0,
    labelText: String? = null,
    @ColorInt textColor: Int = 0xFF404040.toInt(),
    @ColorInt textColorNight: Int = 0xFFFFFFFF.toInt(),
    textSize: Double = 16.0,
    textFont: List<String> = listOf("DIN Pro Medium", "Arial Unicode MS Regular")
) {
    DisposableMapEffect(points, fillExtrusionColor, fillExtrusionOpacity, fillExtrusionHeight, fillExtrusionBase, labelText, textColor, textColorNight, textSize, textFont) { mapView ->
        val manager = BuildingAnnotationManager(mapView)
        manager.textFont = textFont

        val annotation = BuildingAnnotationOptions(
            points = points,
            fillExtrusionColor = fillExtrusionColor,
            fillExtrusionOpacity = fillExtrusionOpacity,
            fillExtrusionHeight = fillExtrusionHeight,
            fillExtrusionBase = fillExtrusionBase,
            labelText = labelText,
            textColor = textColor,
            textColorNight = textColorNight,
            textSize = textSize
        )
        manager.annotations = listOf(annotation)

        onDispose {
            manager.annotations = emptyList()
        }
    }
}
