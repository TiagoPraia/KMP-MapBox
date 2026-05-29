package pt.isel.leic.g26.kmp.mapbox

import androidx.compose.runtime.Composable

data class MapOverlayAction(
    val content: @Composable () -> Unit,
)
