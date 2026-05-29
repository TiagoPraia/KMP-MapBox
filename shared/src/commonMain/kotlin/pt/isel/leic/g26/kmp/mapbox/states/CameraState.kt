package pt.isel.leic.g26.kmp.mapbox.states

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class CameraState {
    var savedLatitude: Double? = null
    var savedLongitude: Double? = null
    var savedZoom: Double = 17.0
    var followUser by mutableStateOf(true)
    var isAnimating by mutableStateOf(false)

    fun saveCameraPosition(
        lat: Double,
        lng: Double,
    ) {
        savedLatitude = lat
        savedLongitude = lng
    }

    fun resumeFollowing() {
        followUser = true
    }

    fun stopFollowing() {
        followUser = false
    }
}
