package com.mapbox.navigation.examples.standalone.buildingannotation

import androidx.compose.runtime.Composable
import com.mapbox.maps.extension.compose.DisposableMapEffect
import com.mapbox.maps.extension.compose.MapboxMapComposable

/**
 * A Composable function that displays multiple building annotations on the map.
 *
 * This composable manages the lifecycle of building annotations using DisposableMapEffect.
 * It follows the SDK pattern for annotation groups (like CircleAnnotationGroup).
 * All annotations in the group share the same BuildingAnnotationManager instance for efficiency.
 *
 * Usage:
 * ```
 * MapboxMap {
 *     BuildingAnnotationGroup(
 *         annotations = listOf(
 *             BuildingAnnotationOptions(
 *                 points = building1Points,
 *                 fillExtrusionHeight = 50.0
 *             ),
 *             BuildingAnnotationOptions(
 *                 points = building2Points,
 *                 fillExtrusionColor = 0xFF00FF00.toInt(),
 *                 fillExtrusionHeight = 75.0
 *             )
 *         )
 *     )
 * }
 * ```
 *
 * @param annotations The list of building annotation options to display
 */
@Composable
@MapboxMapComposable
fun BuildingAnnotationGroup(
    annotations: List<BuildingAnnotationOptions>
) {
    DisposableMapEffect(annotations) { mapView ->
        val manager = BuildingAnnotationManager(mapView)
        manager.annotations = annotations

        onDispose {
            manager.annotations = emptyList()
        }
    }
}
