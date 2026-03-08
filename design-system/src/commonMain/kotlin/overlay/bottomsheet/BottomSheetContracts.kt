package overlay.bottomsheet

import androidx.compose.runtime.compositionLocalOf

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
    val showCloseButton: Boolean = false
)

interface BottomSheetOverlayScope {
    var properties: BottomSheetProperties
}

interface BottomSheetScope {
    val isDragHandleShown: Boolean
    val isCloseButtonShown: Boolean
    fun dismiss()
}

val LocalBottomSheetScope = compositionLocalOf<BottomSheetScope?> { null }
