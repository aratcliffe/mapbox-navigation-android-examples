package com.mapbox.navigation.examples.standalone.arcannotation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotationState
import com.mapbox.maps.extension.compose.style.GenericStyle
import com.mapbox.maps.extension.compose.style.slotsContent

/**
 * Demonstrates how to draw a dashed curved arc between two points using ArcAnnotation.
 *
 * The arc can be used to indicate a walking path between locations, such as from a
 * parking area to a building entrance.
 */
class ArcAnnotationActivity : AppCompatActivity() {

    private val origin = Point.fromLngLat(-122.413590602335, 37.7654159350627)
    private val destination = Point.fromLngLat(-122.41399208364109, 37.76562945703791)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MapView()
        }
    }

    @Composable
    private fun MapView() {
        val mapViewportState = rememberMapViewportState {
            setCameraOptions {
                center(
                    Point.fromLngLat(
                        (origin.longitude() + destination.longitude()) / 2,
                        (origin.latitude() + destination.latitude()) / 2
                    )
                )
                zoom(19.0)
                pitch(0.0)
                bearing(85.0)
            }
        }

        val originState = remember {
            CircleAnnotationState().apply {
                circleRadius = 8.0
                circleColor = Color(0xFFFF69B4)
                circleStrokeWidth = 2.0
                circleStrokeColor = Color.White
            }
        }
        val destinationState = remember {
            CircleAnnotationState().apply {
                circleRadius = 8.0
                circleColor = Color(0xFF007AFF)
                circleStrokeWidth = 2.0
                circleStrokeColor = Color.White
            }
        }

        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
        ) {
            GenericStyle(style = "mapbox://styles/mapbox/standard", slotsContent = slotsContent {
                slot("top") {
                    ArcAnnotation(
                        start = origin,
                        end = destination,
                    )
                }
            })
            CircleAnnotation(point = origin, circleAnnotationState = originState)
            CircleAnnotation(point = destination, circleAnnotationState = destinationState)
        }
    }
}
