package io.github.tiagopraia.kmp.mapbox

import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.getElementById("root") as HTMLElement) {
        WebMapWrapper(
            accessToken = BuildKonfig.MAPBOX_ACCESS_TOKEN,
        )
    }
}
