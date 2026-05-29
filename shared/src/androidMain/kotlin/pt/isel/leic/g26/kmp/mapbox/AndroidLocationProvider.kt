package pt.isel.leic.g26.kmp.mapbox

import dev.jordond.compass.Location
import dev.jordond.compass.Priority
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.LocationRequest
import dev.jordond.compass.geolocation.mobile
import kotlinx.coroutines.flow.Flow

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
