package io.github.tiagopraia.kmp.mapbox.map

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.tiagopraia.kmp.mapbox.AnchoredOverlay
import io.github.tiagopraia.kmp.mapbox.GeographicPoint
import io.github.tiagopraia.kmp.mapbox.config.AndroidMapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapOverlays
import io.github.tiagopraia.kmp.mapbox.gps.rememberGpsState

private const val REQUESTED_LOCATION = "location_permanently_denied"

private fun isDeniedPermanently(context: Context): Boolean =
    context
        .getSharedPreferences("map_prefs", Context.MODE_PRIVATE)
        .getBoolean(REQUESTED_LOCATION, false)

private fun markDeniedPermanently(context: Context) =
    context
        .getSharedPreferences("map_prefs", Context.MODE_PRIVATE)
        .edit {
            putBoolean(REQUESTED_LOCATION, true)
        }

@Composable
fun AndroidMapWrapper(
    accessToken: String,
    overlays: MapOverlays = MapOverlays(),
    config: AndroidMapConfig = AndroidMapConfig(),
    onMapReady: () -> Unit = {},
    onMapClick: ((GeographicPoint) -> Boolean)? = null,
    anchoredOverlays: List<AnchoredOverlay> = emptyList(),
    onOverlayClick: (id: String, point: GeographicPoint) -> Unit = { _, _ -> },
    onLocationUpdate: ((GeographicPoint) -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissionState by rememberSaveable {
        mutableStateOf(
            when {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED -> PermissionState.GRANTED

                isDeniedPermanently(context) -> PermissionState.DENIED_PERMANENTLY

                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    ) -> PermissionState.DENIED_ONCE

                else -> PermissionState.NOT_ASKED
            },
        )
    }

    val isGpsEnabled = rememberGpsState()
    var mapHasInitialized by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher =
        createPermissionLauncher(
            context = context,
            activity = activity,
            currentState = { permissionState },
            onStateChange = { permissionState = it },
        )

    LaunchedEffect(Unit) {
        if (permissionState == PermissionState.NOT_ASKED) {
            permissionLauncher(permissionLauncher)
        }
    }

    // Reevaluate after settings
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME &&
                    permissionState == PermissionState.DENIED_PERMANENTLY
                ) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionState = PermissionState.GRANTED
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MakeChoice(
        accessToken = accessToken,
        context = context,
        permissionLauncher = permissionLauncher,
        permissionState = permissionState,
        isGpsEnabled = isGpsEnabled,
        mapHasInitialized = mapHasInitialized,
        onMapHasInitialized = { mapHasInitialized = true },
        config = config,
        overlays = overlays,
        anchoredOverlays = anchoredOverlays,
        onMapReady = onMapReady,
        onMapClick = onMapClick,
        onOverlayClick = onOverlayClick,
        onLocationUpdate = onLocationUpdate,
        modifier = modifier,
    )
}

@Composable
private fun MakeChoice(
    accessToken: String,
    context: Context,
    permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    permissionState: PermissionState,
    isGpsEnabled: Boolean,
    mapHasInitialized: Boolean,
    onMapHasInitialized: () -> Unit,
    config: AndroidMapConfig,
    overlays: MapOverlays,
    anchoredOverlays: List<AnchoredOverlay>,
    onMapReady: () -> Unit,
    onMapClick: ((GeographicPoint) -> Boolean)?,
    onOverlayClick: (id: String, point: GeographicPoint) -> Unit,
    onLocationUpdate: ((GeographicPoint) -> Unit)?,
    modifier: Modifier,
) {
    when (permissionState) {
        PermissionState.GRANTED ->
            if (isGpsEnabled || mapHasInitialized) {
                AndroidMap(
                    accessToken = accessToken,
                    config = config,
                    overlays = overlays,
                    anchoredOverlays = anchoredOverlays,
                    onOverlayClick = onOverlayClick,
                    onMapReady = {
                        onMapHasInitialized()
                        onMapReady()
                    },
                    onMapClick = onMapClick,
                    isGpsEnabled = isGpsEnabled,
                    onLocationUpdate = onLocationUpdate,
                    modifier = modifier,
                )
            } else {
                LocationDisabled()
            }
        PermissionState.NOT_ASKED -> { }
        PermissionState.DENIED_ONCE ->
            PermissionDeniedOnce {
                permissionLauncher(permissionLauncher)
            }
        PermissionState.DENIED_PERMANENTLY -> PermissionDeniedPermanently(context)
    }
}

@Composable
private fun createPermissionLauncher(
    context: Context,
    activity: Activity,
    currentState: () -> PermissionState,
    onStateChange: (PermissionState) -> Unit,
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

    val newPermissionState =
        when {
            fineGranted -> PermissionState.GRANTED

            currentState() == PermissionState.DENIED_ONCE &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) -> {
                markDeniedPermanently(context)
                PermissionState.DENIED_PERMANENTLY
            }

            coarseGranted ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) -> PermissionState.DENIED_ONCE

            else -> PermissionState.NOT_ASKED
        }
    onStateChange(newPermissionState)
}

private fun permissionLauncher(
    permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>,
) {
    permissionLauncher.launch(
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ),
    )
}

private enum class PermissionState {
    NOT_ASKED,
    GRANTED,
    DENIED_ONCE,
    DENIED_PERMANENTLY,
}

@Composable
private fun PermissionDeniedOnce(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = "Precise location access is required for the map to work properly",
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Give Permission")
            }
        }
    }
}

@Composable
private fun PermissionDeniedPermanently(context: Context) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = "Location permission was permanently denied. Please enable it in Settings or reinstall the app.",
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            }) {
                Text("Open Settings")
            }
        }
    }
}

@Composable
private fun LocationDisabled() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = "Location services are disabled, please activate your GPS to initizalize the map",
                textAlign = TextAlign.Center,
            )
        }
    }
}
