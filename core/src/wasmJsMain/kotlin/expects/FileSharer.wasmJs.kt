package expects

/**
 * WasmJs implementation of shareTextAsFile
 * No-op on web platform
 */
actual fun shareTextAsFile(
    title: String,
    text: String,
    filename: String
) {
    // No-op: file sharing not supported on web
}
