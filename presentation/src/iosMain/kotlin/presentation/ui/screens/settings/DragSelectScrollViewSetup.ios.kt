package presentation.ui.screens.settings

import androidx.compose.runtime.Composable

// No-op: long press is handled by WordCard's combinedClickable, which works
// natively on iOS without any UIKit gesture recognizer workarounds.
@Composable
internal actual fun DragSelectScrollViewSetup() = Unit
