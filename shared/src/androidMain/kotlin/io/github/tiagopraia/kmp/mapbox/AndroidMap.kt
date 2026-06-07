package io.github.tiagopraia.kmp.mapbox

import android.content.Context
import android.view.Gravity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import co.touchlab.kermit.Logger
import com.google.gson.JsonObject
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.eq
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.get
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.ViewportStatus
import com.mapbox.maps.plugin.viewport.data.DefaultViewportTransitionOptions
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateBearing
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.mapbox.maps.plugin.viewport.data.ViewportStatusChangeReason
import com.mapbox.maps.plugin.viewport.viewport
import io.github.tiagopraia.kmp.mapbox.configs.CircleOverlay
import io.github.tiagopraia.kmp.mapbox.configs.FollowButtonConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapOverlays
import io.github.tiagopraia.kmp.mapbox.configs.PolylineOverlay

@Composable
fun AndroidMap(
    accessToken: String,
    config: MapConfig,
    overlays: MapOverlays,
    onOverlayClick: (id: String) -> Unit,
    onMapReady: () -> Unit,
    onMapClick: ((GeoPoint) -> Boolean)? = null,
    isGpsEnabled: Boolean,
    modifier: Modifier,
) {
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    var cameraMode by rememberSaveable { mutableStateOf(CameraTrackingMode.TRACKING_NORTH) }
    var savedZoom by rememberSaveable { mutableDoubleStateOf(config.initialZoom) }
    var savedLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedLng by rememberSaveable { mutableStateOf<Double?>(null) }
    val overlaysRef = remember { mutableStateOf(overlays) }
    overlaysRef.value = overlays

    OverlayDrawingEffect(
        mapViewRef = mapViewRef.value,
        overlays = overlaysRef.value,
    )

    LaunchedEffect(isGpsEnabled, mapViewRef.value) {
        mapViewRef.value?.location?.updateSettings {
            enabled = isGpsEnabled
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                buildMapView(
                    context = context,
                    accessToken = accessToken,
                    overlaysRef = overlaysRef.value,
                    config = config,
                    isGpsEnabled = isGpsEnabled,
                    initialCameraMode = cameraMode,
                    savedZoom = savedZoom,
                    savedLat = savedLat,
                    savedLng = savedLng,
                    onCameraPositionChanged = { zoom, lat, lng ->
                        savedZoom = zoom
                        savedLat = lat
                        savedLng = lng
                    },
                    onMapReady = onMapReady,
                    onMapClick = onMapClick,
                    onOverlayClick = onOverlayClick,
                    onUserInteraction = {
                        cameraMode = CameraTrackingMode.FREE
                    },
                ).also {
                    mapViewRef.value = it
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (isGpsEnabled) {
            FollowButton(
                mapViewRef = mapViewRef.value,
                cameraMode = cameraMode,
                onModeChanged = { cameraMode = it },
                config = config.followButton,
            )
        }
    }
}

@Composable
fun BoxScope.FollowButton(
    mapViewRef: MapView?,
    cameraMode: CameraTrackingMode,
    onModeChanged: (CameraTrackingMode) -> Unit,
    config: FollowButtonConfig,
) {
    FloatingActionButton(
        onClick = {
            mapViewRef?.let { mapView ->
                val viewport = mapView.viewport

                val nextMode =
                    when (cameraMode) {
                        CameraTrackingMode.FREE -> CameraTrackingMode.TRACKING_NORTH
                        CameraTrackingMode.TRACKING_NORTH -> CameraTrackingMode.TRACKING_COMPASS
                        CameraTrackingMode.TRACKING_COMPASS -> CameraTrackingMode.TRACKING_NORTH
                    }

                val transitionOptions =
                    DefaultViewportTransitionOptions
                        .Builder()
                        .maxDurationMs(800)
                        .build()

                val bearingConfig =
                    if (nextMode == CameraTrackingMode.TRACKING_COMPASS) {
                        FollowPuckViewportStateBearing.SyncWithLocationPuck
                    } else {
                        FollowPuckViewportStateBearing.Constant(NORTH)
                    }

                val zoom = mapView.mapboxMap.cameraState.zoom

                viewport.transitionTo(
                    targetState =
                        viewport.makeFollowPuckViewportState(
                            FollowPuckViewportStateOptions
                                .Builder()
                                .zoom(zoom)
                                .pitch(0.0)
                                .bearing(bearingConfig)
                                .build(),
                        ),
                    transition = viewport.makeDefaultViewportTransition(transitionOptions),
                )

                onModeChanged(nextMode)
            }
        },
        shape = config.buttonShape,
        containerColor = config.followButtonColor,
        modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .then(config.buttonModifier),
    ) {
        Logger.i("Camera Mode: $cameraMode")
        Icon(
            imageVector =
                when (cameraMode) {
                    CameraTrackingMode.FREE -> Icons.Outlined.MyLocation
                    CameraTrackingMode.TRACKING_NORTH -> Icons.Default.MyLocation
                    CameraTrackingMode.TRACKING_COMPASS -> Icons.Default.Explore
                },
            contentDescription = "Camera Mode",
        )
    }
}

@Composable
private fun OverlayDrawingEffect(
    mapViewRef: MapView?,
    overlays: MapOverlays,
) {
    LaunchedEffect(overlays, mapViewRef) {
        val style = mapViewRef?.mapboxMap?.style ?: return@LaunchedEffect
        updateCirclesSource(style, overlays.circles)
        updatePolylinesSource(style, overlays.polylines)
    }
}

private fun updateCirclesSource(
    style: Style,
    circles: List<CircleOverlay>,
) {
    val features =
        circles.map { circle ->
            Feature.fromGeometry(
                Point.fromLngLat(circle.center.longitude, circle.center.latitude),
                JsonObject().apply {
                    addProperty(PROP_OVERLAY_ID, circle.id)
                    addProperty(PROP_IS_CLICKABLE, circle.isClickable)
                    addProperty(PROP_COLOR, circle.colorHex)
                },
            )
        }
    style.getSourceAs<GeoJsonSource>(CIRCLES_SOURCE_ID)?.featureCollection(FeatureCollection.fromFeatures(features))
}

private fun updatePolylinesSource(
    style: Style,
    polylines: List<PolylineOverlay>,
) {
    val features =
        polylines.map { polyline ->
            Feature.fromGeometry(
                LineString.fromLngLats(
                    polyline.points.map { Point.fromLngLat(it.longitude, it.latitude) },
                ),
                JsonObject().apply {
                    addProperty(PROP_OVERLAY_ID, polyline.id)
                    addProperty(PROP_IS_CLICKABLE, polyline.isClickable)
                    addProperty(PROP_COLOR, polyline.colorHex)
                    addProperty(PROP_IS_DASHED, polyline.isDashed)
                },
            )
        }
    style.getSourceAs<GeoJsonSource>(POLYLINES_SOURCE_ID)?.featureCollection(FeatureCollection.fromFeatures(features))
}

private fun initOverlayLayers(
    style: Style,
    config: MapConfig,
) {
    style.addSource(
        geoJsonSource(CIRCLES_SOURCE_ID) {
            featureCollection(FeatureCollection.fromFeatures(emptyList()))
        },
    )
    style.addSource(
        geoJsonSource(POLYLINES_SOURCE_ID) {
            featureCollection(FeatureCollection.fromFeatures(emptyList()))
        },
    )

    style.addLayer(
        lineLayer(POLYLINES_SOLID_LAYER_ID, POLYLINES_SOURCE_ID) {
            lineColor(get(PROP_COLOR))
            lineWidth(config.lineWidth)
            lineCap(LineCap.ROUND)
            lineJoin(LineJoin.ROUND)
            filter(
                eq {
                    get(PROP_IS_DASHED)
                    literal(false)
                },
            )
        },
    )

    style.addLayer(
        lineLayer(POLYLINES_DASHED_LAYER_ID, POLYLINES_SOURCE_ID) {
            lineColor(get(PROP_COLOR))
            lineWidth(config.lineWidth)
            lineCap(LineCap.ROUND)
            lineJoin(LineJoin.ROUND)
            lineDasharray(listOf(4.0, 4.0))
            filter(
                eq {
                    get(PROP_IS_DASHED)
                    literal(true)
                },
            )
        },
    )

    style.addLayer(
        circleLayer(CIRCLES_LAYER_ID, CIRCLES_SOURCE_ID) {
            circleRadius(config.pointRadius)
            circleColor(get(PROP_COLOR))
        },
    )
}

private fun buildMapView(
    context: Context,
    accessToken: String,
    overlaysRef: MapOverlays,
    config: MapConfig,
    isGpsEnabled: Boolean,
    initialCameraMode: CameraTrackingMode,
    savedZoom: Double,
    savedLat: Double?,
    savedLng: Double?,
    onCameraPositionChanged: (Double, Double, Double) -> Unit,
    onMapReady: () -> Unit,
    onMapClick: ((GeoPoint) -> Boolean)? = null,
    onOverlayClick: (id: String) -> Unit,
    onUserInteraction: () -> Unit,
): MapView {
    MapboxOptions.accessToken = accessToken

    return MapView(context).apply {
        restoreFreeCameraIfNeeded(mapboxMap, initialCameraMode, savedZoom, savedLat, savedLng)

        mapboxMap.loadStyle(config.styleUri) { style ->
            setupCompass(config)
            setupLocation(isGpsEnabled, config)
            setupCamera(initialCameraMode, savedZoom, savedLat, savedLng)
            setupViewportObserver(onUserInteraction)
            setupCameraChangeListener(onCameraPositionChanged)
            setupOverlays(style, overlaysRef, config)
            onMapReady()
        }

        registerGestureListeners(mapboxMap, onMapClick, onOverlayClick)
    }
}

private fun MapView.setupCompass(config: MapConfig) {
    compass.updateSettings {
        clickable = false
        position = if (config.compassPosition != -1) config.compassPosition else Gravity.START
        marginLeft = config.compassMarginLeft

        if (config.compassMarginTop != -1f) marginTop = config.compassMarginTop
        if (config.compassMarginRight != -1f) marginRight = config.compassMarginRight
        if (config.compassMarginBottom != -1f) marginBottom = config.compassMarginBottom
        // enabled = false
    }
}

private fun MapView.setupLocation(
    isGpsEnabled: Boolean,
    config: MapConfig,
) {
    location.updateSettings {
        enabled = isGpsEnabled
        pulsingEnabled = config.locationPulseAnimation
        showAccuracyRing = config.showAccuracyRing
        locationPuck = createDefault2DPuck(withBearing = true)
        puckBearing = PuckBearing.HEADING
        puckBearingEnabled = true
    }
}

private fun MapView.setupCamera(
    initialCameraMode: CameraTrackingMode,
    savedZoom: Double,
    savedLat: Double?,
    savedLng: Double?,
) {
    if (initialCameraMode != CameraTrackingMode.FREE) {
        restoreTrackingMode(initialCameraMode, savedZoom)
    } else if (savedLat != null && savedLng != null) {
        mapboxMap.setCamera(
            CameraOptions
                .Builder()
                .center(Point.fromLngLat(savedLng, savedLat))
                .zoom(savedZoom)
                .pitch(0.0)
                .build(),
        )
    }
}

private fun MapView.setupViewportObserver(onUserInteraction: () -> Unit) {
    viewport.addStatusObserver { _, to, reason ->
        if (to == ViewportStatus.Idle &&
            reason == ViewportStatusChangeReason.USER_INTERACTION
        ) {
            onUserInteraction()
        }
    }
}

private fun MapView.setupCameraChangeListener(onCameraPositionChanged: (Double, Double, Double) -> Unit) {
    mapboxMap.subscribeCameraChanged {
        val state = mapboxMap.cameraState
        onCameraPositionChanged(state.zoom, state.center.latitude(), state.center.longitude())
    }
}

private fun setupOverlays(
    style: Style,
    overlaysRef: MapOverlays,
    config: MapConfig,
) {
    initOverlayLayers(style, config)
    updateCirclesSource(style, overlaysRef.circles)
    updatePolylinesSource(style, overlaysRef.polylines)
}

private fun restoreFreeCameraIfNeeded(
    mapboxMap: MapboxMap,
    initialCameraMode: CameraTrackingMode,
    savedZoom: Double,
    savedLat: Double?,
    savedLng: Double?,
) {
    if (initialCameraMode == CameraTrackingMode.FREE && savedLat != null && savedLng != null) {
        mapboxMap.setCamera(
            CameraOptions
                .Builder()
                .center(Point.fromLngLat(savedLng, savedLat))
                .zoom(savedZoom)
                .pitch(0.0)
                .build(),
        )
    }
}

private fun MapView.restoreTrackingMode(
    mode: CameraTrackingMode,
    zoom: Double,
) {
    val bearingConfig =
        if (mode == CameraTrackingMode.TRACKING_COMPASS) {
            FollowPuckViewportStateBearing.SyncWithLocationPuck
        } else {
            FollowPuckViewportStateBearing.Constant(0.0)
        }

    viewport.transitionTo(
        targetState =
            viewport.makeFollowPuckViewportState(
                FollowPuckViewportStateOptions
                    .Builder()
                    .zoom(zoom)
                    .pitch(0.0)
                    .bearing(bearingConfig)
                    .build(),
            ),
        transition = viewport.makeImmediateViewportTransition(),
    )
}

private fun registerGestureListeners(
    mapboxMap: MapboxMap,
    onMapClick: ((GeoPoint) -> Boolean)?,
    onOverlayClick: (id: String) -> Unit,
) {
    mapboxMap.addOnMapClickListener { point ->
        mapboxMap.queryRenderedFeatures(
            RenderedQueryGeometry(mapboxMap.pixelForCoordinate(point)),
            RenderedQueryOptions(
                listOf(CIRCLES_LAYER_ID, POLYLINES_SOLID_LAYER_ID, POLYLINES_DASHED_LAYER_ID),
                null,
            ),
        ) { expected ->
            val features = expected.value ?: return@queryRenderedFeatures
            val hit =
                features.firstOrNull { qf ->
                    qf.queriedFeature.feature
                        .getProperty(PROP_IS_CLICKABLE)
                        ?.asBoolean == true
                }
            if (hit != null) {
                val overlayId =
                    hit.queriedFeature.feature
                        .getProperty(PROP_OVERLAY_ID)
                        ?.asString
                if (overlayId != null) {
                    onOverlayClick(overlayId)
                    return@queryRenderedFeatures
                }
            }

            // No overlay clicked
            onMapClick?.invoke(
                GeoPoint(
                    latitude = point.latitude(),
                    longitude = point.longitude(),
                    altitude = point.altitude(),
                ),
            )
        }
        true
    }
}
