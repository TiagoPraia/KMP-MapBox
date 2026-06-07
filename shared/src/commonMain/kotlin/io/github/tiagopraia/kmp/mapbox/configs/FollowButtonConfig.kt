package io.github.tiagopraia.kmp.mapbox.configs

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

data class FollowButtonConfig(
    val buttonModifier: Modifier = Modifier.padding(12.dp).size(48.dp),
    val buttonAlignment: Alignment = Alignment.BottomEnd,
    val buttonShape: Shape = CircleShape,
    val followButtonColor: Color = Color.White,
)
