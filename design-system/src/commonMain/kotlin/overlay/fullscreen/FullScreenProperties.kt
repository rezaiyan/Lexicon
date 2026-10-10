package overlay.fullscreen

data class FullScreenProperties(
    val dismissOnBackPress: Boolean = true,
    val isStatusBarsPaddingEnabled: Boolean = true,
    val isNavigationBarsPaddingEnabled: Boolean = true,
    val dismissOnSwipe: Boolean = false,
    /**
     * Colour painted behind the content and the system bars. Must match the content's own
     * background, otherwise the status and navigation bars show as differently coloured bands.
     */
    val container: FullScreenContainer = FullScreenContainer.Surface,
)

/** Theme roles a full-screen overlay can paint edge to edge; resolved at composition so theme changes apply. */
enum class FullScreenContainer { Surface, Background }
