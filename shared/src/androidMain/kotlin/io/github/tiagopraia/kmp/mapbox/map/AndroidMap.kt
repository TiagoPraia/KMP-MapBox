package io.github.tiagopraia.kmp.mapbox.map

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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
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
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.eq
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.get
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.interpolate
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.generated.rasterDemSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.extension.style.terrain.generated.terrain
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
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import io.github.tiagopraia.kmp.mapbox.AnchoredOverlay
import io.github.tiagopraia.kmp.mapbox.CIRCLES_LAYER_ID
import io.github.tiagopraia.kmp.mapbox.CIRCLES_SOURCE_ID
import io.github.tiagopraia.kmp.mapbox.CameraTrackingMode
import io.github.tiagopraia.kmp.mapbox.GeographicPoint
import io.github.tiagopraia.kmp.mapbox.NORTH
import io.github.tiagopraia.kmp.mapbox.POLYLINES_DASHED_LAYER_ID
import io.github.tiagopraia.kmp.mapbox.POLYLINES_SOLID_LAYER_ID
import io.github.tiagopraia.kmp.mapbox.POLYLINES_SOURCE_ID
import io.github.tiagopraia.kmp.mapbox.PROP_COLOR
import io.github.tiagopraia.kmp.mapbox.PROP_IS_CLICKABLE
import io.github.tiagopraia.kmp.mapbox.PROP_IS_DASHED
import io.github.tiagopraia.kmp.mapbox.PROP_OVERLAY_ID
import io.github.tiagopraia.kmp.mapbox.PROP_RADIUS
import io.github.tiagopraia.kmp.mapbox.PROP_WIDTH
import io.github.tiagopraia.kmp.mapbox.config.AndroidMapConfig
import io.github.tiagopraia.kmp.mapbox.configs.CircleOverlay
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapOverlays
import io.github.tiagopraia.kmp.mapbox.configs.PolylineOverlay

@Composable
fun AndroidMap(
    accessToken: String,
    config: AndroidMapConfig,
    overlays: MapOverlays,
    anchoredOverlays: List<AnchoredOverlay> = emptyList(),
    onOverlayClick: (id: String, point: GeographicPoint) -> Unit,
    onMapReady: () -> Unit,
    onMapClick: ((GeographicPoint) -> Boolean)? = null,
    isGpsEnabled: Boolean,
    onLocationUpdate: ((GeographicPoint) -> Unit)?,
    modifier: Modifier,
) {
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    var cameraMode by rememberSaveable { mutableStateOf(CameraTrackingMode.TRACKING_NORTH) }
    var savedZoom by rememberSaveable { mutableDoubleStateOf(config.mapConfig.initialZoom) }
    var savedLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedLng by rememberSaveable { mutableStateOf<Double?>(null) }
    val overlaysRef = remember { mutableStateOf(overlays) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current
    val activeAnnotations = remember { mutableMapOf<String, ComposeView>() }

    overlaysRef.value = overlays

    OverlayDrawingEffect(
        mapViewRef = mapViewRef.value,
        overlays = overlaysRef.value,
        config = config.mapConfig,
    )

    LaunchedEffect(anchoredOverlays, mapViewRef.value) {
        val mapView = mapViewRef.value ?: return@LaunchedEffect
        syncAnchoredOverlays(
            mapView = mapView,
            overlays = anchoredOverlays,
            activeAnnotations = activeAnnotations,
            lifecycleOwner = lifecycleOwner,
            savedStateRegistryOwner = savedStateRegistryOwner,
            currentZoom = savedZoom,
            minZoom = config.mapConfig.overlayMinZoom,
            maxZoom = config.mapConfig.overlayMaxZoom,
        )
    }

    LaunchedEffect(savedZoom, mapViewRef.value) {
        val mapView = mapViewRef.value ?: return@LaunchedEffect
        updateAnnotationsVisibility(
            manager = mapView.viewAnnotationManager,
            activeAnnotations = activeAnnotations,
            currentZoom = savedZoom,
            minZoom = config.mapConfig.overlayMinZoom,
            maxZoom = config.mapConfig.overlayMaxZoom,
        )
    }

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
                    onLocationUpdate = onLocationUpdate,
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
                config = config,
            )
        }
    }
}

