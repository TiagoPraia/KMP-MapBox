package io.github.tiagopraia.kmp.mapbox.gps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager

class GpsStateObserver(
    private val context: Context,
) {
    private var receiver: BroadcastReceiver? = null

    val isGpsEnabled: Boolean
        get() {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }

    fun startObserving(onChange: (Boolean) -> Unit) {
        receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                        onChange(isGpsEnabled)
                    }
                }
            }
        context.registerReceiver(receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
    }

    fun stopObserving() {
        receiver?.let { context.unregisterReceiver(it) }
        receiver = null
    }
}
