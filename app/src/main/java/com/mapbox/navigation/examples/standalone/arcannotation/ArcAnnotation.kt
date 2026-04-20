package com.mapbox.navigation.examples.standalone.arcannotation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.DisposableMapEffect
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfConstants
import java.util.UUID

/**
 * A Composable that draws a dashed curved arc between two points on the map.
 *
 * The arc uses a quadratic bezier curve with a perpendicular control point scaled
 * to the line distance, producing a subtle curve that grows proportionally with
 * the distance between the two points.
 *
 * @param start The origin coordinate
 * @param end The destination coordinate
 * @param lineColor The color of the arc line (CSS color string)
 * @param lineWidth The stroke width in pixels
 * @param lineOpacity The opacity of the arc (0.0–1.0)
 * @param lineDashArray Dash pattern as [dashLength, gapLength] in multiples of line width
 */
@Composable
@MapboxMapComposable
fun ArcAnnotation(
    start: Point,
    end: Point,
    lineColor: String = "#007afc",
    lineWidth: Double = 3.0,
    lineOpacity: Double = 0.8,
    lineDashArray: List<Double> = listOf(4.0, 2.0),
) {
    val arcPoints = remember(start, end) { computeArcPoints(start, end) }

    DisposableMapEffect(arcPoints, lineColor, lineWidth, lineOpacity, lineDashArray) { mapView ->
        val id = UUID.randomUUID().toString()
        val sourceId = "arc-annotation-source-$id"
        val layerId = "arc-annotation-layer-$id"

        mapView.mapboxMap.getStyle { style ->
            style.addSource(
                geoJsonSource(sourceId) {
                    geometry(LineString.fromLngLats(arcPoints))
                }
            )
            style.addLayer(
                lineLayer(layerId, sourceId) {
                    lineColor(lineColor)
                    lineWidth(lineWidth)
                    lineOpacity(lineOpacity)
                    lineCap(LineCap.ROUND)
                    lineDasharray(lineDashArray)
                    slot("top")
                }
            )
        }

        onDispose {
            mapView.mapboxMap.getStyle { style ->
                style.getLayer(layerId)?.let { style.removeStyleLayer(layerId) }
                style.getSource(sourceId)?.let { style.removeStyleSource(sourceId) }
            }
        }
    }
}

/**
 * Computes 101 points along a quadratic bezier arc between [start] and [end].
 */
private fun computeArcPoints(start: Point, end: Point): List<Point> {
    val distanceMeters = TurfMeasurement.distance(start, end, TurfConstants.UNIT_METERS)

    val arcHeight = when {
        distanceMeters < 10 -> distanceMeters * 0.35
        distanceMeters < 100 -> distanceMeters * 0.25
        else -> minOf(distanceMeters * 0.05, 8.0)
    }

    val mid = TurfMeasurement.midpoint(start, end)
    val lineBearing = TurfMeasurement.bearing(start, end)

    val dx = end.longitude() - start.longitude()
    val dy = end.latitude() - start.latitude()

    val offsetAngle = if (dx * dy >= 0) 90.0 else -90.0

    val controlBearing = lineBearing + offsetAngle
    val control =
        TurfMeasurement.destination(mid, arcHeight, controlBearing, TurfConstants.UNIT_METERS)

    val steps = 100
    return (0..steps).map { i ->
        val t = i.toDouble() / steps
        val mt = 1.0 - t
        val lng =
            mt * mt * start.longitude() + 2 * mt * t * control.longitude() + t * t * end.longitude()
        val lat =
            mt * mt * start.latitude() + 2 * mt * t * control.latitude() + t * t * end.latitude()
        Point.fromLngLat(lng, lat)
    }
}