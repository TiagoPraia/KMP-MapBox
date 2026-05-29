package pt.isel.leic.g26.kmp.mapbox

import dev.jordond.compass.Location
import kotlinx.coroutines.flow.Flow

class WebLocationProvider : LocationProvider {
    override val locationUpdates: Flow<Location> = TODO("Not yet implemented")

    override suspend fun startTracking() {
        TODO("Not yet implemented")
    }

    override fun stopTracking() {
        TODO("Not yet implemented")
    }
}

actual fun createLocationProvider(): LocationProvider = WebLocationProvider()
