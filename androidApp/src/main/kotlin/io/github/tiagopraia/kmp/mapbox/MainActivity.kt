package io.github.tiagopraia.kmp.mapbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.tiagopraia.kmp.mapbox.config.AndroidMapConfig
import io.github.tiagopraia.kmp.mapbox.configs.CircleOverlay
import io.github.tiagopraia.kmp.mapbox.configs.MapOverlays
import io.github.tiagopraia.kmp.mapbox.configs.PolylineOverlay
import io.github.tiagopraia.kmp.mapbox.map.AndroidMapWrapper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            TestScreen(BuildConfig.MAPBOX_ACCESS_TOKEN)
//            AndroidMapWrapper(
//                accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN,
//            )
        }
    }
}

@Composable
fun TestScreen(accessToken: String) {
    // Overlays "de nascença", fixos
    val initialCircles =
        listOf(
            CircleOverlay(
                id = "start",
                center = GeographicPoint(latitude = 38.7223, longitude = -9.1393, altitude = 0.0),
                colorHex = "#00FF00",
                radius = 8.0,
                isClickable = true,
            ),
            CircleOverlay(
                id = "end",
                center = GeographicPoint(latitude = 38.7300, longitude = -9.1500, altitude = 0.0),
                colorHex = "#FF0000",
                radius = 8.0,
                isClickable = true,
            ),
        )
    val initialPolylines =
        listOf(
            PolylineOverlay(
                id = "trail-1",
                points =
                    listOf(
                        GeographicPoint(38.7223, -9.1393, 0.0),
                        GeographicPoint(38.7260, -9.1440, 0.0),
                        GeographicPoint(38.7300, -9.1500, 0.0),
                    ),
                colorHex = "#0000FF",
                isClickable = true,
                width = 8.0,
                isDashed = false,
            ),
        )

    // Overlays dinamicamente adicionados por clique — precisa de ser mutableStateOf
    // para o LaunchedEffect(overlays, ...) disparar de novo
    var dynamicCircles by remember { mutableStateOf(listOf<CircleOverlay>()) }
    var clickCounter by remember { mutableIntStateOf(0) }

    val overlays =
        remember(dynamicCircles) {
            MapOverlays(
                circles = initialCircles + dynamicCircles,
                polylines = initialPolylines,
            )
        }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidMapWrapper(
                accessToken = accessToken,
                overlays = overlays,
                config = AndroidMapConfig(),
                onMapReady = { println("Map ready") },
                onOverlayClick = { id ->
                    println("Overlay clicked: $id")
                },
                onMapClick = { point ->
                    // Clique fora de qualquer overlay -> cria um novo círculo ali
                    clickCounter++
                    val newOverlay =
                        CircleOverlay(
                            id = "dynamic-$clickCounter",
                            center = point,
                            colorHex = "#FFA500",
                            radius = 8.0,
                            isClickable = true,
                        )
                    dynamicCircles = dynamicCircles + newOverlay
                    println("Map clicked at: $point -> created overlay dynamic-$clickCounter")
                    true
                },
                onLocationUpdate = null,
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {}
