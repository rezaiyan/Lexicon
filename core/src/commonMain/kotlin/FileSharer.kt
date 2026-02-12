package expects

/**
 * Platform-specific file sharing functionality
 * Creates a temporary file and shares it using the native share sheet
 */
expect fun shareTextAsFile(
    title: String,
    text: String,
    filename: String
)

