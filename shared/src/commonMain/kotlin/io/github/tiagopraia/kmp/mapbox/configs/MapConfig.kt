package io.github.tiagopraia.kmp.mapbox.configs

data class MapConfig(
    val initialZoom: Double = 17.0,
    val animationDuration: Long = 1000,
    val styleUri: String = MapStyle.OUTDOORS,
    val pointRadius: Double = 8.0,
    val lineWidth: Double = 4.0,
    val overlayMinZoom: Double = 11.0,
    val overlayMaxZoom: Double = 24.0,
    val overlayMinFactor: Double = 0.5,
    val overlayMaxFactor: Double = 4.0,
)
