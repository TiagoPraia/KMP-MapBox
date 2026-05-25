package pt.isel_leic_g09.kmp_mapbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pt.isel_leic_g09.kmp_mapbox.configs.MapConfig

@Composable
expect fun Map(
    accessToken: String,
    vm: MapViewModel,
    config: MapConfig = MapConfig(),
    extraOverlays: List<MapOverlayAction> = emptyList(),
    modifier: Modifier = Modifier,
)
