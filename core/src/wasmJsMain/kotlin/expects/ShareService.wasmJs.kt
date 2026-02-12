package expects

/**
 * WasmJs implementation of ShareService
 * No-op on web platform
 */
private class WasmJsShareService : ShareService {
    override fun share(title: String, text: String) {
        // No-op: Web Share API requires user gesture context which is hard to guarantee
    }
}

actual fun getShareService(): ShareService = WasmJsShareService()
