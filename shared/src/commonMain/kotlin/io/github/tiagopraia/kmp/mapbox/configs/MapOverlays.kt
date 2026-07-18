package io.github.tiagopraia.kmp.mapbox.configs

import io.github.tiagopraia.kmp.mapbox.GeographicPoint

data class MapOverlays(
    val circles: List<CircleOverlay> = emptyList(),
    val polylines: List<PolylineOverlay> = emptyList(),
)

data class CircleOverlay(
    val id: String,
    val center: GeographicPoint,
    val radius: Double? = null,
    val colorHex: String,
    val isClickable: Boolean = true,
)

data class PolylineOverlay(
    val id: String,
    val points: List<GeographicPoint>,
    val colorHex: String,
    val width: Double? = null,
    val isDashed: Boolean = false,
    val isClickable: Boolean = true,
)
