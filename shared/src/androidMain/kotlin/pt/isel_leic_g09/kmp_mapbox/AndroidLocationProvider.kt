package pt.isel_leic_g09.kmp_mapbox

import dev.jordond.compass.Location
import dev.jordond.compass.Priority
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.LocationRequest
import dev.jordond.compass.geolocation.TrackingStatus
import dev.jordond.compass.geolocation.mobile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AndroidLocationProvider : LocationProvider {

    private val geolocator = Geolocator.mobile()

    override val locationUpdates: Flow<Location> = geolocator.locationUpdates

    override suspend fun startTracking() {
        geolocator.startTracking(LocationRequest(Priority.HighAccuracy))
    }

    override fun stopTracking() {
        geolocator.stopTracking()
    }
}

actual fun createLocationProvider(): LocationProvider = AndroidLocationProvider()