package com.mapbox.navigation.examples.standalone.buildingannotation

import androidx.annotation.ColorInt
import com.mapbox.geojson.Point
import java.util.UUID

/**
 * Represents options for creating a 3D building annotation.
 *
 * This data class holds the configuration for rendering a 3D building on the map.
 * It follows the SDK pattern of CircleAnnotationOptions, PolygonAnnotationOptions, etc.
 *
 * @param id Unique identifier for the annotation (auto-generated if not provided)
 * @param points The list of coordinates defining the building footprint polygon (required)
 * @param fillExtrusionColor The color of the building extrusion (ARGB Int)
 * @param fillExtrusionOpacity The opacity of the building extrusion (0.0-1.0)
 * @param fillExtrusionHeight The height of the building extrusion in meters
 * @param fillExtrusionBase The base elevation of the building extrusion in meters
 */
data class BuildingAnnotationOptions(
    val id: String = UUID.randomUUID().toString(),
    val points: List<Point>,
    @ColorInt val fillExtrusionColor: Int? = null,
    val fillExtrusionOpacity: Double? = null,
    val fillExtrusionHeight: Double? = null,
    val fillExtrusionBase: Double? = null
) {
    internal val sourceId: String get() = "building-annotation-source-$id"
    internal val layerId: String get() = "building-annotation-layer-$id"
}
