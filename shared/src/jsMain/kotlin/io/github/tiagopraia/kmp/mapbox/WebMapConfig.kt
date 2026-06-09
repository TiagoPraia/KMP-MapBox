package io.github.tiagopraia.kmp.mapbox

import io.github.tiagopraia.kmp.mapbox.configs.MapConfig

data class WebMapConfig(
    val showAccuracyRing: Boolean = true,
    val enableHighAccuracy: Boolean = true,
    val trackUserLocation: Boolean = true,
    val showUserHeading: Boolean = true,
    val mapConfig: MapConfig = MapConfig(),
    val followButton: WebFollowButtonConfig? = WebFollowButtonConfig(),
)
