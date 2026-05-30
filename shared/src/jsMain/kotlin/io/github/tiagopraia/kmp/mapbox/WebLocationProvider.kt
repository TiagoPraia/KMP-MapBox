package io.github.tiagopraia.kmp.mapbox

import dev.jordond.compass.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class WebLocationProvider : LocationProvider {
    override val locationUpdates: Flow<Location> = emptyFlow()

    override suspend fun startTracking() { }

    override fun stopTracking() { }
}

actual fun createLocationProvider(): LocationProvider = WebLocationProvider()
