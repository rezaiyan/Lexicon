package platform

/**
 * WasmJs implementation of IAppVersionProvider
 * Returns a hardcoded version string for the web platform
 */
class WasmJsAppVersionProvider : IAppVersionProvider {
    override fun getVersion(): String = "1.0.0"
}
