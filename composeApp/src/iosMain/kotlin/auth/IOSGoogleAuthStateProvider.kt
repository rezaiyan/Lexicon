package auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

/**
 * iOS implementation of Google authentication state provider
 */
class IOSGoogleAuthStateProvider : IGoogleAuthStateProvider {
    override fun isSignedInWithGoogle(): Boolean = Firebase.auth.currentUser != null
    
    override suspend fun getSilentGoogleIdToken(): String? = 
        Firebase.auth.currentUser?.getIdToken(false)
    
    override suspend fun signOutFromGoogle() {
        Firebase.auth.signOut()
    }
}


