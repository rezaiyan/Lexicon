package overlay.bottomsheet

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class BottomSheetMode {
    FullScreen,
    Dynamic,
    SizeToFit
}

data class BottomSheetProperties(
    val dismissOnTouchOutside: Boolean = true,
    val dismissOnBackPress: Boolean = true,
    val isNavigationBarsPaddingEnabled: Boolean = false,
    val sheetGesturesEnabled: Boolean = true,
    val showDragHandle: Boolean = true
)

interface BottomSheetOverlayScope {
    var properties: BottomSheetProperties
}

interface BottomSheetScope {
    val isDragHandleShown: Boolean
    fun dismiss()
}

/**
 * Per-page configuration for [BottomSheetPages].
 *
 * @param showBackButton Whether the toolbar shows a back arrow (only when the navigator can go back).
 * @param showCloseButton Whether the toolbar shows a close X (only when `onClose` is provided).
 * @param properties If non-null, auto-synced to the enclosing [BottomSheetOverlayScope] when this page becomes current.
 */
data class BottomSheetPageConfig(
    val showBackButton: Boolean = true,
    val showCloseButton: Boolean = true,
    val properties: BottomSheetProperties? = null,
)

val LocalBottomSheetScope = compositionLocalOf<BottomSheetScope?> { null }

/**
 * Provided by [BottomSheetOverlay] so that [BottomSheetPages] can auto-sync
 * [BottomSheetOverlayScope.properties] without call sites needing `LaunchedEffect`.
 */
val LocalBottomSheetOverlayScope = compositionLocalOf<BottomSheetOverlayScope?> { null }

/**
 * Coordinates the toolbar when [BottomSheetPages] instances are nested.
 *
 * The outermost (root) [BottomSheetPages] creates and provides this via
 * [LocalBottomSheetToolbarOwner]. An inner [BottomSheetPages] detects the
 * parent owner, registers its toolbar state here, and suppresses its own
 * toolbar rendering. The root toolbar then shows the inner state when
 * present, falling back to its own state.
 */
@Stable
class BottomSheetToolbarOwner {
    var innerBack: (() -> Unit)? by mutableStateOf(null)
        internal set
    var innerClose: (() -> Unit)? by mutableStateOf(null)
        internal set
}

val LocalBottomSheetToolbarOwner = compositionLocalOf<BottomSheetToolbarOwner?> { null }
