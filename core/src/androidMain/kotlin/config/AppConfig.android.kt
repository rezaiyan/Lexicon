package config

import com.alirezaiyan.vokab.core.BuildConfig

actual object AppConfig {
    actual val VOKAB_BACKEND_URL: String = "https://${BuildConfig.VOKAB_BACKEND_HOST}/api/v1"
    actual val GOOGLE_SERVER_CLIENT_ID: String = BuildConfig.GOOGLE_SERVER_CLIENT_ID
    actual val REVENUECAT_ANDROID_KEY: String = BuildConfig.REVENUECAT_ANDROID_KEY
    actual val REVENUECAT_IOS_KEY: String = BuildConfig.REVENUECAT_IOS_KEY
}
