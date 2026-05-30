package io.github.tiagopraia.kmp.mapbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.tiagopraia.kmp.mapbox.states.CameraState
import io.github.tiagopraia.kmp.mapbox.states.RouteState
import kotlinx.coroutines.launch

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