private fun syncAnchoredOverlays(
    mapView: MapView,
    overlays: List<AnchoredOverlay>,
    activeAnnotations: MutableMap<String, ComposeView>,
    lifecycleOwner: LifecycleOwner,
    savedStateRegistryOwner: SavedStateRegistryOwner,
    currentZoom: Double,
    minZoom: Double,
    maxZoom: Double,
) {
    val manager = mapView.viewAnnotationManager
    val currentIds = overlays.map { it.id }.toSet()
    val parentCompositionContext = mapView.findViewTreeCompositionContext()
    val isVisible = currentZoom in minZoom..maxZoom
    val density = mapView.context.resources.displayMetrics.density

    activeAnnotations.keys.filterNot { it in currentIds }.toList().forEach { id ->
        activeAnnotations[id]?.let { manager.removeViewAnnotation(it) }
        activeAnnotations.remove(id)
    }

    overlays.forEach { overlay ->
        val existingView = activeAnnotations[overlay.id]
        val widthPx = overlay.widthDp.times(density)
        val heightPx = overlay.heightDp.times(density)

        if (existingView == null) {
            val composeView =
                ComposeView(mapView.context).apply {
                    layoutParams =
                        android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                    setViewTreeLifecycleOwner(lifecycleOwner)
                    setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
                    if (parentCompositionContext != null) {
                        setParentCompositionContext(parentCompositionContext)
                    }
                    setContent { overlay.content() }
                }

            manager.addViewAnnotation(
                composeView,
                viewAnnotationOptions {
                    geometry(Point.fromLngLat(overlay.point.longitude, overlay.point.latitude))
                    width(widthPx)
                    height(heightPx)
                    annotationAnchor { anchor(ViewAnnotationAnchor.TOP) }
                    allowOverlap(true)
                    allowOverlapWithPuck(true)
                    visible(isVisible)
                },
            )
            activeAnnotations[overlay.id] = composeView
        } else {
            existingView.setContent { overlay.content() }
            manager.updateViewAnnotation(
                existingView,
                viewAnnotationOptions {
                    geometry(Point.fromLngLat(overlay.point.longitude, overlay.point.latitude))
                    width(widthPx)
                    height(heightPx)
                    allowOverlapWithPuck(true)
                    visible(isVisible)
                },
            )
        }
    }
}

private fun updateAnnotationsVisibility(
    manager: ViewAnnotationManager,
    activeAnnotations: MutableMap<String, ComposeView>,
    currentZoom: Double,
    minZoom: Double,
    maxZoom: Double,
) {
    val shouldBeVisible = currentZoom in minZoom..maxZoom
    activeAnnotations.values.forEach { view ->
        try {
            manager.updateViewAnnotation(
                view,
                viewAnnotationOptions {
                    visible(shouldBeVisible)
                    allowOverlapWithPuck(true)
                },
            )
        } catch (e: Exception) {
            Logger.e(tag = "AndroidMap") { "Failed to update visibility: ${e.message}" }
        }
    }
}

@Composable
fun BoxScope.FollowButton(
    mapViewRef: MapView?,
    cameraMode: CameraTrackingMode,
    onModeChanged: (CameraTrackingMode) -> Unit,
    config: AndroidMapConfig,
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
                        .maxDurationMs(config.mapConfig.animationDuration)
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
        shape = config.followButton.buttonShape,
        containerColor = config.followButton.followButtonColor,
        modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .then(config.followButton.buttonModifier),
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
    config: MapConfig,
) {
    LaunchedEffect(overlays, mapViewRef) {
        val style = mapViewRef?.mapboxMap?.style ?: return@LaunchedEffect
        updateCirclesSource(style, overlays.circles, config.pointRadius)
        updatePolylinesSource(style, overlays.polylines, config.lineWidth)
    }
}

