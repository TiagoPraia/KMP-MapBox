package io.github.tiagopraia.kmp.mapbox

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapOverlays

@Composable
fun WebMapWrapper(
    accessToken: String,
    overlays: MapOverlays = MapOverlays(),
    config: MapConfig = MapConfig(),
    onMapReady: () -> Unit = {},
    onMapClick: ((GeoPoint) -> Boolean)? = null,
    onOverlayClick: (id: String) -> Unit = {},
    modifier: Modifier = Modifier.fillMaxSize(),
    buttonState: MapButtonState? = MapButtonState(),
) {
    WebMap(
        accessToken = accessToken,
        overlays = overlays,
        config = config,
        onMapReady = onMapReady,
        onMapClick = onMapClick,
        onOverlayClick = onOverlayClick,
        buttonState = buttonState,
        modifier = modifier,
    )
}
