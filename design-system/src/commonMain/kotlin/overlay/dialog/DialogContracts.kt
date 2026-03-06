package overlay.dialog

data class DialogProperties(
    val dismissOnTouchOutside: Boolean = true,
    val dismissOnBackPress: Boolean = true
)

interface DialogOverlayScope {
    var properties: DialogProperties
}
