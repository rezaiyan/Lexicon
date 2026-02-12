package expects

/**
 * WasmJs implementation of network logging
 * Uses console.log for browser developer tools
 */
actual fun logNetwork(tag: String, message: String) {
    println("[$tag] $message")
}

actual fun logPlatform(tag: String, message: String) {
    println("[$tag] $message")
}
