package com.mapbox.navigation.examples.standalone.buildingannotation

import androidx.annotation.ColorInt
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillExtrusionLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.utils.ColorUtils
import java.util.concurrent.atomic.AtomicLong

/**
 * Manages the lifecycle of building annotations on the map.
 *
 * **Note:** Due to FillExtrusionLayer limitations, `fillExtrusionOpacity` is applied uniformly
 * to all buildings. Individual annotation opacity values are ignored.
 *
 * Usage for Android Views:
 * ```
 * val manager = BuildingAnnotationManager(mapView)
 * // Set defaults for all annotations
 * manager.fillExtrusionColor = 0xFF00FF00.toInt()  // Green
 * manager.fillExtrusionOpacity = 0.9
 * manager.fillExtrusionHeight = 50.0
 *
 * manager.annotations = listOf(
 *     BuildingAnnotationOptions(
 *         points = building1Points
 *         // Uses manager defaults
 *     ),
 *     BuildingAnnotationOptions(
 *         points = building2Points,
 *         fillExtrusionHeight = 75.0  // Override height for this building
 *     )
 * )
 *
 * // To clean up annotations
 * manager.annotations = emptyList()
 * ```
 */
class BuildingAnnotationManager(private val mapView: MapView) {

    private val sourceId: String
    private val layerId: String
    private var isInitialized = false

    init {
        val id = idGenerator.incrementAndGet()
        sourceId = "building-annotation-source-$id"
        layerId = "building-annotation-layer-$id"
    }

    /**
     * The list of annotations currently displayed on the map.
     * Setting this property will automatically update the map.
     */
    var annotations: List<BuildingAnnotationOptions> = emptyList()
        set(value) {
            field = value
            updateAnnotations()
        }

    /**
     * The default fillExtrusionColor for all annotations added to this annotation manager
     * if not overwritten by individual annotation settings.
     * Default value: 0xFF3489F9 (blue)
     */
    @ColorInt
    var fillExtrusionColor: Int = 0xFF3489F9.toInt()
        set(value) {
            field = value
            updateAnnotations()
        }

    /**
     * The default fillExtrusionOpacity for all annotations added to this annotation manager.
     * This is a layer-level property that applies uniformly to all annotations.
     * Individual annotation `fillExtrusionOpacity` values are ignored.
     * Value range: [0, 1], where 0 is fully transparent and 1 is fully opaque.
     * Default value: 0.8
     */
    var fillExtrusionOpacity: Double = 0.8
        set(value) {
            field = value
            updateLayerOpacity()
        }

    /**
     * The default fillExtrusionHeight for all annotations added to this annotation manager
     * if not overwritten by individual annotation settings.
     * Default value: 50.0 meters
     */
    var fillExtrusionHeight: Double = 50.0
        set(value) {
            field = value
            updateAnnotations()
        }

    /**
     * The default fillExtrusionBase for all annotations added to this annotation manager
     * if not overwritten by individual annotation settings.
     * Default value: 0.0 meters
     */
    var fillExtrusionBase: Double = 0.0
        set(value) {
            field = value
            updateAnnotations()
        }

    private fun setupLayer() {
        if (isInitialized) return

        mapView.mapboxMap.getStyle { style ->
            try {
                // Create source with empty FeatureCollection
                style.addSource(
                    geoJsonSource(sourceId) {
                        featureCollection(FeatureCollection.fromFeatures(emptyList()))
                    }
                )

                // Create layer with data-driven styling using expressions
                // Note: fillExtrusionOpacity must be constant (doesn't support data-driven expressions)
                style.addLayer(
                    fillExtrusionLayer(layerId, sourceId) {
                        fillExtrusionColor(get("color"))
                        fillExtrusionHeight(get("height"))
                        fillExtrusionBase(get("base"))
                        fillExtrusionOpacity(fillExtrusionOpacity)
                        visibility(Visibility.VISIBLE)
                    }
                )

                isInitialized = true
            } catch (e: Exception) {
                // Silently handle errors
            }
        }
    }

    private fun updateAnnotations() {
        setupLayer()
        if (!isInitialized) return

        mapView.mapboxMap.getStyle { style ->
            try {
                val features = annotations.map { createFeature(it) }
                val featureCollection = FeatureCollection.fromFeatures(features)

                style.getSource(sourceId)?.let { source ->
                    (source as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)?.data(
                        featureCollection.toJson()
                    )
                }
            } catch (e: Exception) {
                // Silently handle errors
            }
        }
    }

    private fun createFeature(annotation: BuildingAnnotationOptions): Feature {
        val polygon = Polygon.fromLngLats(listOf(annotation.points))
        return Feature.fromGeometry(polygon).apply {
            addStringProperty("color", ColorUtils.colorToRgbaString(
                annotation.fillExtrusionColor ?: fillExtrusionColor
            ))
            addNumberProperty("height", annotation.fillExtrusionHeight ?: fillExtrusionHeight)
            addNumberProperty("base", annotation.fillExtrusionBase ?: fillExtrusionBase)
        }
    }

    private fun updateLayerOpacity() {
        if (!isInitialized) return

        mapView.mapboxMap.getStyle { style ->
            try {
                style.getLayer(layerId)?.let { layer ->
                    (layer as? com.mapbox.maps.extension.style.layers.generated.FillExtrusionLayer)?.fillExtrusionOpacity(
                        fillExtrusionOpacity
                    )
                }
            } catch (e: Exception) {
                // Silently handle errors
            }
        }
    }

    companion object {
        private val idGenerator = AtomicLong(0)
    }
}
