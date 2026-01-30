package com.mapbox.navigation.examples.standalone.buildingannotation

import com.mapbox.geojson.Feature
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillExtrusionLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource

/**
 * Manages the lifecycle of building annotations on the map.
 *
 * This manager handles adding, updating, and removing building annotations.
 * It uses GeoJSONSource and FillExtrusionLayer to render 3D buildings on the map.
 *
 * Usage for Android Views:
 * ```
 * val manager = BuildingAnnotationManager(mapView)
 * manager.annotations = listOf(
 *     BuildingAnnotationOptions(
 *         points = buildingPoints,
 *         fillExtrusionHeight = 50.0
 *         // Uses default blue color (hsl(214, 94%, 59%)) from Mapbox Standard style
 *     )
 * )
 *
 * // To clean up annotations
 * manager.annotations = emptyList()
 * ```
 */
class BuildingAnnotationManager(private val mapView: MapView) {

    /**
     * The list of annotations currently displayed on the map.
     * Setting this property will automatically sync changes to the map.
     */
    var annotations: List<BuildingAnnotationOptions> = emptyList()
        set(value) {
            val old = field
            field = value
            syncAnnotations(old, value)
        }

    private fun syncAnnotations(
        old: List<BuildingAnnotationOptions>,
        new: List<BuildingAnnotationOptions>
    ) {
        val oldIds = old.map { it.id }.toSet()
        val newIds = new.map { it.id }.toSet()

        // Remove annotations that are no longer present
        val toRemove = oldIds - newIds
        toRemove.forEach { id ->
            old.find { it.id == id }?.let { removeAnnotation(it) }
        }

        // Add new annotations
        val toAdd = newIds - oldIds
        toAdd.forEach { id ->
            new.find { it.id == id }?.let { addAnnotation(it) }
        }

        // Update existing annotations if they changed
        val existing = oldIds.intersect(newIds)
        existing.forEach { id ->
            val oldAnnotation = old.find { it.id == id }
            val newAnnotation = new.find { it.id == id }
            if (oldAnnotation != null && newAnnotation != null && oldAnnotation != newAnnotation) {
                removeAnnotation(oldAnnotation)
                addAnnotation(newAnnotation)
            }
        }
    }

    private fun addAnnotation(annotation: BuildingAnnotationOptions) {
        mapView.mapboxMap.getStyle { style ->
            try {
                val polygon = Polygon.fromLngLats(listOf(annotation.points))
                val feature = Feature.fromGeometry(polygon)

                style.addSource(
                    geoJsonSource(annotation.sourceId) {
                        data(feature.toJson())
                    }
                )

                style.addLayer(
                    fillExtrusionLayer(annotation.layerId, annotation.sourceId) {
                        fillExtrusionHeight(annotation.fillExtrusionHeight ?: 50.0)
                        fillExtrusionBase(annotation.fillExtrusionBase ?: 0.0)
                        // Default color matches Mapbox Standard style buildingSelectColor: hsl(214, 94%, 59%)
                        fillExtrusionColor(annotation.fillExtrusionColor ?: 0xFF3489F9.toInt())
                        fillExtrusionOpacity(annotation.fillExtrusionOpacity ?: 0.8)
                        visibility(Visibility.VISIBLE)
                    }
                )
            } catch (e: Exception) {
                // Silently handle errors (source/layer may already exist)
            }
        }
    }

    private fun removeAnnotation(annotation: BuildingAnnotationOptions) {
        mapView.mapboxMap.getStyle { style ->
            try {
                style.removeStyleLayer(annotation.layerId)
                style.removeStyleSource(annotation.sourceId)
            } catch (e: Exception) {
                // Silently handle errors
            }
        }
    }
}
