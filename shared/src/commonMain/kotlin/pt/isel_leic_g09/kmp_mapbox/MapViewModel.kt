package pt.isel_leic_g09.kmp_mapbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import pt.isel_leic_g09.kmp_mapbox.states.CameraState
import pt.isel_leic_g09.kmp_mapbox.states.RouteState

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
