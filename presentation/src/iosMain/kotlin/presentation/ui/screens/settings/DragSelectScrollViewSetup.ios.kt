package presentation.ui.screens.settings

import androidx.compose.runtime.Composable

// No-op: dragSelectGesture runs in common Compose pointer input, which works
// on iOS without any UIKit gesture recognizer workarounds.
@Composable
internal actual fun DragSelectScrollViewSetup() = Unit
