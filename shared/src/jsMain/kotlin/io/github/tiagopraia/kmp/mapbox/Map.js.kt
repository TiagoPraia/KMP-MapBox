package io.github.tiagopraia.kmp.mapbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig

@Composable
actual fun Map(
    accessToken: String,
    vm: MapViewModel,
    config: MapConfig,
    extraOverlays: List<MapOverlayAction>,
    modifier: Modifier,
) {
}
