package io.github.tiagopraia.kmp.mapbox

import androidx.compose.runtime.Composable

data class AnchoredOverlay(
    val id: String,
    val point: GeographicPoint,
    val widthDp: Double = 500.0,
    val heightDp: Double = 500.0,
    val content: @Composable () -> Unit,
)
