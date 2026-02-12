package core

actual fun getPlatformName(): String = "iOS"

actual fun isDebugMode(): Boolean {
    return platform.Foundation.NSBundle.mainBundle.bundlePath.contains("Debug") ||
           !platform.Foundation.NSBundle.mainBundle.bundlePath.contains("Release")
}

