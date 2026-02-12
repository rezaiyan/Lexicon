package config

/**
 * Application configuration that varies by platform
 * API keys and secrets are kept in local.properties (gitignored)
 */
expect object AppConfig {
    /**
     * Vokab Backend API URL
     * Stored in local.properties as: vokab.backend.url=http://localhost:8080/api/v1
     * For Android emulator, use: http://10.0.2.2:8080/api/v1
     */
    val VOKAB_BACKEND_URL: String
    val GOOGLE_SERVER_CLIENT_ID: String
    val REVENUECAT_ANDROID_KEY: String
    val REVENUECAT_IOS_KEY: String
}

