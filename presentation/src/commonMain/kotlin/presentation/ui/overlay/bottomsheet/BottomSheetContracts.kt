package presentation.ui.overlay.bottomsheet

import androidx.compose.runtime.compositionLocalOf

/**
 * Public bottom sheet modes.
 */
enum class BottomSheetMode {
    /** Takes the entire height. We hide drag handle and pad for system bars if requested. */
    FullScreen,

    /** Default M3 behavior: partially-expanded supported (if the content fits). */
    Dynamic,

    /** Content size drives height (no partially-expanded state). */
    SizeToFit
}

/**
 * Behaviour & styling toggles you might want to vary per-sheet.
 */
data class BottomSheetProperties(
    val dismissOnTouchOutside: Boolean = true,
    val dismissOnBackPress: Boolean = true,
    val isNavigationBarsPaddingEnabled: Boolean = false,
    val sheetGesturesEnabled: Boolean = true
)

/**
 * Scope exposed to sheet content so it can read/adjust live properties if needed.
 * Keep this small — most things should be passed as props to the content itself.
 */
interface BottomSheetOverlayScope {
    var properties: BottomSheetProperties
}

/**
 * Internal scope to expose handy bits (e.g., is drag handle shown) to child composables.
 */
interface BottomSheetScope {
    val isDragHandleShown: Boolean
    fun dismiss()
}

/**
 * CompositionLocal for accessing bottom sheet scope in child composables.
 */
val LocalBottomSheetScope = compositionLocalOf<BottomSheetScope?> { null }

