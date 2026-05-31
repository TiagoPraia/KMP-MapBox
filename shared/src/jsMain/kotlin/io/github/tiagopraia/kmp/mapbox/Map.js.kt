package io.github.tiagopraia.kmp.mapbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig
import kotlinx.browser.document
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div

@JsModule("mapbox-gl")
@JsNonModule
external object mapboxgl {
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

        fun addControl(control: dynamic)

        fun resize()

        fun addSource(
            id: String,
            source: dynamic,
        )

        fun addLayer(layer: dynamic)

        fun getSource(id: String): dynamic
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

@Composable
actual fun Map(
    accessToken: String,
    vm: MapViewModel,
    config: MapConfig,
    extraOverlays: List<MapOverlayAction>,
    modifier: Modifier,
) {
    val containerId = remember { "web-map-container" }
    val mapRef = remember { mutableStateOf<mapboxgl.Map?>(null) }

    LaunchedEffect(Unit) {
        injectMapboxCss()
    }

    LaunchedEffect(mapRef.value) {
        val map = mapRef.value ?: return@LaunchedEffect
        map.onEvent("click") { e ->
            if (vm.route.isDrawingMode) {
                val lng = e.lngLat.lng.unsafeCast<Double>()
                val lat = e.lngLat.lat.unsafeCast<Double>()
                vm.route.addPoint(GeoPoint(lat, lng, 0.0))
            }
        }
    }

    LaunchedEffect(vm.route.points.size, mapRef.value) {
        val map = mapRef.value ?: return@LaunchedEffect

        val pointsSourceId = "route-points-${vm.route.routeId}"
        val linesSourceId = "route-lines-${vm.route.routeId}"

        if (!sourceExists(map, pointsSourceId)) {
            map.addSource(pointsSourceId, geoJsonSource(emptyFeatureCollection()))
            map.addSource(linesSourceId, geoJsonSource(emptyFeatureCollection()))
            map.addLayer(circleLayer("route-points-layer-${vm.route.routeId}", pointsSourceId))
            map.addLayer(lineLayer("route-lines-layer-${vm.route.routeId}", linesSourceId))
        }

        if (vm.route.completedRoute == null || vm.route.points.isNotEmpty()) {
            map.getSource(pointsSourceId).setData(buildRouteFeatureCollection(vm.route.points))
            map.getSource(linesSourceId).setData(buildLineFeatureCollection(vm.route.points))
        }
    }

    DisposableEffect(containerId) {
        val map =
            createMapboxMap(
                containerId = containerId,
                accessToken = accessToken,
                zoom = config.initialZoom,
                styleUri = config.styleUri,
            )

        map.on("load") {
            map.resize()
            mapRef.value = map

            val geolocateOptions = js("{}")
            geolocateOptions["positionOptions"] = js("{}")
            geolocateOptions["positionOptions"]["enableHighAccuracy"] = true
            geolocateOptions["trackUserLocation"] = true
            geolocateOptions["showUserHeading"] = true
            geolocateOptions["showAccuracyCircle"] = true
            geolocateOptions["fitBoundsOptions"] = js("{}")
            geolocateOptions["fitBoundsOptions"]["maxZoom"] = config.initialZoom

            val geolocate = mapboxgl.GeolocateControl(geolocateOptions)
            map.addControl(geolocate)

            geolocate.on("add") {
                geolocate.trigger()
            }
        }

        onDispose {
            map.remove()
            mapRef.value = null
        }
    }

    Div(attrs = {
        style {
            property("position", "relative")
            property("width", "100%")
            property("height", "100vh")
        }
    }) {
        Div(attrs = {
            id(containerId)
            style {
                property("width", "100%")
                property("height", "100%")
                property("position", "absolute")
                property("top", "0")
                property("left", "0")
                property("z-index", "0")
            }
        })
        Div(attrs = {
            style {
                property("position", "absolute")
                property("top", "0")
                property("left", "0")
                property("width", "100%")
                property("height", "100%")
                property("z-index", "1")
                property("pointer-events", "none")
            }
        }) {
            WebOverlayButtons(vm = vm, config = config)
            extraOverlays.forEach { it.content() }
        }
    }
}

private fun injectMapboxCss() {
    val linkId = "mapbox-gl-css"
    if (document.getElementById(linkId) != null) return

    val link = document.createElement("link")
    link.setAttribute("id", linkId)
    link.setAttribute("rel", "stylesheet")
    link.setAttribute("href", "https://api.mapbox.com/mapbox-gl-js/v3.9.4/mapbox-gl.css")
    document.head?.appendChild(link)
}

private fun createMapboxMap(
    containerId: String,
    accessToken: String,
    zoom: Double,
    styleUri: String,
): mapboxgl.Map {
    mapboxgl.accessToken = accessToken

    val options = js("{}")
    options.container = containerId
    options.style = styleUri
    options.zoom = zoom

    return mapboxgl.Map(options)
}

@Composable
private fun WebButton(
    onClick: () -> Unit,
    backgroundColor: String = ColorJs.WHITE.hex,
    textColor: String = ColorJs.WHITE.hex,
    content: @Composable () -> Unit,
) {
    Button(attrs = {
        style {
            property("width", "48px")
            property("height", "48px")
            property("border-radius", "8px")
            property("background-color", backgroundColor)
            property("border", "none")
            property("cursor", "pointer")
            property("font-size", "20px")
            property("color", textColor)
            property("display", "flex")
            property("align-items", "center")
            property("justify-content", "center")
        }
        onClick { onClick() }
    }) {
        content()
    }
}

@Composable
fun WebOverlayButtons(
    vm: MapViewModel,
    config: MapConfig,
) {
    if (vm.route.isDrawingMode) {
        Div(attrs = {
            style {
                property("position", "absolute")
                property("bottom", "16px")
                property("left", "16px")
                property("display", "flex")
                property("flex-direction", "column")
                property("gap", "8px")
                property("pointer-events", "all")
            }
        }) {
            WebButton(onClick = { vm.route.cancel() }, backgroundColor = ColorJs.RED.hex) {
                if (vm.route.canUndo) {
                    org.jetbrains.compose.web.dom.Span(attrs = {
                        classes("material-icons")
                    }) {
                        org.jetbrains.compose.web.dom
                            .Text("delete")
                    }
                } else {
                    org.jetbrains.compose.web.dom.Span(attrs = {
                        classes("material-icons")
                    }) {
                        org.jetbrains.compose.web.dom
                            .Text("close")
                    }
                }
            }
            WebButton(
                onClick = { vm.route.undoLast() },
                backgroundColor = if (vm.route.canUndo) ColorJs.WHITE.hex else ColorJs.GRAY.hex,
                textColor = if (vm.route.canUndo) ColorJs.BLACK.hex else ColorJs.WHITE.hex,
            ) {
                org.jetbrains.compose.web.dom.Span(attrs = {
                    classes("material-icons")
                }) {
                    org.jetbrains.compose.web.dom
                        .Text("undo")
                }
            }
            WebButton(
                onClick = { vm.route.complete() },
                backgroundColor = if (vm.route.canComplete) ColorJs.BLUE.hex else ColorJs.GRAY.hex,
            ) {
                org.jetbrains.compose.web.dom.Span(attrs = {
                    classes("material-icons")
                }) {
                    org.jetbrains.compose.web.dom
                        .Text("check")
                }
            }
        }
    } else if (config.drawRouteConfig.showCreationButton) {
        Div(attrs = {
            style {
                property("position", "absolute")
                property("bottom", "16px")
                property("left", "16px")
                property("pointer-events", "all")
            }
        }) {
            WebButton(onClick = { vm.route.startNewRoute() }, textColor = ColorJs.BLACK.hex) {
                org.jetbrains.compose.web.dom.Span(attrs = {
                    classes("material-icons")
                }) {
                    org.jetbrains.compose.web.dom
                        .Text("add")
                }
            }
        }
    }
}

private fun emptyFeatureCollection(): dynamic {
    val obj = js("{}")
    obj["type"] = "FeatureCollection"
    obj["features"] = emptyArray<dynamic>()
    return obj
}

private fun geoJsonSource(data: dynamic): dynamic {
    val source = js("{}")
    source["type"] = "geojson"
    source["data"] = data
    return source
}

private fun circleLayer(
    id: String,
    source: String,
    color: String = ColorJs.GREEN.hex,
    radius: Int = 8,
): dynamic {
    val paint = js("{}")
    paint["circle-color"] = color
    paint["circle-radius"] = radius

    val layer = js("{}")
    layer["id"] = id
    layer["type"] = "circle"
    layer["source"] = source
    layer["paint"] = paint
    return layer
}

private fun lineLayer(
    id: String,
    source: String,
    color: String = ColorJs.GREEN.hex,
    width: Double = 4.0,
): dynamic {
    val paint = js("{}")
    paint["line-color"] = color
    paint["line-width"] = width
    paint["line-cap"] = "round"
    paint["line-join"] = "round"

    val layer = js("{}")
    layer["id"] = id
    layer["type"] = "line"
    layer["source"] = source
    layer["paint"] = paint
    return layer
}

private fun buildRouteFeatureCollection(points: List<GeoPoint>): dynamic {
    val features =
        points
            .mapIndexed { _, point ->
                val coords = arrayOf(point.longitude, point.latitude)
                val geometry = js("{}")
                geometry["type"] = "Point"
                geometry["coordinates"] = coords
                val feature = js("{}")
                feature["type"] = "Feature"
                feature["geometry"] = geometry
                feature["properties"] = js("{}")
                feature
            }.toTypedArray()

    val collection = js("{}")
    collection["type"] = "FeatureCollection"
    collection["features"] = features
    return collection
}

private fun buildLineFeatureCollection(points: List<GeoPoint>): dynamic {
    if (points.size < 2) return emptyFeatureCollection()

    val coords = points.map { arrayOf(it.longitude, it.latitude) }.toTypedArray()

    val geometry = js("{}")
    geometry["type"] = "LineString"
    geometry["coordinates"] = coords

    val feature = js("{}")
    feature["type"] = "Feature"
    feature["geometry"] = geometry
    feature["properties"] = js("{}")

    val collection = js("{}")
    collection["type"] = "FeatureCollection"
    collection["features"] = arrayOf(feature)
    return collection
}

private fun sourceExists(
    map: mapboxgl.Map,
    id: String,
): Boolean =
    try {
        map.getSource(id) != null
    } catch (_: Throwable) {
        false
    }
