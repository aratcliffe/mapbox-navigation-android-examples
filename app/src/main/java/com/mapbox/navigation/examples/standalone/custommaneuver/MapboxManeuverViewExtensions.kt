package com.mapbox.navigation.examples.standalone.custommaneuver

import android.view.View
import android.view.ViewGroup
import com.mapbox.navigation.ui.components.maneuver.view.MapboxManeuverView

/**
 * Binds custom layout interactions to the Compose state.

 *
 * @param isExpanded The current state from Compose.
 * @param onExpansionToggled Callback to notify Compose when the user wants to change state.
 * @param onExpanded Optional callback invoked when the maneuver list is expanded.
 * @param onCollapsed Optional callback invoked when the maneuver list is collapsed.
 */
fun MapboxManeuverView.bindFullscreenState(
    isExpanded: Boolean,
    onExpansionToggled: (Boolean) -> Unit,
    onExpanded: (() -> Unit)? = null,
    onCollapsed: (() -> Unit)? = null
) {
    val context = this.context

    val closeButton = findViewById<View>(
        resources.getIdentifier("closeManeuversButton", "id", context.packageName)
    )
    val closeButtonDivider = findViewById<View>(
        resources.getIdentifier("closeButtonDivider", "id", context.packageName)
    )
    val headerLayout = findViewById<View>(
        resources.getIdentifier("mainManeuverLayout", "id", context.packageName)
    )
    val upcomingManeuverRecycler = findViewById<View>(
        resources.getIdentifier("upcomingManeuverRecycler", "id", context.packageName)
    )

    val maneuverCard = findViewById<View>(
        resources.getIdentifier("maneuver", "id", context.packageName)
    )

    closeButton?.let { button ->
        // Get background from the RecyclerView which has the SDK's styled background
        upcomingManeuverRecycler?.background?.let { background ->
            button.background = background.constantState?.newDrawable()?.mutate()
        }
    }

    if (isExpanded) {
        updateUpcomingManeuversVisibility(View.VISIBLE)
        closeButton?.visibility = View.VISIBLE
        closeButtonDivider?.visibility = View.VISIBLE

        // Force full height
        maneuverCard?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
    } else {
        updateUpcomingManeuversVisibility(View.GONE)
        closeButton?.visibility = View.GONE
        closeButtonDivider?.visibility = View.GONE

        // Reset to wrap content
        maneuverCard?.layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    headerLayout?.setOnClickListener {
        if (isExpanded) {
            onExpansionToggled(false)
            onCollapsed?.invoke()
        } else {
            onExpansionToggled(true)
            onExpanded?.invoke()
        }
    }
    closeButton?.setOnClickListener {
        onExpansionToggled(false)
        onCollapsed?.invoke()
    }
}