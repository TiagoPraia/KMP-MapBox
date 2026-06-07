package io.github.tiagopraia.kmp.mapbox.gps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberGpsState(): Boolean {
    val context = LocalContext.current
    var isGpsEnabled by rememberSaveable {
        mutableStateOf(GpsStateObserver(context).isGpsEnabled)
    }

    DisposableEffect(context) {
        val observer = GpsStateObserver(context)
        observer.startObserving { enabled ->
            isGpsEnabled = enabled
        }
        onDispose { observer.stopObserving() }
    }

    return isGpsEnabled
}
