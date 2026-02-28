package expects

import platform.Foundation.NSLog
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.darwin.NSObjectProtocol

actual class AppleSignInHelper {
    
    private var successObserver: NSObjectProtocol? = null
    private var failureObserver: NSObjectProtocol? = null
    
    actual fun signIn(
        onSuccess: (idToken: String, fullName: String?, appleUserId: String) -> Unit,
        onFailure: (error: String) -> Unit
    ) {
        println(" [AppleSignIn.kt] Initiating Apple Sign In")
        NSLog(" [AppleSignIn.kt] Initiating Apple Sign In")
        
        successObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        failureObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        
        successObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = "AppleSignInSuccess",
            `object` = null,
            queue = null
        ) { notification: NSNotification? ->
            println(" [AppleSignIn.kt] Success notification received")
            NSLog(" [AppleSignIn.kt] Success notification received")
            
            notification?.userInfo?.let { userInfo ->
                val idToken = userInfo["idToken"] as? String
                val fullName = userInfo["fullName"] as? String
                val appleUserId = userInfo["appleUserId"] as? String
                
                if (idToken != null && appleUserId != null) {
                    onSuccess(idToken, fullName, appleUserId)
                } else {
                    onFailure("Invalid token or user ID received")
                }
            } ?: onFailure("No data in success notification")
            
            cleanup()
        }
        
        failureObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = "AppleSignInFailure",
            `object` = null,
            queue = null
        ) { notification: NSNotification? ->
            println(" [AppleSignIn.kt] Failure notification received")
            NSLog(" [AppleSignIn.kt] Failure notification received")
            
            val error = notification?.userInfo?.get("error") as? String ?: "Unknown error"
            onFailure(error)
            
            cleanup()
        }
        
        NSNotificationCenter.defaultCenter.postNotificationName(
            "StartAppleSignIn",
            `object` = null
        )
        
        println(" [AppleSignIn.kt] Posted StartAppleSignIn notification")
    }
    
    private fun cleanup() {
        successObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        failureObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        successObserver = null
        failureObserver = null
    }
}



