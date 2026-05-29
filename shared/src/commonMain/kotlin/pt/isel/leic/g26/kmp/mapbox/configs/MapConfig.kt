package pt.isel.leic.g26.kmp.mapbox.configs

import androidx.compose.ui.graphics.Color

data class MapConfig(
    val initialZoom: Double = 17.0,
    val animationDuration: Long = 1000,
    val styleUri: String = MapStyle.OUTDOORS,
    val locationPulseAnimation: Boolean = false,
    val showAccuracyRing: Boolean = true,
    val followButton: FollowButtonConfig = FollowButtonConfig(),
    val drawRouteConfig: RouteConfig = RouteConfig(),
)

val LightBlue = Color(0xFF3398FF)
