package pt.isel.leic.g26.kmp.mapbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import pt.isel.leic.g26.kmp.mapbox.states.CameraState
import pt.isel.leic.g26.kmp.mapbox.states.RouteState

class MapViewModel : ViewModel() {
    companion object {
        fun getFactory() =
            viewModelFactory {
                initializer { MapViewModel() }
            }
    }

    val locationProvider: LocationProvider = createLocationProvider()
    val camera = CameraState()
    val route = RouteState()

    init {
        viewModelScope.launch {
            locationProvider.startTracking()
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationProvider.stopTracking()
    }
}
