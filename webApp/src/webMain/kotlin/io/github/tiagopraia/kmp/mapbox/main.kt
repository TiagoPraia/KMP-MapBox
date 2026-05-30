package io.github.tiagopraia.kmp.mapbox

import androidx.compose.runtime.remember
import org.jetbrains.compose.web.renderComposable

fun main() {
    renderComposable(rootElementId = "root") {
        val vm = remember { MapViewModel() }
        Map(
            accessToken = "MY_TOKEN",
            vm = vm,
        )
    }
}
