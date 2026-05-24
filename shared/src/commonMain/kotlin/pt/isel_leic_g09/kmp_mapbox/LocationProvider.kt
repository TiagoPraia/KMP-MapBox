package pt.isel_leic_g09.kmp_mapbox

import kotlinx.coroutines.flow.Flow
import dev.jordond.compass.Location

interface LocationProvider {
    val locationUpdates: Flow<Location>
    suspend fun startTracking()
    fun stopTracking()
}

expect fun createLocationProvider(): LocationProvider