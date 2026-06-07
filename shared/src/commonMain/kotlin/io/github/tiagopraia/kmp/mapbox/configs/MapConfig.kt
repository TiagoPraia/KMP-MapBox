package io.github.tiagopraia.kmp.mapbox.configs

data class MapConfig(
    val initialZoom: Double = 17.0,
    val animationDuration: Long = 1000,
    val styleUri: String = MapStyle.OUTDOORS,
    val compassPosition: Int = -1,
    val compassMarginTop: Float = 75f,
    val compassMarginLeft: Float = 16f,
    val compassMarginRight: Float = -1f,
    val compassMarginBottom: Float = -1f,
    val locationPulseAnimation: Boolean = false,
    val showAccuracyRing: Boolean = true,
    val pointRadius: Double = 8.0,
    val lineWidth: Double = 4.0,
    val followButton: FollowButtonConfig = FollowButtonConfig(),
)
