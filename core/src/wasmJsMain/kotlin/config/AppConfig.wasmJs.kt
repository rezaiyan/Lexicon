package config

/**
 * WasmJs implementation of AppConfig
 * Uses hardcoded defaults for web platform
 */
actual object AppConfig {
    actual val VOKAB_BACKEND_URL: String = "http://localhost:8080/api/v1"
    actual val GOOGLE_SERVER_CLIENT_ID: String = ""
    actual val REVENUECAT_ANDROID_KEY: String = ""
    actual val REVENUECAT_IOS_KEY: String = ""
}
