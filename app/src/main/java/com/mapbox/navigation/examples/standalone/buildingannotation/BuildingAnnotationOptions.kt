package com.mapbox.navigation.examples.standalone.buildingannotation

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
 * @param fillExtrusionColor The color of the building extrusion (CSS color string, e.g. "#3489F9")
 * @param fillExtrusionOpacity The opacity of the building extrusion (0.0-1.0)
 * @param fillExtrusionHeight The height of the building extrusion in meters
 * @param fillExtrusionBase The base elevation of the building extrusion in meters
 * @param labelText Optional text label to display above the building (elevated to rooftop)
 * @param labelPosition When provided, the label is placed at the nearest point on the building
 *   boundary to this point, with the text anchor set so the label renders inside the polygon.
 *   Falls back to the polygon centroid when null.
 * @param textColor Color of the label text in day light preset (CSS color string)
 * @param textColorNight Color of the label text in night light preset (CSS color string).
 *   Falls back to [textColor] if not provided.
 * @param textSize Size of the label text in sp
 */
data class BuildingAnnotationOptions(
    val id: String = UUID.randomUUID().toString(),
    val points: List<Point>,
    val fillExtrusionColor: String? = null,
    val fillExtrusionOpacity: Double? = null,
    val fillExtrusionHeight: Double? = null,
    val fillExtrusionBase: Double? = null,
    val labelText: String? = null,
    val labelPosition: Point? = null,
    val textColor: String? = null,
    val textColorNight: String? = null,
    val textSize: Double? = null
)
