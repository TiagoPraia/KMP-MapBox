package io.github.tiagopraia.kmp.mapbox

import io.github.tiagopraia.kmp.mapbox.configs.MapStyle

data class WebMapConfig(
    val initialZoom: Double = 17.0,
    val styleUri: String = MapStyle.OUTDOORS,
    val showAccuracyRing: Boolean = true,
    val pointRadius: Double = 8.0,
    val lineWidth: Double = 4.0,
    val enableHighAccuracy: Boolean = true,
    val trackUserLocation: Boolean = true,
    val showUserHeading: Boolean = true,
    val animationDuration: Int = 1000,
    val followButton: WebFollowButtonConfig? = WebFollowButtonConfig(),
)
