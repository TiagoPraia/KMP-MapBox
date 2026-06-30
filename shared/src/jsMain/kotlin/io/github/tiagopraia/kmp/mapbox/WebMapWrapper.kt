package io.github.tiagopraia.kmp.mapbox

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.tiagopraia.kmp.mapbox.configs.MapOverlays
import org.w3c.dom.HTMLElement

@Composable
fun WebMapWrapper(
    accessToken: String,
    overlays: MapOverlays = MapOverlays(),
    config: WebMapConfig = WebMapConfig(),
    onMapClick: ((GeographicPoint) -> Boolean)? = null,
    onOverlayClick: (id: String) -> Unit = {},
    modifier: Modifier = Modifier.fillMaxSize(),
    extraHTML: List<HTMLElement> = emptyList(),
) {
    WebMap(
        accessToken = accessToken,
        overlays = overlays,
        config = config,
        onMapClick = onMapClick,
        onOverlayClick = onOverlayClick,
        extraHTML = extraHTML,
        modifier = modifier,
    )
}
