package pt.isel.leic.g26.kmp.mapbox.configs

import androidx.compose.ui.graphics.Color

data class RouteConfig(
    val showCreationButton: Boolean = true,
    val pointColor: Color = Color.Green,
    val pointRadius: Double = 8.0,
    val lineColor: Color = Color.Green,
    val lineWidth: Double = 4.0,
    val drawButtonsConfig: DrawingButtonsConfig = DrawingButtonsConfig(),
)
