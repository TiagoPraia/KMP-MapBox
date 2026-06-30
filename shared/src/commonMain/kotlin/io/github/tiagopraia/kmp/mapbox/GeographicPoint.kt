package io.github.tiagopraia.kmp.mapbox

data class GeographicPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
) {
    override fun toString() = "($latitude;$longitude;$altitude)"
}