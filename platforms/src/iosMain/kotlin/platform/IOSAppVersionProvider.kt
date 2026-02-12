package platform

import platform.Foundation.NSBundle

/**
 * iOS implementation of IAppVersionProvider
 * Retrieves version information from the app's Info.plist (CFBundleShortVersionString)
 */
class IOSAppVersionProvider : IAppVersionProvider {
    
    override fun getVersion(): String {
        return try {
            val bundle = NSBundle.mainBundle
            val version = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
            version ?: DEFAULT_VERSION
        } catch (e: Exception) {
            DEFAULT_VERSION
        }
    }
    
    companion object {
        private const val DEFAULT_VERSION = "1.0.0"
    }
}

