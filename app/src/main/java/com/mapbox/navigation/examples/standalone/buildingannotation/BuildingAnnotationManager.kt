package com.mapbox.navigation.examples.standalone.buildingannotation

import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import android.util.Log
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillExtrusionLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolZOrder
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlin.math.abs
import com.mapbox.geojson.Polygon as GeoJsonPolygon
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
 * manager.fillExtrusionColor = "#00FF00"  // Green
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
    private val symbolSourceId: String
    private val symbolLayerId: String
    private var isInitialized = false

    init {
        val id = idGenerator.incrementAndGet()
        sourceId = "building-annotation-source-$id"
        layerId = "building-annotation-layer-$id"
        symbolSourceId = "building-annotation-symbol-source-$id"
        symbolLayerId = "building-annotation-symbol-layer-$id"
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
     * Default value: "#3489F9" (blue)
     */
    var fillExtrusionColor: String = "#3489F9"
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

    /**
     * The default text color for labels in the day light preset.
     * Default value: "#404040", matching the Standard style road label color
     */
    var textColor: String = "#404040"
        set(value) {
            field = value
            updateAnnotations()
        }

    /**
     * The default text color for labels in the night light preset.
     * Falls back to [textColor] if null.
     * Default value: "#FFFFFF"
     */
    var textColorNight: String = "#FFFFFF"
        set(value) {
            field = value
            updateAnnotations()
        }

    /**
     * The default text size for labels in sp if not overwritten by individual annotation settings.
     * Default value: 16.0
     */
    var textSize: Double = 16.0
        set(value) {
            field = value
            updateAnnotations()
        }

    /**
     * The font stack for labels. Uses the first available font from the list.
     * Default value: ["DIN Pro Medium", "Arial Unicode MS Regular"]
     */
    var textFont: List<String> = listOf("DIN Pro Medium", "Arial Unicode MS Regular")
        set(value) {
            field = value
            updateSymbolLayerStyle()
        }

    /**
     * The Mapbox Standard style slot to insert layers into (e.g. "bottom", "middle", "top").
     * Must be set before the first [annotations] assignment, as slot is applied at layer creation
     * time and cannot be changed afterwards.
     * Default value: null (no slot — layers inserted at the top of the layer stack)
     */
    var slot: String? = null

    private fun setupLayer() {
        if (isInitialized) return
        mapView.mapboxMap.getStyle { style ->
            if (isInitialized) return@getStyle

            try {
                style.addSource(
                    geoJsonSource(sourceId) {
                        featureCollection(FeatureCollection.fromFeatures(emptyList()))
                    }
                )

                style.addLayer(
                    fillExtrusionLayer(layerId, sourceId) {
                        fillExtrusionColor(get("color"))
                        fillExtrusionHeight(get("height"))
                        fillExtrusionBase(get("base"))
                        fillExtrusionOpacity(fillExtrusionOpacity)
                        visibility(Visibility.VISIBLE)
                        slot?.let { slot(it) }
                    }
                )
            } catch (e: Exception) {
                Log.e("BuildingAnnotation", "Failed to set up fill extrusion layer", e)
                return@getStyle
            }

            try {
                style.addSource(
                    geoJsonSource(symbolSourceId) {
                        featureCollection(FeatureCollection.fromFeatures(emptyList()))
                    }
                )

                // Symbol layer elevated to rooftop via symbolZElevate
                // Requires symbolPlacement = POINT and symbolZOrder = AUTO
                style.addLayer(
                    symbolLayer(symbolLayerId, symbolSourceId) {
                        symbolPlacement(SymbolPlacement.POINT)
                        symbolZElevate(true)
                        symbolZOrder(SymbolZOrder.AUTO)
                        textField(get("label"))
                        textVariableAnchor(listOf("top", "bottom", "left", "right"))
                        textAnchor(get("textAnchor"))
                        textEmissiveStrength(1.0)
                        textColor(
                            Expression.fromRaw("""["interpolate",["linear"],["measure-light","brightness"],0.25,["get","textColorNight"],0.3,["get","textColorDay"]]""")
                        )
                        textHaloColor(
                            Expression.fromRaw("""["interpolate",["linear"],["measure-light","brightness"],0.25,"#0D0D0D",0.3,"#FFFFFF"]""")
                        )
                        textSize(get("textSize"))
                        textHaloWidth(0.5)
                        textHaloBlur(1.0)
                        textFont(textFont)
                        visibility(Visibility.VISIBLE)
                    }
                )
            } catch (e: Exception) {
                Log.e("BuildingAnnotation", "Failed to set up symbol layer", e)
            }

            isInitialized = true
            updateSources(style)
        }
    }

    private fun updateAnnotations() {
        if (!isInitialized) {
            setupLayer() // will call updateSources once initialized
            return
        }
        mapView.mapboxMap.style?.let { updateSources(it) }
    }

    private fun updateSources(style: com.mapbox.maps.Style) {
        try {
            val features = annotations.map { createFeature(it) }
            val featureCollection = FeatureCollection.fromFeatures(features)

            style.getSource(sourceId)?.let { source ->
                (source as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)?.data(
                    featureCollection.toJson()
                )
            }

            val centroidFeatures = annotations.mapNotNull { createCentroidFeature(it) }
            val centroidCollection = FeatureCollection.fromFeatures(centroidFeatures)
            style.getSource(symbolSourceId)?.let { source ->
                (source as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)?.data(
                    centroidCollection.toJson()
                )
            }
        } catch (e: Exception) {
            Log.e("BuildingAnnotation", "Failed to update sources", e)
        }
    }

    private fun createCentroidFeature(annotation: BuildingAnnotationOptions): Feature? {
        if (annotation.labelText == null) return null
        val buildingFootprint = GeoJsonPolygon.fromLngLats(listOf(annotation.points))
        val centroid = TurfMeasurement.center(Feature.fromGeometry(buildingFootprint)).geometry() as? Point
            ?: annotation.points.first()

        val nearest = annotation.labelPosition ?: centroid
        val dx = nearest.longitude() - centroid.longitude()
        val dy = nearest.latitude() - centroid.latitude()
        val anchor = if (abs(dx) >= abs(dy)) {
            if (dx > 0) "right" else "left"
        } else {
            if (dy > 0) "top" else "bottom"
        }
        // Shift 5 meters inward from the boundary toward the centroid
        val bearing = TurfMeasurement.bearing(nearest, centroid)
        val boundaryToCentroidMeters = TurfMeasurement.distance(nearest, centroid, TurfConstants.UNIT_METERS)
        val insetMeters = minOf(5.0, boundaryToCentroidMeters * 0.5)
        val labelPosition = TurfMeasurement.destination(nearest, insetMeters, bearing, TurfConstants.UNIT_METERS)

        return Feature.fromGeometry(labelPosition).apply {
            addStringProperty("label", annotation.labelText)
            addStringProperty("textAnchor", anchor)
            addStringProperty("textColorDay", annotation.textColor ?: textColor)
            addStringProperty("textColorNight", annotation.textColorNight ?: textColorNight)
            addNumberProperty("textSize", annotation.textSize ?: textSize)
        }
    }

    private fun createFeature(annotation: BuildingAnnotationOptions): Feature {
        val polygon = Polygon.fromLngLats(listOf(annotation.points))
        return Feature.fromGeometry(polygon).apply {
            addStringProperty("color", annotation.fillExtrusionColor ?: fillExtrusionColor)
            addNumberProperty("height", annotation.fillExtrusionHeight ?: fillExtrusionHeight)
            addNumberProperty("base", annotation.fillExtrusionBase ?: fillExtrusionBase)
        }
    }

    private fun updateSymbolLayerStyle() {
        if (!isInitialized) return
        val style = mapView.mapboxMap.style ?: return
        (style.getLayer(symbolLayerId) as? SymbolLayer)?.textFont(textFont)
    }

    private fun updateLayerOpacity() {
        if (!isInitialized) return
        val style = mapView.mapboxMap.style ?: return
        (style.getLayer(layerId) as? com.mapbox.maps.extension.style.layers.generated.FillExtrusionLayer)
            ?.fillExtrusionOpacity(fillExtrusionOpacity)
    }

    companion object {
        private val idGenerator = AtomicLong(0)
    }
}
