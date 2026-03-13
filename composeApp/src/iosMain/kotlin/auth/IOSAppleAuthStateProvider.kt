package auth

import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of Apple Auth State Provider
 * Checks stored Apple user identifier for credential state
 */
class IOSAppleAuthStateProvider : IAppleAuthStateProvider {
    
    companion object {
        private const val APPLE_USER_ID_KEY = "appleUserIdentifier"
    }
    
    override suspend fun isSignedInWithApple(): Boolean {
        val userId = getAppleUserIdentifier()
        return userId != null
    }
    
    override suspend fun getAppleUserIdentifier(): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(APPLE_USER_ID_KEY)
    }
    
    override suspend fun signOutFromApple() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(APPLE_USER_ID_KEY)
        NSUserDefaults.standardUserDefaults.synchronize()
    }
}