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
        val isSignedIn = userId != null
        println("🍎 [IOSAppleAuth] Checking sign-in state: $isSignedIn (userId: $userId)")
        return isSignedIn
    }
    
    override suspend fun getAppleUserIdentifier(): String? {
        val userId = NSUserDefaults.standardUserDefaults.stringForKey(APPLE_USER_ID_KEY)
        println("🍎 [IOSAppleAuth] Retrieved Apple user ID: ${if (userId != null) "exists" else "null"}")
        return userId
    }
    
    override suspend fun signOutFromApple() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(APPLE_USER_ID_KEY)
        NSUserDefaults.standardUserDefaults.synchronize()
        println("🍎 [IOSAppleAuth] Signed out from Apple (cleared user ID)")
    }
}