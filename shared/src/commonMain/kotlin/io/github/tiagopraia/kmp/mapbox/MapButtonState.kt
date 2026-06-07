package io.github.tiagopraia.kmp.mapbox

data class MapButtonState(
    val onProfileClick: (() -> Unit)? = null,
    val onTrailsClick: (() -> Unit)? = null,
    val isDrawingMode: Boolean = false,
    val canUndo: Boolean = false,
    val canComplete: Boolean = false,
    val onStartRoute: (() -> Unit)? = null,
    val onUndo: (() -> Unit)? = null,
    val onCancel: (() -> Unit)? = null,
    val onComplete: (() -> Unit)? = null,
)
