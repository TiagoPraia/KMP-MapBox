package pt.isel_leic_g09.kmp_mapbox

import androidx.compose.runtime.Composable

data class MapOverlayAction(
    val content: @Composable () -> Unit,
)
