package core

import platform.Foundation.NSBundle

actual fun getPlatformName(): String = "iOS"

actual fun getAppVersion(): String {
    return NSBundle.mainBundle.infoDictionary
        ?.get("CFBundleShortVersionString") as? String ?: "unknown"
}

actual fun isDebugMode(): Boolean {
    return NSBundle.mainBundle.bundlePath.contains("Debug") ||
           !NSBundle.mainBundle.bundlePath.contains("Release")
}

