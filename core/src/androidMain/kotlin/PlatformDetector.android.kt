package core

actual fun getPlatformName(): String = "Android"

actual fun isDebugMode(): Boolean {
    return try {
        val buildConfigClass = Class.forName("com.alirezaiyan.vokab.BuildConfig")
        val debugField = buildConfigClass.getField("DEBUG")
        debugField.getBoolean(null) as Boolean
    } catch (e: Exception) {
        false
    }
}