private fun buildZoomInterpolatedSize(
    propertyName: String,
    referenceZoom: Double,
    minZoom: Double,
    maxZoom: Double,
    minFactor: Double,
    maxFactor: Double,
): Expression =
    interpolate {
        linear()
        zoom()
        literal(minZoom)
        product {
            literal(minFactor)
            get(propertyName)
        }
        literal(referenceZoom)
        get(propertyName)
        literal(maxZoom)
        product {
            literal(maxFactor)
            get(propertyName)
        }
    }

private fun updateCirclesSource(
    style: Style,
    circles: List<CircleOverlay>,
    defaultRadius: Double,
) {
    println("updateCirclesSource called with ${circles.size} circles: ${circles.map { it.id }}")
    val features =
        circles.map { circle ->
            Feature.fromGeometry(
                Point.fromLngLat(circle.center.longitude, circle.center.latitude),
                JsonObject().apply {
                    addProperty(PROP_OVERLAY_ID, circle.id)
                    addProperty(PROP_IS_CLICKABLE, circle.isClickable)
                    addProperty(PROP_COLOR, circle.colorHex)
                    addProperty(PROP_RADIUS, circle.radius ?: defaultRadius)
                },
            )
        }
    style.getSourceAs<GeoJsonSource>(CIRCLES_SOURCE_ID)?.featureCollection(FeatureCollection.fromFeatures(features))
}

private fun updatePolylinesSource(
    style: Style,
    polylines: List<PolylineOverlay>,
    defaultWidth: Double,
) {
    println("updatePolyLineSource called with ${polylines.size} circles: ${polylines.map { it.id }}")
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
                    addProperty(PROP_WIDTH, polyline.width ?: defaultWidth)
                },
            )
        }
    style.getSourceAs<GeoJsonSource>(POLYLINES_SOURCE_ID)?.featureCollection(FeatureCollection.fromFeatures(features))
}

private fun initOverlayLayers(
    style: Style,
    config: AndroidMapConfig,
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

    val lineWidthExpr =
        buildZoomInterpolatedSize(
            propertyName = PROP_WIDTH,
            referenceZoom = config.mapConfig.initialZoom,
            minZoom = config.mapConfig.overlayMinZoom,
            maxZoom = config.mapConfig.overlayMaxZoom,
            minFactor = config.mapConfig.overlayMinFactor,
            maxFactor = config.mapConfig.overlayMaxFactor,
        )

    val circleRadiusExpr =
        buildZoomInterpolatedSize(
            propertyName = PROP_RADIUS,
            referenceZoom = config.mapConfig.initialZoom,
            minZoom = config.mapConfig.overlayMinZoom,
            maxZoom = config.mapConfig.overlayMaxZoom,
            minFactor = config.mapConfig.overlayMinFactor,
            maxFactor = config.mapConfig.overlayMaxFactor,
        )

    style.addLayer(
        lineLayer(POLYLINES_SOLID_LAYER_ID, POLYLINES_SOURCE_ID) {
            lineColor(get(PROP_COLOR))
            lineWidth(lineWidthExpr)
            lineCap(LineCap.ROUND)
            lineJoin(LineJoin.ROUND)
            minZoom(config.mapConfig.overlayMinZoom)
            maxZoom(config.mapConfig.overlayMaxZoom)
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
            lineWidth(lineWidthExpr)
            lineCap(LineCap.ROUND)
            lineJoin(LineJoin.ROUND)
            minZoom(config.mapConfig.overlayMinZoom)
            maxZoom(config.mapConfig.overlayMaxZoom)
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
            circleRadius(circleRadiusExpr)
            minZoom(config.mapConfig.overlayMinZoom)
            maxZoom(config.mapConfig.overlayMaxZoom)
            circleColor(get(PROP_COLOR))
        },
    )
}

