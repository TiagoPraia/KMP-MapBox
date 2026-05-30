package io.github.tiagopraia.kmp.mapbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig
import kotlinx.browser.document
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

        fun addControl(control: dynamic)

        fun resize()
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

    Div(
        attrs = {
            id(containerId)
            style {
                property("width", "100%")
                property("height", "100vh")
                property("position", "absolute")
                property("top", "0")
                property("left", "0")
            }
        },
    )
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
