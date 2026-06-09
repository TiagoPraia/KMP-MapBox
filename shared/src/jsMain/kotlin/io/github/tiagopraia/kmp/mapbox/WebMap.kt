package io.github.tiagopraia.kmp.mapbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import co.touchlab.kermit.Logger
import io.github.tiagopraia.kmp.mapbox.configs.CircleOverlay
import io.github.tiagopraia.kmp.mapbox.configs.MapOverlays
import io.github.tiagopraia.kmp.mapbox.configs.PolylineOverlay
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLLinkElement
import org.w3c.dom.HTMLStyleElement

const val BUTTON_SIZE = "48px"
const val BUTTON_RADIUS = "8px"
const val BUTTON_SHADOW = "0 2px 6px rgba(0,0,0,0.3)"
const val BUTTON_MARGIN = "16px"
const val BUTTON_ICON_SIZE = "24px"

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
    }

    open class AttributionControl(
        options: dynamic = definedExternally,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WebMap(
    accessToken: String,
    overlays: MapOverlays,
    config: WebMapConfig,
    onMapClick: ((GeoPoint) -> Boolean)?,
    onOverlayClick: (id: String) -> Unit,
    extraHTML: List<HTMLElement>,
    modifier: Modifier,
) {
    val mapId = "mapbox"
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

            val extraDiv = document.createElement("div") as HTMLDivElement
            extraDiv.id = "$mapId-extra"
            extraDiv.style.position = "absolute"
            extraDiv.style.asDynamic().inset = "0"
            extraDiv.style.asDynamic().pointerEvents = "none"
            container.appendChild(extraDiv)

            container
        },
        onRelease = {
            mapRef.value?.remove()
            mapRef.value = null
            isStyleReady.value = false
        },
        update = { container ->
            val extraDiv = container.querySelector("#$mapId-extra") as? HTMLDivElement
            if (extraDiv != null) {
                extraDiv.innerHTML = ""
                extraHTML.forEach { element ->
                    element.style.asDynamic().pointerEvents = "auto"
                    extraDiv.appendChild(element)
                }
            }

            if (mapRef.value != null) return@HtmlElementView

            MapBox.accessToken = accessToken

            val options = js("{}")
            options.container = mapId
            options.style = config.mapConfig.styleUri
            options.zoom = config.mapConfig.initialZoom
            options.attributionControl = false

            val map = MapBox.Map(options)
            val attrOptions = js("{}")
            attrOptions.compact = true
            map.addControl(MapBox.AttributionControl(attrOptions), WebFollowButtonPosition.TOP_LEFT.mapboxPosition)

            map.on("load") {
                injectMapboxGeolocateStyle(config.followButton)

                val geolocateOptions = js("{}")
                geolocateOptions.positionOptions = js("{}")
                geolocateOptions.positionOptions.enableHighAccuracy = config.enableHighAccuracy
                geolocateOptions.trackUserLocation = config.trackUserLocation
                geolocateOptions.showUserHeading = config.showUserHeading
                geolocateOptions.showAccuracyCircle = config.showAccuracyRing
                geolocateOptions.fitBoundsOptions = js("{}")
                geolocateOptions.fitBoundsOptions.maxZoom = config.mapConfig.initialZoom
                geolocateOptions.fitBoundsOptions.duration = config.mapConfig.animationDuration

                val followPosition =
                    config.followButton?.position?.mapboxPosition
                        ?: WebFollowButtonPosition.BOTTOM_RIGHT.mapboxPosition
                val geolocate = MapBox.GeolocateControl(geolocateOptions)
                map.addControl(geolocate, followPosition)
                window.requestAnimationFrame { geolocate.trigger() }

                map.resize()

                initWebOverlayLayers(map, config)
                updateWebCirclesSource(map, overlaysRef.value.circles)
                updateWebPolylinesSource(map, overlaysRef.value.polylines)
                isStyleReady.value = true
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

private fun injectMapboxCss() {
    if (document.getElementById("mapbox-gl-css") != null) return
    val link = document.createElement("link") as HTMLLinkElement
    link.id = "mapbox-gl-css"
    link.rel = "stylesheet"
    link.href = "https://api.mapbox.com/mapbox-gl-js/v3.9.4/mapbox-gl.css"
    document.head?.appendChild(link)
}

private fun injectMapboxGeolocateStyle(followButton: WebFollowButtonConfig?) {
    if (document.getElementById("mapbox-geolocate-style") != null) return
    val style = document.createElement("style") as HTMLStyleElement
    style.id = "mapbox-geolocate-style"

    val a =
        buildString {
            append(".mapboxgl-ctrl-geolocate {")
            followButton?.size?.let { append("width: $it !important; height: $it !important;") }
            followButton?.borderRadius?.let { append("border-radius: $it !important;") }
            followButton?.backgroundColor?.let { append("background-color: $it !important;") }
            followButton?.shadow?.let { append("box-shadow: $it !important;") }
            if (followButton?.size !=
                null
            ) {
                append("display: flex !important; align-items: center !important; justify-content: center !important;")
            }
            append("}")

            followButton?.iconSize?.let {
                append(".mapboxgl-ctrl-geolocate .mapboxgl-ctrl-icon {")
                append("width: $it !important; height: $it !important;")
                append("}")
            }

            followButton?.margin?.let {
                val positionClass =
                    when (followButton.position) {
                        WebFollowButtonPosition.TOP_LEFT -> ".mapboxgl-ctrl-top-left"
                        WebFollowButtonPosition.TOP_RIGHT -> ".mapboxgl-ctrl-top-right"
                        WebFollowButtonPosition.BOTTOM_LEFT -> ".mapboxgl-ctrl-bottom-left"
                        WebFollowButtonPosition.BOTTOM_RIGHT -> ".mapboxgl-ctrl-bottom-right"
                    }

                append("$positionClass .mapboxgl-ctrl {")
                append("margin: $it !important;")
                followButton.borderRadius?.let { radius -> append("border-radius: $radius !important;") }
                append("box-shadow: nono !important;")
                append("}")
            }
        }
    style.textContent = a
    Logger.i(a)
    document.head?.appendChild(style)
}

private fun initWebOverlayLayers(
    map: MapBox.Map,
    config: WebMapConfig,
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
    solidLayer.paint = solidPaint

    val solidLayout = js("{}")
    solidLayout["line-cap"] = "round"
    solidLayout["line-join"] = "round"
    solidLayer.layout = solidLayout
    solidLayer.filter = arrayOf("==", arrayOf("get", PROP_IS_DASHED), false)
    map.addLayer(solidLayer)

    val dashedLayer = js("{}")
    dashedLayer.id = POLYLINES_DASHED_LAYER_ID
    dashedLayer.type = "line"
    dashedLayer.source = POLYLINES_SOURCE_ID

    val dashedPaint = js("{}")
    dashedPaint["line-color"] = arrayOf("get", PROP_COLOR)
    dashedPaint["line-width"] = arrayOf("get", PROP_WIDTH)
    dashedPaint["line-dasharray"] = arrayOf(4, 4)
    dashedLayer.paint = dashedPaint

    val dashedLayout = js("{}")
    dashedLayout["line-cap"] = "round"
    dashedLayout["line-join"] = "round"
    dashedLayer.layout = dashedLayout
    dashedLayer.filter = arrayOf("==", arrayOf("get", PROP_IS_DASHED), true)
    map.addLayer(dashedLayer)

    val circleLayer = js("{}")
    circleLayer.id = CIRCLES_LAYER_ID
    circleLayer.type = "circle"
    circleLayer.source = CIRCLES_SOURCE_ID
    val circlePaint = js("{}")
    circlePaint["circle-color"] = arrayOf("get", PROP_COLOR)
    circlePaint["circle-radius"] = config.mapConfig.pointRadius
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
