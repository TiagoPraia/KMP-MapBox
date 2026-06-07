package io.github.tiagopraia.kmp.mapbox.configs

import io.github.tiagopraia.kmp.mapbox.GeoPoint

data class MapOverlays(
    val circles: List<CircleOverlay> = emptyList(),
    val polylines: List<PolylineOverlay> = emptyList(),
)

data class CircleOverlay(
    val id: String,
    val center: GeoPoint,
    val radius: Double,
    val colorHex: String,
    val isClickable: Boolean = true,
)

data class PolylineOverlay(
    val id: String,
    val points: List<GeoPoint>,
    val colorHex: String,
    val width: Double,
    val isDashed: Boolean = false,
    val isClickable: Boolean = true,
)
