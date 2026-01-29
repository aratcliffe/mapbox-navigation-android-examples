package com.mapbox.navigation.examples.standalone.buildingannotation

import androidx.annotation.ColorInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.layers.generated.FillExtrusionLayer
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.rememberGeoJsonSourceState
import java.util.UUID

/**
 * A Composable annotation that displays a 3D building.
 *
 * This annotation uses the SDK's composable layer and source components to render
 * a building footprint with 3D extrusion. The lifecycle is automatically managed by
 * the Compose framework - when removed from composition, the source and layer are cleaned up.
 *
 * Usage:
 * ```
 * MapboxMap {
 *     buildingPoints.value?.let { points ->
 *         BuildingAnnotation(
 *             points = points,
 *             fillExtrusionColor = 0xFFFF0000.toInt(),
 *             fillExtrusionHeight = 50.0
 *         )
 *     }
 * }
 * ```
 *
 * @param points The list of coordinates for the building footprint polygon
 * @param fillExtrusionColor The color of the building extrusion (ARGB Int, default: red)
 * @param fillExtrusionOpacity The opacity of the building extrusion (0.0-1.0, default: 0.8)
 * @param fillExtrusionHeight The height of the building extrusion in meters (default: 50.0)
 * @param fillExtrusionBase The base elevation of the building extrusion in meters (default: 0.0)
 */
@OptIn(MapboxDelicateApi::class)
@Composable
fun BuildingAnnotation(
    points: List<Point>,
    @ColorInt fillExtrusionColor: Int = 0xFFFF0000.toInt(),
    fillExtrusionOpacity: Double = 0.8,
    fillExtrusionHeight: Double = 50.0,
    fillExtrusionBase: Double = 0.0
) {
    val uuid = remember { UUID.randomUUID() }
    val sourceId = "building-annotation-source-$uuid"
    val layerId = "building-annotation-layer-$uuid"

    val polygon = Polygon.fromLngLats(listOf(points))
    val feature = Feature.fromGeometry(polygon)

    val sourceState = rememberGeoJsonSourceState(sourceId) {
        data = GeoJSONData(listOf(feature))
    }

    val color = remember(fillExtrusionColor) {
        Color(fillExtrusionColor)
    }

    FillExtrusionLayer(
        layerId = layerId,
        sourceState = sourceState
    ) {
        this.fillExtrusionHeight = DoubleValue(fillExtrusionHeight)
        this.fillExtrusionBase = DoubleValue(fillExtrusionBase)
        this.fillExtrusionColor = ColorValue(color)
        this.fillExtrusionOpacity = DoubleValue(fillExtrusionOpacity)
    }
}
