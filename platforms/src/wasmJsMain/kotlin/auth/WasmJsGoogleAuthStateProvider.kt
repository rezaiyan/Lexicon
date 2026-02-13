package auth

class WasmJsGoogleAuthStateProvider : IGoogleAuthStateProvider {

    override fun isSignedInWithGoogle(): Boolean =
        jsIsSignedIn()?.toString() == "true"

    override suspend fun getSilentGoogleIdToken(): String? =
        awaitIdToken(forceRefresh = false)

    override suspend fun signOutFromGoogle() {
        awaitSignOut()
    }
}
