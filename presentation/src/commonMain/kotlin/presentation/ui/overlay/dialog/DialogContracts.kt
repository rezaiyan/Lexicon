package presentation.ui.overlay.dialog

/**
 * Behaviour & styling toggles for dialogs.
 */
data class DialogProperties(
    val dismissOnTouchOutside: Boolean = true,
    val dismissOnBackPress: Boolean = true
)

/**
 * Scope exposed to dialog content so it can read/adjust live properties if needed.
 * Keep this small — most things should be passed as props to the content itself.
 */
interface DialogOverlayScope {
    var properties: DialogProperties
}

