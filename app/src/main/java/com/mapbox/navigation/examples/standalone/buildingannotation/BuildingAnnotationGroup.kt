package com.mapbox.navigation.examples.standalone.buildingannotation

import androidx.annotation.ColorInt
import androidx.compose.runtime.Composable
import com.mapbox.maps.extension.compose.DisposableMapEffect
import com.mapbox.maps.extension.compose.MapboxMapComposable

/**
 * A Composable function that displays multiple building annotations on the map.
 *
 * **Note:** Due to FillExtrusionLayer limitations, `fillExtrusionOpacity` is applied uniformly
 * to all buildings in the group. Individual annotation opacity values are ignored.
 *
 * Usage:
 * ```
 * MapboxMap {
 *     BuildingAnnotationGroup(
 *         annotations = listOf(
 *             BuildingAnnotationOptions(
 *                 points = building1Points
 *                 // Uses group defaults
 *             ),
 *             BuildingAnnotationOptions(
 *                 points = building2Points,
 *                 fillExtrusionColor = 0xFF00FF00.toInt(),
 *                 fillExtrusionHeight = 75.0
 *             )
 *         ),
 *         fillExtrusionColor = 0xFF3489F9.toInt(),  // Default blue
 *         fillExtrusionOpacity = 0.9,
 *         fillExtrusionHeight = 50.0,
 *         fillExtrusionBase = 0.0
 *     )
 * }
 * ```
 *
 * @param annotations The list of building annotation options to display
 * @param fillExtrusionColor The default color for all buildings in the group (default: 0xFF3489F9)
 * @param fillExtrusionOpacity The opacity for all buildings in the group (0.0-1.0, default: 0.8)
 * @param fillExtrusionHeight The default height for all buildings in the group in meters (default: 50.0)
 * @param fillExtrusionBase The default base elevation for all buildings in the group in meters (default: 0.0)
 */
@Composable
@MapboxMapComposable
fun BuildingAnnotationGroup(
    annotations: List<BuildingAnnotationOptions>,
    @ColorInt fillExtrusionColor: Int = 0xFF3489F9.toInt(),
    fillExtrusionOpacity: Double = 0.8,
    fillExtrusionHeight: Double = 50.0,
    fillExtrusionBase: Double = 0.0
) {
    DisposableMapEffect(annotations, fillExtrusionColor, fillExtrusionOpacity, fillExtrusionHeight, fillExtrusionBase) { mapView ->
        val manager = BuildingAnnotationManager(mapView)
        manager.fillExtrusionColor = fillExtrusionColor
        manager.fillExtrusionOpacity = fillExtrusionOpacity
        manager.fillExtrusionHeight = fillExtrusionHeight
        manager.fillExtrusionBase = fillExtrusionBase
        manager.annotations = annotations

        onDispose {
            manager.annotations = emptyList()
        }
    }
}
