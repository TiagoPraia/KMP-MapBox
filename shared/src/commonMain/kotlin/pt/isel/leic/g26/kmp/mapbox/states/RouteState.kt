package pt.isel.leic.g26.kmp.mapbox.states

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pt.isel.leic.g26.kmp.mapbox.GeoPoint

class RouteState {
    var isDrawingMode by mutableStateOf(false)
    var routeId by mutableStateOf(0)

    private val _points = mutableStateListOf<GeoPoint>()
    val points: List<GeoPoint> get() = _points

    var completedRoute: List<GeoPoint>? by mutableStateOf(null)
        private set

    val canComplete: Boolean get() = _points.size >= 2
    val canUndo: Boolean get() = _points.isNotEmpty()

    fun startNewRoute() {
        completedRoute = null
        routeId++
        isDrawingMode = true
    }

    fun addPoint(point: GeoPoint) {
        _points.add(point)
    }

    fun undoLast() {
        if (_points.isNotEmpty()) _points.removeLast()
    }

    fun complete() {
        if (canComplete) {
            completedRoute = _points.toList()
            _points.clear()
            isDrawingMode = false
        }
    }

    fun cancel() {
        val cancelDrawMode = !canUndo
        _points.clear()
        completedRoute = null
        if (cancelDrawMode) isDrawingMode = false
    }
}
