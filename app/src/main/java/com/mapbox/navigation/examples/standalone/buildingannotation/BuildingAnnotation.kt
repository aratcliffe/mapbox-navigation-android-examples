package com.mapbox.navigation.examples.standalone.buildingannotation

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
 *         fillExtrusionColor = "#3489F9",
 *         fillExtrusionHeight = 50.0
 *     )
 * }
 * ```
 *
 * @param points The list of coordinates defining the building footprint polygon
 * @param fillExtrusionColor The color of the building extrusion (CSS color string, default: "#3489F9")
 * @param fillExtrusionOpacity The opacity of the building extrusion (0.0-1.0, default: 0.8)
 * @param fillExtrusionHeight The height of the building extrusion in meters (default: 50.0)
 * @param fillExtrusionBase The base elevation of the building extrusion in meters (default: 0.0)
 * @param labelText Optional text label to display above the building, elevated to the rooftop (default: null)
 * @param labelPosition When provided, the label is placed at the nearest point on the building
 *   boundary to this point. Falls back to the polygon centroid when null.
 * @param textColor Color of the label text in day light preset (CSS color string, default: "#404040")
 * @param textColorNight Color of the label text in night light preset (CSS color string).
 *   Falls back to [textColor] if not provided.
 * @param textSize Size of the label text in sp (default: 16.0)
 * @param textFont Font stack for the label (default: ["DIN Pro Medium", "Arial Unicode MS Regular"])
 * @param slot The Mapbox Standard style slot to insert layers into (e.g. "bottom", "middle", "top")
 */
@Composable
@MapboxMapComposable
fun BuildingAnnotation(
    points: List<Point>,
    fillExtrusionColor: String = "#3489F9",
    fillExtrusionOpacity: Double = 0.8,
    fillExtrusionHeight: Double = 50.0,
    fillExtrusionBase: Double = 0.0,
    labelText: String? = null,
    labelPosition: Point? = null,
    textColor: String = "#404040",
    textColorNight: String = "#FFFFFF",
    textSize: Double = 16.0,
    textFont: List<String> = listOf("DIN Pro Medium", "Arial Unicode MS Regular"),
    slot: String? = null,
) {
    DisposableMapEffect(points, fillExtrusionColor, fillExtrusionOpacity, fillExtrusionHeight, fillExtrusionBase, labelText, labelPosition, textColor, textColorNight, textSize, textFont, slot) { mapView ->
        val manager = BuildingAnnotationManager(mapView)
        manager.slot = slot
        manager.textFont = textFont

        val annotation = BuildingAnnotationOptions(
            points = points,
            fillExtrusionColor = fillExtrusionColor,
            fillExtrusionOpacity = fillExtrusionOpacity,
            fillExtrusionHeight = fillExtrusionHeight,
            fillExtrusionBase = fillExtrusionBase,
            labelText = labelText,
            labelPosition = labelPosition,
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
