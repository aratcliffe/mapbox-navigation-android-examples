package com.mapbox.navigation.examples.standalone.buildingannotation

import androidx.annotation.ColorInt
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillExtrusionLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolZOrder
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.utils.ColorUtils
import com.mapbox.turf.TurfMeasurement
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

    /**
     * The default text color for labels in the day light preset.
     * Default value: hsl(0, 0%, 25%) = #404040, matching the Standard style road label color
     */
    @ColorInt
    var textColor: Int = 0xFF404040.toInt()
        set(value) {
            field = value
            updateAnnotations()
        }

    /**
     * The default text color for labels in the night light preset.
     * Falls back to [textColor] if null.
     * Default value: null
     */
    @ColorInt
    var textColorNight: Int = 0xFFFFFFFF.toInt()
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

    private fun setupLayer() {
        if (isInitialized) return

        mapView.mapboxMap.getStyle { style ->
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
                    }
                )

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
                        textAnchor(TextAnchor.TOP)
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

                val centroidFeatures = annotations.mapNotNull { createCentroidFeature(it) }
                val centroidCollection = FeatureCollection.fromFeatures(centroidFeatures)
                style.getSource(symbolSourceId)?.let { source ->
                    (source as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)?.data(
                        centroidCollection.toJson()
                    )
                }
            } catch (e: Exception) {
                // Silently handle errors
            }
        }
    }

    private fun createCentroidFeature(annotation: BuildingAnnotationOptions): Feature? {
        if (annotation.labelText == null) return null
        val buildingFootprint = GeoJsonPolygon.fromLngLats(listOf(annotation.points))
        val centroid = TurfMeasurement.center(Feature.fromGeometry(buildingFootprint)).geometry() as? Point
            ?: annotation.points.first()
        return Feature.fromGeometry(centroid).apply {
            addStringProperty("label", annotation.labelText ?: "")
            addStringProperty("textColorDay", ColorUtils.colorToRgbaString(annotation.textColor ?: textColor))
            addStringProperty("textColorNight", ColorUtils.colorToRgbaString(annotation.textColorNight ?: textColorNight))
            addNumberProperty("textSize", annotation.textSize ?: textSize)
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

    private fun updateSymbolLayerStyle() {
        if (!isInitialized) return

        mapView.mapboxMap.getStyle { style ->
            try {
                style.getLayer(symbolLayerId)?.let { layer ->
                    (layer as? SymbolLayer)?.textFont(textFont)
                }
            } catch (e: Exception) {
                // Silently handle errors
            }
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
