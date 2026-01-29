package com.mapbox.navigation.examples.standalone.buildingannotation

import com.mapbox.geojson.Geometry
import com.mapbox.geojson.MultiPolygon
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.interactions.standard.generated.StandardBuildings
import com.mapbox.maps.interactions.standard.generated.StandardBuildingsFeature
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Queries building features at the specified point.
 *
 * @param point The coordinate to query for buildings
 * @return List of StandardBuildingsFeature found at the point, or empty list if none found
 */
@OptIn(MapboxExperimental::class)
suspend fun MapboxMap.queryBuildings(
    point: Point
): List<StandardBuildingsFeature> = suspendCoroutine { continuation ->
    val screenCoordinate = pixelForCoordinate(point)
    val geometry = RenderedQueryGeometry(screenCoordinate)

    val descriptor = StandardBuildings()

    queryRenderedFeatures(
        descriptor = descriptor,
        geometry = geometry,
        filter = null,
        callback = { features ->
            continuation.resume(features)
        }
    )
}

/**
 * Extension to convert Geometry to a list of points for polygon coordinates.
 *
 * Extracts the outer ring coordinates from Polygon or MultiPolygon geometries.
 * The caller is responsible for using this to extract coordinates from
 * FeaturesetFeature.geometry.
 *
 * @return List of points forming the polygon, or null if geometry is not a polygon type
 */
fun Geometry.toPoints(): List<Point>? {
    return when (this) {
        is Polygon -> coordinates().firstOrNull()
        is MultiPolygon -> coordinates().firstOrNull()?.firstOrNull()
        else -> null
    }
}
