package pt.isel_leic_g09.kmp_mapbox

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch

class MapViewModel() : ViewModel() {
    companion object {
        fun getFactory() =
            viewModelFactory {
                initializer {
                    MapViewModel()
                }
            }
    }

    val locationProvider: LocationProvider = createLocationProvider()

    var savedLatitude: Double? = null
    var savedLongitude: Double? = null
    var savedZoom: Double = 17.0

    var followUser by mutableStateOf(true)
    var isAnimating by mutableStateOf(false)

    init {
        viewModelScope.launch {
            locationProvider.startTracking()
        }
    }

    fun saveCameraPosition(lat: Double, lng: Double) {
        savedLatitude = lat
        savedLongitude = lng
    }

    fun resumeFollowing() {
        followUser = true
    }

    fun stopFollowing() {
        followUser = false
    }

    override fun onCleared() {
        super.onCleared()
        locationProvider.stopTracking()
    }
}