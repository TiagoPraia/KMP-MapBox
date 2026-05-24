package pt.isel_leic_g09.kmp_mapbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.Text
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.gestures.addOnMoveListener
import com.mapbox.maps.plugin.gestures.addOnScaleListener
import com.mapbox.maps.plugin.locationcomponent.location

@Composable
actual fun Map(
    accessToken: String,
    vm: MapViewModel,
    modifier: Modifier,
) {
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val userLocation by vm.locationProvider.locationUpdates.collectAsState(null)

    // Segue o utilizador com transição suave
    LaunchedEffect(userLocation, mapViewRef.value, vm.followUser) {
        if (!vm.followUser) return@LaunchedEffect
        userLocation ?: return@LaunchedEffect
        val mapView = mapViewRef.value ?: return@LaunchedEffect

        vm.isAnimating = true
        mapView.mapboxMap.flyTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(
                    userLocation!!.coordinates.longitude,
                    userLocation!!.coordinates.latitude,
                ))
                .zoom(vm.savedZoom)
                .build(),
            MapAnimationOptions.mapAnimationOptions {
                duration(800)
            }
        )
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                MapboxOptions.accessToken = accessToken
                MapView(context).apply {
                    mapboxMap.loadStyle(Style.OUTDOORS) {
                        if (vm.savedLatitude != null && vm.savedLongitude != null) {
                            mapboxMap.setCamera(
                                CameraOptions.Builder()
                                    .center(Point.fromLngLat(vm.savedLongitude!!, vm.savedLatitude!!))
                                    .zoom(vm.savedZoom ?: 17.0)
                                    .build()
                            )
                        }
                        this@apply.location.updateSettings {
                            enabled = true
                            pulsingEnabled = false
                        }
                    }

                    // Utilizador mexe na câmara → para de seguir
                    mapboxMap.addOnMoveListener(object : com.mapbox.maps.plugin.gestures.OnMoveListener {
                        override fun onMoveBegin(detector: com.mapbox.android.gestures.MoveGestureDetector) {
                            vm.stopFollowing()
                        }
                        override fun onMove(detector: com.mapbox.android.gestures.MoveGestureDetector) = false
                        override fun onMoveEnd(detector: com.mapbox.android.gestures.MoveGestureDetector) {}
                    })

                    mapboxMap.addOnScaleListener(object : com.mapbox.maps.plugin.gestures.OnScaleListener {
                        override fun onScaleBegin(detector: com.mapbox.android.gestures.StandardScaleGestureDetector) {
                            vm.stopFollowing()
                        }
                        override fun onScale(detector: com.mapbox.android.gestures.StandardScaleGestureDetector) { }
                        override fun onScaleEnd(detector: com.mapbox.android.gestures.StandardScaleGestureDetector) {
                            vm.savedZoom = mapboxMap.cameraState.zoom
                        }
                    })

                    mapboxMap.subscribeMapIdle {
                        if(!vm.isAnimating) {
                            val cam = mapboxMap.cameraState
                            vm.saveCameraPosition(
                                cam.center.latitude(),
                                cam.center.longitude(),
                            )
                        }
                        vm.isAnimating = false
                    }

                    mapViewRef.value = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        FloatingActionButton(
            onClick = { vm.resumeFollowing() },
            shape = CircleShape,
            containerColor = if (vm.followUser) Color(0xFF1A73E8) else Color(0xFF9E9E9E),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(30.dp),
        ) { }
    }
}