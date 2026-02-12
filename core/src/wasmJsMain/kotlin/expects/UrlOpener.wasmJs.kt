package expects

/**
 * WasmJs implementation of openUrl
 * Opens URLs using kotlinx.browser window API
 */
actual fun openUrl(url: String) {
    openWindow(url)
}

private fun openWindow(url: String): JsAny? =
    js("window.open(url, '_blank')")
