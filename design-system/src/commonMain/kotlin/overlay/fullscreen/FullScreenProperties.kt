package overlay.fullscreen

data class FullScreenProperties(
    val dismissOnBackPress: Boolean = true,
    val isStatusBarsPaddingEnabled: Boolean = true,
    val isNavigationBarsPaddingEnabled: Boolean = true,
    val dismissOnSwipe: Boolean = false,
)
