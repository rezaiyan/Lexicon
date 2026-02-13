package config

actual object AppConfig {
    actual val VOKAB_BACKEND_URL: String = "https://${WasmBuildConfig.VOKAB_BACKEND_HOST}/api/v1"
    actual val GOOGLE_SERVER_CLIENT_ID: String = WasmBuildConfig.GOOGLE_SERVER_CLIENT_ID
    actual val REVENUECAT_ANDROID_KEY: String = WasmBuildConfig.REVENUECAT_ANDROID_KEY
    actual val REVENUECAT_IOS_KEY: String = WasmBuildConfig.REVENUECAT_IOS_KEY
}
