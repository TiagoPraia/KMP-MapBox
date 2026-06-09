package io.github.tiagopraia.kmp.mapbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.tiagopraia.kmp.mapbox.map.AndroidMapWrapper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AndroidMapWrapper(
                accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN,
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {}
