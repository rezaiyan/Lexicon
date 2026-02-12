package config

import platform.Foundation.NSBundle

private fun requireConfigValue(key: String): String {
    val rawValue = NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String
        ?: error("Missing '$key' in Info.plist. Configure it via Config.private.xcconfig.")
    return rawValue
        .replace("\\/", "/")
        .trim('"')
}

actual object AppConfig {
    private fun buildUrl(host: String): String =
        "https://${host.removePrefix("https://").removePrefix("http://").trimEnd('/')}/api/v1"
    actual val VOKAB_BACKEND_URL: String = buildUrl(requireConfigValue("VokabBackendHost"))
    actual val GOOGLE_SERVER_CLIENT_ID: String = requireConfigValue("GIDServerClientID")
    actual val REVENUECAT_ANDROID_KEY: String = requireConfigValue("REVENUECAT_ANDROID_KEY")
    actual val REVENUECAT_IOS_KEY: String = requireConfigValue("REVENUECAT_IOS_KEY")
}
