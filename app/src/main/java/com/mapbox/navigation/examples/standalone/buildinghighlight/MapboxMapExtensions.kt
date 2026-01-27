package com.mapbox.navigation.examples.standalone.buildinghighlight

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.None
import com.mapbox.geojson.Point
import com.mapbox.maps.FeatureStateOperationCallback
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.dsl.generated.lte
import com.mapbox.maps.interactions.standard.generated.StandardBuildings
import com.mapbox.maps.interactions.standard.generated.StandardBuildingsState

/**
 * Extension functions for building selection.
 */

private const val BUILDING_SELECTION_EXPRESSION_ID = 1000

/**
 * Selects the building on the map at the specified coordinate.
 *
 * @param coordinate The coordinate of the building to select.
 * @param importId Provide if you import the style instead of loading it directly.
 */
fun MapboxMap.selectBuilding(
    coordinate: Point,
    importId: String? = null
) {
    val descriptor = StandardBuildings(importId)

    val expression = lte {
        distance(coordinate)
        literal(0)
    }

    val state = StandardBuildingsState {
        select(true)
    }

    setFeatureStateExpression(
        featureStateExpressionId = BUILDING_SELECTION_EXPRESSION_ID,
        featureset = descriptor,
        expression = expression,
        state = state,
        callback = object : FeatureStateOperationCallback {
            override fun run(expected: Expected<String, None>) {}
        }
    )
}

/**
 * Removes the current building selection from the map.
 *
 * Clears any building highlights that were set by [selectBuilding].
 */
fun MapboxMap.deselectBuilding() {
    removeFeatureStateExpression(
        featureStateExpressionId = BUILDING_SELECTION_EXPRESSION_ID,
        callback = object : FeatureStateOperationCallback {
            override fun run(expected: Expected<String, None>) {}
        }
    )
}
