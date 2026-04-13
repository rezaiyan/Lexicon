package core

actual fun getPlatformName(): String = "Android"

actual fun getAppVersion(): String {
    return try {
        val buildConfigClass = Class.forName("com.alirezaiyan.vokab.BuildConfig")
        val versionNameField = buildConfigClass.getField("VERSION_NAME")
        versionNameField.get(null) as String
    } catch (e: Exception) {
        "unknown"
    }
}

actual fun isDebugMode(): Boolean {
    return try {
        val buildConfigClass = Class.forName("com.alirezaiyan.vokab.BuildConfig")
        val debugField = buildConfigClass.getField("DEBUG")
        debugField.getBoolean(null)
    } catch (e: Exception) {
        false
    }
}

