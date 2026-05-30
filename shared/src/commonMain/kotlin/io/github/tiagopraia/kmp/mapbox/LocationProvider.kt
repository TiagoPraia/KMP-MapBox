package io.github.tiagopraia.kmp.mapbox

import dev.jordond.compass.Location
import kotlinx.coroutines.flow.Flow

interface LocationProvider {
    val locationUpdates: Flow<Location>

    suspend fun startTracking()

    fun stopTracking()
}

expect fun createLocationProvider(): LocationProvider
