package com.mapbox.navigation.examples.standalone.custommaneuver

import android.view.View
import android.view.ViewGroup
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.ui.components.maneuver.view.MapboxManeuverView

// Tags for tracking listener setup and storing state/callbacks
// Using R.id-style Int keys for View.setTag(int, Object)
private val TAG_LISTENERS_INITIALIZED = "fullscreen_listeners_initialized".hashCode()
private val TAG_IS_EXPANDED = "is_expanded".hashCode()
private val TAG_ON_EXPANSION_TOGGLED = "on_expansion_toggled".hashCode()
private val TAG_ON_EXPANDED = "on_expanded".hashCode()
private val TAG_ON_COLLAPSED = "on_collapsed".hashCode()

/**
 * Binds custom layout interactions to the Compose state.
 *
 * @param isExpanded The current state from Compose.
 * @param onExpansionToggled Callback to notify Compose when the user wants to change state.
 * @param onExpanded Optional callback invoked when the maneuver list is expanded.
 * @param onCollapsed Optional callback invoked when the maneuver list is collapsed.
 */
@Suppress("UNCHECKED_CAST")
fun MapboxManeuverView.bindFullscreenState(
    isExpanded: Boolean,
    onExpansionToggled: (Boolean) -> Unit,
    onExpanded: (() -> Unit)? = null,
    onCollapsed: (() -> Unit)? = null
) {
    val closeButton = findViewById<View>(R.id.closeManeuversButton)
    val closeButtonDivider = findViewById<View>(R.id.closeButtonDivider)
    val maneuverHeader = findViewById<View>(R.id.maneuverHeader)
    val upcomingManeuverRecycler = findViewById<View>(R.id.upcomingManeuverRecycler)
    val maneuverCard = findViewById<View>(R.id.maneuver)

    closeButton?.let { button ->
        // Get background from the RecyclerView which has the SDK's styled background
        upcomingManeuverRecycler?.background?.let { background ->
            button.background = background.constantState?.newDrawable()?.mutate()
        }
    }

    // Store state and callbacks as tags so listeners can access the latest values
    setTag(TAG_IS_EXPANDED, isExpanded)
    setTag(TAG_ON_EXPANSION_TOGGLED, onExpansionToggled)
    setTag(TAG_ON_EXPANDED, onExpanded)
    setTag(TAG_ON_COLLAPSED, onCollapsed)

    if (getTag(TAG_LISTENERS_INITIALIZED) != true) {
        maneuverHeader?.setOnClickListener {
            val currentIsExpanded = getTag(TAG_IS_EXPANDED) as? Boolean ?: false
            val currentExpanded = (getTag(TAG_ON_EXPANSION_TOGGLED) as? ((Boolean) -> Unit))
            val currentOnExpanded = getTag(TAG_ON_EXPANDED) as? (() -> Unit)
            val currentOnCollapsed = getTag(TAG_ON_COLLAPSED) as? (() -> Unit)

            if (currentIsExpanded) {
                currentExpanded?.invoke(false)
                currentOnCollapsed?.invoke()
            } else {
                currentExpanded?.invoke(true)
                currentOnExpanded?.invoke()
            }
        }
        closeButton?.setOnClickListener {
            val currentExpanded = (getTag(TAG_ON_EXPANSION_TOGGLED) as? ((Boolean) -> Unit))
            val currentOnCollapsed = getTag(TAG_ON_COLLAPSED) as? (() -> Unit)

            currentExpanded?.invoke(false)
            currentOnCollapsed?.invoke()
        }

        setTag(TAG_LISTENERS_INITIALIZED, true)
    }

    if (isExpanded) {
        updateUpcomingManeuversVisibility(View.VISIBLE)
        closeButton?.visibility = View.VISIBLE
        closeButtonDivider?.visibility = View.VISIBLE

        // Force full height
        maneuverCard?.layoutParams = maneuverCard?.layoutParams?.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    } else {
        updateUpcomingManeuversVisibility(View.GONE)
        closeButton?.visibility = View.GONE
        closeButtonDivider?.visibility = View.GONE

        // Reset to wrap content
        maneuverCard?.layoutParams = maneuverCard?.layoutParams?.apply {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }
}

fun MapboxManeuverView.unbindFullscreenState() {
    // Use direct R.id access instead of getIdentifier
    val maneuverHeader = findViewById<View>(R.id.maneuverHeader)
    val closeButton = findViewById<View>(R.id.closeManeuversButton)

    maneuverHeader?.setOnClickListener(null)
    closeButton?.setOnClickListener(null)

    setTag(TAG_LISTENERS_INITIALIZED, null)
    setTag(TAG_IS_EXPANDED, null)
    setTag(TAG_ON_EXPANSION_TOGGLED, null)
    setTag(TAG_ON_EXPANDED, null)
    setTag(TAG_ON_COLLAPSED, null)
}