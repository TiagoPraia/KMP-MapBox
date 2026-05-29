package pt.isel.leic.g26.kmp.mapbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pt.isel.leic.g26.kmp.mapbox.configs.MapConfig

@Composable
expect fun Map(
    accessToken: String,
    vm: MapViewModel,
    config: MapConfig = MapConfig(),
    extraOverlays: List<MapOverlayAction> = emptyList(),
    modifier: Modifier = Modifier,
)
