package auth

import kotlinx.coroutines.delay

// --- Firebase initialization ---

fun jsFirebaseInit(apiKey: String, authDomain: String, projectId: String): JsAny? =
    js("window._lexiconFirebase.init(apiKey, authDomain, projectId)")

// --- Google Sign-In ---

fun jsStartGoogleSignIn(): JsAny? =
    js("window._lexiconFirebase.signInWithGoogle()")

private fun jsSignInPending(): JsAny? =
    js("window._lexiconFirebase._signInState.pending")

private fun jsSignInToken(): JsAny? =
    js("window._lexiconFirebase._signInState.token")

private fun jsSignInError(): JsAny? =
    js("window._lexiconFirebase._signInState.error")

suspend fun awaitGoogleSignIn(): String {
    jsStartGoogleSignIn()
    while (jsSignInPending()?.toString() != "false") {
        delay(50)
    }
    val error = jsSignInError()
    if (error != null) throw RuntimeException(error.toString())
    return jsSignInToken()?.toString() ?: throw RuntimeException("No token received")
}

// --- Redirect result (mobile sign-in) ---

private fun jsRedirectPending(): JsAny? =
    js("window._lexiconFirebase._redirectResult.pending")

private fun jsRedirectToken(): JsAny? =
    js("window._lexiconFirebase._redirectResult.token")

private fun jsRedirectError(): JsAny? =
    js("window._lexiconFirebase._redirectResult.error")

suspend fun awaitRedirectResult(): String? {
    while (jsRedirectPending()?.toString() != "false") {
        delay(50)
    }
    if (jsRedirectError() != null) return null
    return jsRedirectToken()?.toString()
}

// --- ID token retrieval ---

fun jsStartGetIdToken(forceRefresh: Boolean): JsAny? =
    js("window._lexiconFirebase.getCurrentUserIdToken(forceRefresh)")

private fun jsTokenPending(): JsAny? =
    js("window._lexiconFirebase._tokenState.pending")

private fun jsTokenValue(): JsAny? =
    js("window._lexiconFirebase._tokenState.token")

private fun jsTokenError(): JsAny? =
    js("window._lexiconFirebase._tokenState.error")

suspend fun awaitIdToken(forceRefresh: Boolean): String? {
    jsStartGetIdToken(forceRefresh)
    while (jsTokenPending()?.toString() != "false") {
        delay(50)
    }
    val error = jsTokenError()
    if (error != null) return null
    return jsTokenValue()?.toString()
}

// --- Auth state ---

fun jsIsSignedIn(): JsAny? =
    js("window._lexiconFirebase.isSignedIn()")

// --- Sign out ---

fun jsStartSignOut(): JsAny? =
    js("window._lexiconFirebase.signOut()")

private fun jsSignOutPending(): JsAny? =
    js("window._lexiconFirebase._signOutState.pending")

suspend fun awaitSignOut() {
    jsStartSignOut()
    while (jsSignOutPending()?.toString() != "false") {
        delay(50)
    }
}
