package expects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

data class SystemBarsState(
    val statusBarColor: Color,
    val navigationBarColor: Color,
    val darkIcons: Boolean
)

class SystemBarsController(default: SystemBarsState) {
    var defaultState by mutableStateOf(default)
    private var override: SystemBarsState? by mutableStateOf(null)

    val currentState: SystemBarsState
        get() = override ?: defaultState

    internal fun setOverride(state: SystemBarsState?) {
        override = state
    }
}

val LocalSystemBarsController = compositionLocalOf<SystemBarsController> {
    error("No SystemBarsController provided — wrap your content with CompositionLocalProvider")
}

/**
 * Temporarily overrides system bar colors while this composable is in the tree.
 * Reverts to the default when removed from composition.
 *
 * Works from anywhere in the compose tree, including inside ModalBottomSheet dialogs,
 * because it both updates the shared controller (for the activity window) and calls
 * [SetSystemBarsColor] locally (for dialog windows).
 */
@Composable
fun OverrideSystemBars(
    statusBarColor: Color,
    navigationBarColor: Color,
    darkIcons: Boolean
) {
    val controller = LocalSystemBarsController.current
    val state = remember(statusBarColor, navigationBarColor, darkIcons) {
        SystemBarsState(statusBarColor, navigationBarColor, darkIcons)
    }

    // Apply directly — handles dialog windows (ModalBottomSheet) that have their own Window
    SetSystemBarsColor(statusBarColor, navigationBarColor, darkIcons)

    // Update the shared controller so the activity-level observer also applies the change
    SideEffect { controller.setOverride(state) }
    DisposableEffect(Unit) {
        onDispose { controller.setOverride(null) }
    }
}
