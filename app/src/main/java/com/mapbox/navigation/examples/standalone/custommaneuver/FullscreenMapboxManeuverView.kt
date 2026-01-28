package com.mapbox.navigation.examples.standalone.custommaneuver

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.tripdata.maneuver.model.Maneuver
import com.mapbox.navigation.ui.components.maneuver.view.MapboxManeuverView

/**
 * A Composable wrapper for [MapboxManeuverView] with fullscreen expansion support.
 *
 * This composable integrates the custom fullscreen maneuver list behavior into Jetpack Compose,
 * handling state management, and back button navigation.
 *
 * ## Requirements
 *
 * 1. Custom `mapbox_maneuver_layout.xml` in your `res/layout/` folder
 * 2. Extension function `bindFullscreenState()` available
 *
 * ## Features
 *
 * - **Collapsed state**: shows next 2 maneuvers (default SDK behavior)
 * - **Expanded state**: fills screen height with all upcoming maneuvers
 * - **Back button handling**: pressing back when expanded collapses the list
 *
 * ## Usage
 *
 * ```kotlin
 * FullscreenMapboxManeuverView(
 *     maneuvers = maneuvers,
 *     modifier = Modifier.fillMaxWidth(),
 *     onExpanded = { analytics.track("ManeuverListExpanded") },
 *     onCollapsed = { analytics.track("ManeuverListCollapsed") }
 * )
 * ```
 *
 * @param maneuvers The list of maneuvers to display
 * @param modifier Optional modifier for the composable
 * @param onExpanded Optional callback invoked when the maneuver list is expanded
 * @param onCollapsed Optional callback invoked when the maneuver list is collapsed
 */
@Composable
fun FullscreenMapboxManeuverView(
    maneuvers: List<Maneuver>,
    modifier: Modifier = Modifier,
    onExpanded: (() -> Unit)? = null,
    onCollapsed: (() -> Unit)? = null
) {
    if (maneuvers.isEmpty()) return

    val (isExpanded, setExpanded) = remember { mutableStateOf(false) }

    BackHandler(enabled = isExpanded) {
        setExpanded(false)
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .then(if (isExpanded) Modifier.fillMaxHeight() else Modifier.wrapContentHeight())
            .zIndex(if (isExpanded) 10f else 0f),
        factory = { context ->
            MapboxManeuverView(context)
        },
        update = { maneuverView ->
            maneuverView.renderManeuvers(ExpectedFactory.createValue(maneuvers))
            maneuverView.bindFullscreenState(
                isExpanded = isExpanded,
                onExpansionToggled = { setExpanded(it) },
                onExpanded = onExpanded,
                onCollapsed = onCollapsed
            )
        },
    )
}
