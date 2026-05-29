package pt.isel.leic.g26.kmp.mapbox

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pt.isel.leic.g26.kmp.mapbox.configs.DrawingButtonsConfig
import pt.isel.leic.g26.kmp.mapbox.configs.FollowButtonConfig
import pt.isel.leic.g26.kmp.mapbox.configs.MapConfig
import pt.isel.leic.g26.kmp.mapbox.states.RouteState

@Composable
internal fun BoxScope.MapOverlayButtons(
    vm: MapViewModel,
    config: MapConfig,
    extraOverlays: List<MapOverlayAction> = emptyList(),
) {
    if (config.followButton.showFollowButton) {
        FollowButton(
            isFollowing = vm.camera.followUser,
            config = config.followButton,
            onClick = { vm.camera.resumeFollowing() },
        )
    }

    if (vm.route.isDrawingMode) {
        DrawingButtons(
            route = vm.route,
            config = config.drawRouteConfig.drawButtonsConfig,
        )
    } else if (config.drawRouteConfig.showCreationButton) {
        CreationButton(
            config = config.drawRouteConfig.drawButtonsConfig,
            onClick = { vm.route.startNewRoute() },
        )
    }

    extraOverlays.forEach { it.content() }
}

@Composable
fun BoxScope.FollowButton(
    isFollowing: Boolean,
    config: FollowButtonConfig,
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        shape = config.buttonShape,
        containerColor =
            if (isFollowing) {
                config.followButtonActiveColor
            } else {
                config.followButtonInactiveColor
            },
        modifier =
            Modifier
                .align(config.buttonAlignment)
                .then(config.buttonModifier),
    ) {
        Icon(
            imageVector = Icons.Filled.Explore,
            contentDescription = "Explore",
        )
    }
}

@Composable
fun BoxScope.CreationButton(
    config: DrawingButtonsConfig,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.align(config.columnAlignment).then(config.columnModifier),
        horizontalAlignment = config.columnHorizontalAlignment,
        verticalArrangement = config.columnVerticalArrangement,
    ) {
        FloatingActionButton(
            onClick = onClick,
            shape = config.buttonsShape,
            containerColor = config.createButtonColor,
            modifier = config.buttonsModifier,
        ) {
            Icon(
                imageVector = config.createButtonImage,
                contentDescription = "Start route creation",
                tint = config.createButtonTint,
            )
        }
    }
}

@Composable
private fun BoxScope.DrawingButtons(
    route: RouteState,
    config: DrawingButtonsConfig,
) {
    Column(
        modifier = Modifier.align(config.columnAlignment).then(config.columnModifier),
        horizontalAlignment = config.columnHorizontalAlignment,
        verticalArrangement = config.columnVerticalArrangement,
    ) {
        FloatingActionButton(
            onClick = { route.cancel() },
            shape = config.buttonsShape,
            containerColor = config.cancelButtonColor,
            modifier = config.buttonsModifier,
        ) {
            Icon(
                imageVector = if (route.canUndo) config.cancelButtonImageWithValues else config.cancelButtonImageWithoutValues,
                contentDescription = "Cancel route",
                tint = config.cancelButtonTint,
            )
        }

        FloatingActionButton(
            onClick = { route.undoLast() },
            shape = config.buttonsShape,
            containerColor = if (route.canUndo) config.undoButtonEnableColor else config.undoButtonDisableColor,
            modifier = config.buttonsModifier,
        ) {
            Icon(
                imageVector = config.undoButtonImage,
                contentDescription = "Undo last point",
                tint = config.undoButtonTint,
            )
        }

        FloatingActionButton(
            onClick = { route.complete() },
            shape = config.buttonsShape,
            containerColor = if (route.canComplete) config.submitButtonEnableColor else config.submitButtonDisableColor,
            modifier = config.buttonsModifier,
        ) {
            Icon(
                imageVector = config.submitButtonImage,
                contentDescription = "Complete route",
                tint = config.submitButtonTint,
            )
        }
    }
}
