package pt.isel_leic_g09.kmp_mapbox

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import co.touchlab.kermit.Logger
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.OnScaleListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMoveListener
import com.mapbox.maps.plugin.gestures.addOnScaleListener
import com.mapbox.maps.plugin.locationcomponent.location
import dev.jordond.compass.Location
import pt.isel_leic_g09.kmp_mapbox.configs.MapConfig

@Composable
actual fun Map(
    accessToken: String,
    vm: MapViewModel,
    config: MapConfig,
    extraOverlays: List<MapOverlayAction>,
    modifier: Modifier,
) {
    LaunchedEffect(Unit) {
        vm.camera.savedZoom = config.initialZoom
    }

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val userLocation by vm.locationProvider.locationUpdates.collectAsState(null)

    FollowUserEffect(
        userLocation = userLocation,
        mapViewRef = mapViewRef.value,
        vm = vm,
        config = config,
    )

    RouteDrawingEffect(
        mapViewRef = mapViewRef.value,
        vm = vm,
        config = config,
    )

    Box(modifier = modifier) {
        if (userLocation == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            AndroidView(
                factory = { context ->
                    buildMapView(context, accessToken, vm, config).also {
                        mapViewRef.value = it
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            MapOverlayButtons(
                vm = vm,
                config = config,
                extraOverlays = extraOverlays,
            )
        }
    }
}

@Composable
private fun FollowUserEffect(
    userLocation: Location?,
    mapViewRef: MapView?,
    vm: MapViewModel,
    config: MapConfig,
) {
    LaunchedEffect(userLocation, mapViewRef, vm.camera.followUser) {
        if (!vm.camera.followUser) return@LaunchedEffect
        userLocation ?: return@LaunchedEffect
        mapViewRef ?: return@LaunchedEffect

        vm.camera.isAnimating = true
        mapViewRef.mapboxMap.flyTo(
            CameraOptions
                .Builder()
                .center(
                    Point.fromLngLat(
                        userLocation.coordinates.longitude,
                        userLocation.coordinates.latitude,
                    ),
                ).zoom(vm.camera.savedZoom)
                .build(),
            MapAnimationOptions.mapAnimationOptions {
                duration(config.animationDuration)
            },
        )
    }
}

@Composable
private fun RouteDrawingEffect(
    mapViewRef: MapView?,
    vm: MapViewModel,
    config: MapConfig,
) {
    var previousSize by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(vm.route.routeId, mapViewRef) {
        previousSize = 0
    }

    LaunchedEffect(vm.route.points.size, mapViewRef, vm.route.routeId) {
        mapViewRef ?: return@LaunchedEffect
        val style = mapViewRef.mapboxMap.style ?: return@LaunchedEffect
        val currentSize = vm.route.points.size

        if (vm.route.completedRoute != null && currentSize == 0) {
            previousSize = 0
            return@LaunchedEffect
        }

        when {
            currentSize == 0 -> clearRoute(style, vm.route.routeId)
            currentSize > previousSize -> addPointToMap(style, vm.route.routeId, vm.route.points, config)
            currentSize < previousSize -> removeLastPointFromMap(style, vm.route.routeId, index = currentSize)
        }

        previousSize = currentSize
    }
}

private fun addPointToMap(
    style: Style,
    routeId: Int,
    points: List<GeoPoint>,
    config: MapConfig,
) {
    val index = points.size - 1
    val point = points.last()

    style.addSource(
        geoJsonSource("route-$routeId-point-$index") {
            geometry(Point.fromLngLat(point.longitude, point.latitude))
        },
    )
    style.addLayer(
        circleLayer(
            "route-$routeId-point-layer-$index",
            "route-$routeId-point-$index",
        ) {
            circleRadius(config.drawRouteConfig.pointRadius)
            circleColor(config.drawRouteConfig.pointColor.toArgb())
        },
    )

    if (index > 0) {
        val prev = points[index - 1]
        style.addSource(
            geoJsonSource("route-$routeId-line-$index") {
                geometry(
                    LineString.fromLngLats(
                        listOf(
                            Point.fromLngLat(prev.longitude, prev.latitude),
                            Point.fromLngLat(point.longitude, point.latitude),
                        ),
                    ),
                )
            },
        )
        style.addLayerBelow(
            lineLayer(
                "route-$routeId-line-layer-$index",
                "route-$routeId-line-$index",
            ) {
                lineColor(config.drawRouteConfig.lineColor.toArgb())
                lineWidth(config.drawRouteConfig.lineWidth)
                lineCap(LineCap.ROUND)
                lineJoin(LineJoin.ROUND)
            },
            "route-$routeId-point-layer-${index - 1}",
        )
    }
}

private fun removeLastPointFromMap(
    style: Style,
    routeId: Int,
    index: Int,
) {
    if (style.styleLayerExists("route-$routeId-point-layer-$index")) {
        style.removeStyleLayer("route-$routeId-point-layer-$index")
        style.removeStyleSource("route-$routeId-point-$index")
    }
    if (style.styleLayerExists("route-$routeId-line-layer-$index")) {
        style.removeStyleLayer("route-$routeId-line-layer-$index")
        style.removeStyleSource("route-$routeId-line-$index")
    }
}

private fun clearRoute(
    style: Style,
    routeId: Int,
) {
    val prefix = "route-$routeId-"
    style.styleLayers
        .filter { it.id.startsWith(prefix) }
        .forEach { style.removeStyleLayer(it.id) }
    style.styleSources
        .filter { it.id.startsWith(prefix) }
        .forEach { style.removeStyleSource(it.id) }
}

private fun rebuildRoute(
    style: Style,
    vm: MapViewModel,
    config: MapConfig,
) {
    if (vm.route.points.isNotEmpty()) {
        vm.route.points.forEachIndexed { index, _ ->
            addPointToMap(style, vm.route.routeId, vm.route.points.take(index + 1), config)
        }
    }
}

private fun buildMapView(
    context: Context,
    accessToken: String,
    vm: MapViewModel,
    config: MapConfig,
): MapView {
    MapboxOptions.accessToken = accessToken
    return MapView(context).apply {
        mapboxMap.loadStyle(config.styleUri) {
            restoreCamera(mapboxMap, vm, config)
            this@apply.location.updateSettings {
                enabled = true
                pulsingEnabled = config.locationPulseAnimation
                showAccuracyRing = config.showAccuracyRing
            }
            Logger.i("Entering rebuildRoute")
            rebuildRoute(it, vm, config)
        }
        registerGestureListeners(mapboxMap, vm)
        registerIdleListener(mapboxMap, vm)
    }
}

private fun restoreCamera(
    mapboxMap: MapboxMap,
    vm: MapViewModel,
    config: MapConfig,
) {
    val lat = vm.camera.savedLatitude
    val lng = vm.camera.savedLongitude
    if (lat != null && lng != null) {
        mapboxMap.setCamera(
            CameraOptions
                .Builder()
                .center(Point.fromLngLat(lng, lat))
                .zoom(vm.camera.savedZoom)
                .build(),
        )
    } else {
        mapboxMap.setCamera(
            CameraOptions
                .Builder()
                .zoom(config.initialZoom)
                .build(),
        )
    }
}

private fun registerGestureListeners(
    mapboxMap: MapboxMap,
    vm: MapViewModel,
) {
    mapboxMap.addOnMoveListener(
        object : OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) {
                vm.camera.stopFollowing()
            }

            override fun onMove(detector: MoveGestureDetector) = false

            override fun onMoveEnd(detector: MoveGestureDetector) { }
        },
    )

    mapboxMap.addOnScaleListener(
        object : OnScaleListener {
            override fun onScaleBegin(detector: StandardScaleGestureDetector) {
                vm.camera.stopFollowing()
            }

            override fun onScale(detector: StandardScaleGestureDetector) { }

            override fun onScaleEnd(detector: StandardScaleGestureDetector) {
                vm.camera.savedZoom = mapboxMap.cameraState.zoom
            }
        },
    )

    mapboxMap.addOnMapClickListener { point ->
        if (vm.route.isDrawingMode) {
            vm.route.addPoint(
                GeoPoint(
                    latitude = point.latitude(),
                    longitude = point.longitude(),
                    altitude = point.altitude(),
                ),
            )
            true
        } else {
            false
        }
    }
}

private fun registerIdleListener(
    mapboxMap: MapboxMap,
    vm: MapViewModel,
) {
    mapboxMap.subscribeMapIdle {
        if (!vm.camera.isAnimating) {
            val cam = mapboxMap.cameraState
            vm.camera.saveCameraPosition(
                cam.center.latitude(),
                cam.center.longitude(),
            )
        }
        vm.camera.isAnimating = false
    }
}
