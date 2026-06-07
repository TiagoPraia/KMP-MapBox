package io.github.tiagopraia.kmp.mapbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import io.github.tiagopraia.kmp.mapbox.configs.CircleOverlay
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapOverlays
import io.github.tiagopraia.kmp.mapbox.configs.PolylineOverlay
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLLinkElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.HTMLStyleElement

private const val BUTTON_SIZE = "48px"
private const val BUTTON_RADIUS = "8px"
private const val BUTTON_SHADOW = "0 2px 6px rgba(0,0,0,0.3)"
private const val BUTTON_MARGIN = "16px"
private const val BUTTON_GAP = "8px"
private const val BUTTON_ICON_SIZE = "24px"

private const val COLOR_WHITE = "#FFFFFF"
private const val COLOR_GRAY = "#BDBDBD"
private const val COLOR_RED = "#F44336"
private const val COLOR_BLUE = "#29B6F6"
private const val COLOR_ICON_DARK = "#333333"
private const val COLOR_ICON_LIGHT = "#FFFFFF"

@JsModule("mapbox-gl")
@JsNonModule
external object MapBox {
    var accessToken: String

    open class Map(
        options: dynamic,
    ) {
        fun remove()

        fun on(
            event: String,
            callback: () -> Unit,
        )

        @JsName("on")
        fun onEvent(
            event: String,
            callback: (dynamic) -> Unit,
        )

        fun addControl(
            control: dynamic,
            position: String = definedExternally,
        )

        fun resize()

        fun addSource(
            id: String,
            source: dynamic,
        )

        fun addLayer(layer: dynamic)

        fun getSource(id: String): dynamic

        fun queryRenderedFeatures(
            point: dynamic,
            options: dynamic,
        ): dynamic
    }

    open class GeolocateControl(
        options: dynamic,
    ) {
        fun trigger()

        fun on(
            event: String,
            callback: (dynamic) -> Unit,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WebMap(
    accessToken: String,
    overlays: MapOverlays,
    config: MapConfig,
    onMapReady: () -> Unit,
    onMapClick: ((GeoPoint) -> Boolean)?,
    onOverlayClick: (id: String) -> Unit,
    buttonState: MapButtonState?,
    modifier: Modifier,
) {
    val mapId = remember { "mapbox-${(0..99999).random()}" }
    val mapRef = remember { mutableStateOf<MapBox.Map?>(null) }
    val isStyleReady = remember { mutableStateOf(false) }
    val overlaysRef = remember { mutableStateOf(overlays) }
    overlaysRef.value = overlays

    LaunchedEffect(overlays, isStyleReady.value) {
        if (!isStyleReady.value) return@LaunchedEffect
        val map = mapRef.value ?: return@LaunchedEffect
        updateWebCirclesSource(map, overlays.circles)
        updateWebPolylinesSource(map, overlays.polylines)
    }

    HtmlElementView(
        modifier = modifier,
        factory = {
            injectMapboxCss()

            val container = document.createElement("div") as HTMLDivElement
            container.style.position = "relative"
            container.style.width = "100%"
            container.style.height = "100%"
            container.style.asDynamic().overflow = "hidden"

            val mapDiv = document.createElement("div") as HTMLDivElement
            mapDiv.id = mapId
            mapDiv.style.width = "100%"
            mapDiv.style.height = "100%"
            container.appendChild(mapDiv)

            val buttonsDiv = document.createElement("div") as HTMLDivElement
            buttonsDiv.id = "$mapId-buttons"
            buttonsDiv.style.position = "absolute"
            buttonsDiv.style.asDynamic().inset = "0"
            buttonsDiv.style.asDynamic().pointerEvents = "none"
            container.appendChild(buttonsDiv)

            container
        },
        onRelease = {
            mapRef.value?.remove()
            mapRef.value = null
            isStyleReady.value = false
        },
        update = { container ->
            val buttonsDiv = container.querySelector("#$mapId-buttons") as? HTMLDivElement
            if (buttonsDiv != null && buttonState != null) {
                buttonsDiv.innerHTML = ""
                buildOverlayButtons(
                    container = buttonsDiv,
                    buttonState = buttonState,
                )
            }

            if (mapRef.value != null) return@HtmlElementView

            MapBox.accessToken = accessToken

            val options = js("{}")
            options.container = mapId
            options.style = config.styleUri
            options.zoom = config.initialZoom

            val map = MapBox.Map(options)

            map.on("load") {
                injectMapboxGeolocateStyle()

                val geolocateOptions = js("{}")
                geolocateOptions.positionOptions = js("{}")
                geolocateOptions.positionOptions.enableHighAccuracy = true
                geolocateOptions.trackUserLocation = true
                geolocateOptions.showUserHeading = true
                geolocateOptions.showAccuracyCircle = config.showAccuracyRing
                geolocateOptions.fitBoundsOptions = js("{}")
                geolocateOptions.fitBoundsOptions.maxZoom = config.initialZoom

                val geolocate = MapBox.GeolocateControl(geolocateOptions)
                map.addControl(geolocate, "bottom-right")
                window.requestAnimationFrame {
                    geolocate.trigger()
                }
                geolocate.on("add") { geolocate.trigger() }

                map.resize()

                initWebOverlayLayers(map, config)
                updateWebCirclesSource(map, overlaysRef.value.circles)
                updateWebPolylinesSource(map, overlaysRef.value.polylines)
                isStyleReady.value = true
                onMapReady()
            }

            map.onEvent("click") { e ->
                val lng = e.lngLat.lng.unsafeCast<Double>()
                val lat = e.lngLat.lat.unsafeCast<Double>()

                val queryOptions = js("{}")
                queryOptions.layers =
                    arrayOf(
                        POLYLINES_DASHED_LAYER_ID,
                        POLYLINES_SOLID_LAYER_ID,
                        CIRCLES_LAYER_ID,
                    )
                val features = map.queryRenderedFeatures(e.point, queryOptions)
                val length = features.length.unsafeCast<Int>()

                if (length > 0) {
                    val feature = features[0]
                    val isClickable = feature.properties[PROP_IS_CLICKABLE].unsafeCast<Boolean>()
                    if (isClickable) {
                        val overlayId = feature.properties[PROP_OVERLAY_ID].unsafeCast<String>()
                        onOverlayClick(overlayId)
                        return@onEvent
                    }
                }

                onMapClick?.invoke(GeoPoint(lat, lng, 0.0))
            }

            mapRef.value = map
        },
    )
}

private fun buildOverlayButtons(
    container: HTMLDivElement,
    buttonState: MapButtonState,
) {
    buttonState.onProfileClick?.let { container.appendChild(buildProfileButton(it)) }
    buttonState.onTrailsClick?.let { container.appendChild(buildTrailsButton(it)) }
    container.appendChild(buildDrawingColumn(buttonState))
}

private fun buildProfileButton(onClick: () -> Unit): HTMLButtonElement =
    buildFab(COLOR_WHITE, "person", onClick).apply {
        style.position = "absolute"
        style.top = BUTTON_MARGIN
        style.right = BUTTON_MARGIN
        style.asDynamic().pointerEvents = "auto"
    }

private fun buildTrailsButton(onClick: () -> Unit): HTMLButtonElement =
    buildFab(COLOR_WHITE, "route", onClick).apply {
        style.position = "absolute"
        style.bottom = BUTTON_MARGIN
        style.left = "calc($BUTTON_MARGIN + $BUTTON_SIZE + $BUTTON_GAP)"
        style.asDynamic().pointerEvents = "auto"
    }

private fun buildDrawingColumn(buttonState: MapButtonState): HTMLDivElement {
    val column = document.createElement("div") as HTMLDivElement
    column.style.position = "absolute"
    column.style.bottom = BUTTON_MARGIN
    column.style.left = BUTTON_MARGIN
    column.style.display = "flex"
    column.style.asDynamic().flexDirection = "column"
    column.style.asDynamic().gap = BUTTON_GAP
    column.style.asDynamic().pointerEvents = "auto"

    if (buttonState.isDrawingMode) {
        buttonState.onCancel?.let {
            column.appendChild(buildCancelButton(it, buttonState.canUndo))
        }
        buttonState.onUndo?.let {
            column.appendChild(buildUndoButton(it, buttonState.canUndo))
        }
        buttonState.onComplete?.let {
            column.appendChild(buildCompleteButton(it, buttonState.canComplete))
        }
    } else {
        buttonState.onStartRoute?.let {
            column.appendChild(buildCreateButton(it))
        }
    }

    return column
}

private fun buildCreateButton(onClick: () -> Unit): HTMLButtonElement = buildFab(COLOR_WHITE, "add", onClick)

private fun buildCancelButton(
    onClick: () -> Unit,
    canUndo: Boolean,
): HTMLButtonElement = buildFab(COLOR_RED, if (canUndo) "delete" else "close", onClick)

private fun buildUndoButton(
    onClick: () -> Unit,
    canUndo: Boolean,
): HTMLButtonElement = buildFab(if (canUndo) COLOR_WHITE else COLOR_GRAY, "undo", onClick)

private fun buildCompleteButton(
    onClick: () -> Unit,
    canComplete: Boolean,
): HTMLButtonElement = buildFab(if (canComplete) COLOR_BLUE else COLOR_GRAY, "check", onClick)

private fun buildFab(
    backgroundColor: String,
    icon: String,
    onClick: () -> Unit,
): HTMLButtonElement {
    val button = document.createElement("button") as HTMLButtonElement
    button.style.width = BUTTON_SIZE
    button.style.height = BUTTON_SIZE
    button.style.borderRadius = BUTTON_RADIUS
    button.style.backgroundColor = backgroundColor
    button.style.border = "none"
    button.style.cursor = "pointer"
    button.style.display = "flex"
    button.style.asDynamic().alignItems = "center"
    button.style.asDynamic().justifyContent = "center"
    button.style.asDynamic().boxShadow = BUTTON_SHADOW
    button.style.asDynamic().flexShrink = "0"

    val span = document.createElement("span") as HTMLSpanElement
    span.className = "material-icons"
    span.textContent = icon
    span.style.fontSize = BUTTON_ICON_SIZE
    span.style.color =
        if (backgroundColor == COLOR_WHITE || backgroundColor == COLOR_GRAY) {
            COLOR_ICON_DARK
        } else {
            COLOR_ICON_LIGHT
        }
    button.appendChild(span)

    button.addEventListener("click") { onClick() }
    return button
}

private fun injectMapboxCss() {
    if (document.getElementById("mapbox-gl-css") != null) return
    val link = document.createElement("link") as HTMLLinkElement
    link.id = "mapbox-gl-css"
    link.rel = "stylesheet"
    link.href = "https://api.mapbox.com/mapbox-gl-js/v3.9.4/mapbox-gl.css"
    document.head?.appendChild(link)
}

private fun injectMapboxGeolocateStyle() {
    if (document.getElementById("mapbox-geolocate-style") != null) return
    val style = document.createElement("style") as HTMLStyleElement
    style.id = "mapbox-geolocate-style"
    style.textContent =
        """
        .mapboxgl-ctrl-geolocate {
            width: $BUTTON_SIZE !important;
            height: $BUTTON_SIZE !important;
            border-radius: $BUTTON_RADIUS !important;
            background-color: $COLOR_WHITE !important;
            box-shadow: $BUTTON_SHADOW !important;
            display: flex !important;
            align-items: center !important;
            justify-content: center !important;
        }
        .mapboxgl-ctrl-geolocate .mapboxgl-ctrl-icon {
            width: $BUTTON_ICON_SIZE !important;
            height: $BUTTON_ICON_SIZE !important;
        }
        .mapboxgl-ctrl-bottom-right {
            bottom: $BUTTON_MARGIN !important;
            right: $BUTTON_MARGIN !important;
        }
        .mapboxgl-ctrl-bottom-right .mapboxgl-ctrl {
            margin: 0 !important;
            border-radius: $BUTTON_RADIUS !important;
            box-shadow: none !important;
        }
        """.trimIndent()
    document.head?.appendChild(style)
}

private fun initWebOverlayLayers(
    map: MapBox.Map,
    config: MapConfig,
) {
    val emptyCollection = js("{}")
    emptyCollection.type = "FeatureCollection"
    emptyCollection.features = emptyArray<dynamic>()

    val circlesSource = js("{}")
    circlesSource.type = "geojson"
    circlesSource.data = emptyCollection
    map.addSource(CIRCLES_SOURCE_ID, circlesSource)

    val polylinesSource = js("{}")
    polylinesSource.type = "geojson"
    polylinesSource.data = emptyCollection
    map.addSource(POLYLINES_SOURCE_ID, polylinesSource)

    val solidLayer = js("{}")
    solidLayer.id = POLYLINES_SOLID_LAYER_ID
    solidLayer.type = "line"
    solidLayer.source = POLYLINES_SOURCE_ID
    val solidPaint = js("{}")
    solidPaint["line-color"] = arrayOf("get", PROP_COLOR)
    solidPaint["line-width"] = arrayOf("get", PROP_WIDTH)
    solidPaint["line-cap"] = "round"
    solidPaint["line-join"] = "round"
    solidLayer.paint = solidPaint
    solidLayer.filter = arrayOf("==", arrayOf("get", PROP_IS_DASHED), false)
    map.addLayer(solidLayer)

    val dashedLayer = js("{}")
    dashedLayer.id = POLYLINES_DASHED_LAYER_ID
    dashedLayer.type = "line"
    dashedLayer.source = POLYLINES_SOURCE_ID
    val dashedPaint = js("{}")
    dashedPaint["line-color"] = arrayOf("get", PROP_COLOR)
    dashedPaint["line-width"] = arrayOf("get", PROP_WIDTH)
    dashedPaint["line-cap"] = "round"
    dashedPaint["line-join"] = "round"
    dashedPaint["line-dasharray"] = arrayOf(4, 4)
    dashedLayer.paint = dashedPaint
    dashedLayer.filter = arrayOf("==", arrayOf("get", PROP_IS_DASHED), true)
    map.addLayer(dashedLayer)

    val circleLayer = js("{}")
    circleLayer.id = CIRCLES_LAYER_ID
    circleLayer.type = "circle"
    circleLayer.source = CIRCLES_SOURCE_ID
    val circlePaint = js("{}")
    circlePaint["circle-color"] = arrayOf("get", PROP_COLOR)
    circlePaint["circle-radius"] = config.pointRadius
    circleLayer.paint = circlePaint
    map.addLayer(circleLayer)
}

private fun updateWebCirclesSource(
    map: MapBox.Map,
    circles: List<CircleOverlay>,
) {
    val features =
        circles
            .map { circle ->
                val geometry = js("{}")
                geometry.type = "Point"
                geometry.coordinates = arrayOf(circle.center.longitude, circle.center.latitude)
                val props = js("{}")
                props[PROP_OVERLAY_ID] = circle.id
                props[PROP_IS_CLICKABLE] = circle.isClickable
                props[PROP_COLOR] = circle.colorHex
                val feature = js("{}")
                feature.type = "Feature"
                feature.geometry = geometry
                feature.properties = props
                feature
            }.toTypedArray()
    val collection = js("{}")
    collection.type = "FeatureCollection"
    collection.features = features
    map.getSource(CIRCLES_SOURCE_ID).setData(collection)
}

private fun updateWebPolylinesSource(
    map: MapBox.Map,
    polylines: List<PolylineOverlay>,
) {
    val features =
        polylines
            .map { polyline ->
                val geometry = js("{}")
                geometry.type = "LineString"
                geometry.coordinates =
                    polyline.points
                        .map { arrayOf(it.longitude, it.latitude) }
                        .toTypedArray()
                val props = js("{}")
                props[PROP_OVERLAY_ID] = polyline.id
                props[PROP_IS_CLICKABLE] = polyline.isClickable
                props[PROP_COLOR] = polyline.colorHex
                props[PROP_WIDTH] = polyline.width
                props[PROP_IS_DASHED] = polyline.isDashed
                val feature = js("{}")
                feature.type = "Feature"
                feature.geometry = geometry
                feature.properties = props
                feature
            }.toTypedArray()
    val collection = js("{}")
    collection.type = "FeatureCollection"
    collection.features = features
    map.getSource(POLYLINES_SOURCE_ID).setData(collection)
}