private fun buildMapView(
    context: Context,
    accessToken: String,
    overlaysRef: MapOverlays,
    config: AndroidMapConfig,
    isGpsEnabled: Boolean,
    initialCameraMode: CameraTrackingMode,
    savedZoom: Double,
    savedLat: Double?,
    savedLng: Double?,
    onLocationUpdate: ((GeographicPoint) -> Unit)?,
    onCameraPositionChanged: (Double, Double, Double) -> Unit,
    onMapReady: () -> Unit,
    onMapClick: ((GeographicPoint) -> Boolean)? = null,
    onOverlayClick: (id: String, point: GeographicPoint) -> Unit,
    onUserInteraction: () -> Unit,
): MapView {
    MapboxOptions.accessToken = accessToken

    return MapView(context).apply {
        restoreFreeCameraIfNeeded(mapboxMap, initialCameraMode, savedZoom, savedLat, savedLng)

        mapboxMap.loadStyle(
            style(config.mapConfig.styleUri) {
                +rasterDemSource("dem") {
                    url("mapbox://mapbox.mapbox-terrain-dem-v1")
                }
                +terrain("dem")
            },
        ) { style ->
            setupCompass(config)
            setupLocation(isGpsEnabled, onLocationUpdate, config)
            setupCamera(initialCameraMode, savedZoom, savedLat, savedLng)
            setupViewportObserver(onUserInteraction)
            setupCameraChangeListener(onCameraPositionChanged)
            setupOverlays(style, overlaysRef, config)
            onMapReady()
        }

        registerGestureListeners(mapboxMap, onMapClick, onOverlayClick)
    }
}

private fun MapView.setupCompass(config: AndroidMapConfig) {
    compass.updateSettings {
        position = if (config.compassPosition != -1) config.compassPosition else Gravity.START
        marginLeft = config.compassMarginLeft

        if (config.compassMarginTop != -1f) marginTop = config.compassMarginTop
        if (config.compassMarginRight != -1f) marginRight = config.compassMarginRight
        if (config.compassMarginBottom != -1f) marginBottom = config.compassMarginBottom
    }
}

private fun MapView.setupLocation(
    isGpsEnabled: Boolean,
    onLocationUpdate: ((GeographicPoint) -> Unit)?,
    config: AndroidMapConfig,
) {
    location.updateSettings {
        enabled = isGpsEnabled
        pulsingEnabled = config.locationPulseAnimation
        showAccuracyRing = config.showAccuracyRing
        locationPuck = createDefault2DPuck(withBearing = true)
        puckBearing = PuckBearing.HEADING
        puckBearingEnabled = true
    }

    onLocationUpdate?.let { callback ->
        location.addOnIndicatorPositionChangedListener { point ->
            val altitude = mapboxMap.getElevation(point) ?: 0.0
            callback(
                GeographicPoint(
                    latitude = point.latitude(),
                    longitude = point.longitude(),
                    altitude = altitude,
                ),
            )
        }
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
    config: AndroidMapConfig,
) {
    initOverlayLayers(style, config)
    updateCirclesSource(style, overlaysRef.circles, config.mapConfig.pointRadius)
    updatePolylinesSource(style, overlaysRef.polylines, config.mapConfig.lineWidth)
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
    onMapClick: ((GeographicPoint) -> Boolean)?,
    onOverlayClick: (id: String, point: GeographicPoint) -> Unit,
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
                    val geoPoint =
                        GeographicPoint(
                            latitude = point.latitude(),
                            longitude = point.longitude(),
                            altitude = mapboxMap.getElevation(point) ?: 0.0,
                        )
                    onOverlayClick(
                        overlayId,
                        geoPoint,
                    )
                    return@queryRenderedFeatures
                }
            }

            // No overlay clicked
            onMapClick?.invoke(
                GeographicPoint(
                    latitude = point.latitude(),
                    longitude = point.longitude(),
                    altitude = mapboxMap.getElevation(point) ?: 0.0,
                ),
            )
        }
        true
    }
}
