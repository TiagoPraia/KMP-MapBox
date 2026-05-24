package pt.isel_leic_g09.kmp_mapbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun Map(
    accessToken: String,
    vm: MapViewModel,
    modifier: Modifier = Modifier,
)