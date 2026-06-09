package io.github.tiagopraia.kmp.mapbox

data class WebFollowButtonConfig(
    val size: String? = BUTTON_SIZE,
    val borderRadius: String? = BUTTON_RADIUS,
    val backgroundColor: String? = "#FFFFFF",
    val iconSize: String? = BUTTON_ICON_SIZE,
    val shadow: String? = BUTTON_SHADOW,
    val margin: String? = BUTTON_MARGIN,
    val position: WebFollowButtonPosition = WebFollowButtonPosition.BOTTOM_RIGHT,
)

enum class WebFollowButtonPosition(
    val mapboxPosition: String,
) {
    TOP_LEFT("top-left"),
    TOP_RIGHT("top-right"),
    BOTTOM_LEFT("bottom-left"),
    BOTTOM_RIGHT("bottom-right"),
}
