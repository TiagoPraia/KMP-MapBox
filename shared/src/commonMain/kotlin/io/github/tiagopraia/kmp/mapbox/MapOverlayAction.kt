package io.github.tiagopraia.kmp.mapbox

import androidx.compose.runtime.Composable

data class MapOverlayAction(
    val content: @Composable () -> Unit,
)
