package io.github.tiagopraia.kmp.mapbox.config

import io.github.tiagopraia.kmp.mapbox.configs.MapConfig

data class AndroidMapConfig(
    val compassPosition: Int = -1,
    val compassMarginTop: Float = 75f,
    val compassMarginLeft: Float = 16f,
    val compassMarginRight: Float = -1f,
    val compassMarginBottom: Float = -1f,
    val locationPulseAnimation: Boolean = false,
    val showAccuracyRing: Boolean = true,
    val mapConfig: MapConfig = MapConfig(),
    val followButton: AndroidFollowButtonConfig = AndroidFollowButtonConfig(),
)
