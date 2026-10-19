package feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import analytics.IAnalyticsTracker
import com.mmk.kmpauth.google.rememberGoogleSignInState
import com.mmk.kmpauth.uihelper.google.GoogleButtonMode
import com.mmk.kmpauth.uihelper.google.GoogleSignInButton
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import theme.Theme

@Composable
actual fun GoogleSignInContainer(
    onIdToken: suspend (String) -> Unit,
    onError: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()
    val googleButtonStyle = if (isDarkTheme) GoogleButtonMode.Light else GoogleButtonMode.Dark
    val analyticsTracker = koinInject<IAnalyticsTracker>()

    // kmpauth 3 only does the Google step. The backend verifies Firebase ID tokens,
    // so we exchange the Google ID token for a Firebase session ourselves.
    val googleSignIn = rememberGoogleSignInState(
        filterByAuthorizedAccounts = false,
        onResult = { result ->
            result.fold(
                onSuccess = { googleUser ->
                    coroutineScope.launch {
                        try {
                            val credential = GoogleAuthProvider.credential(
                                idToken = googleUser.idToken,
                                accessToken = googleUser.accessToken
                            )
                            val firebaseUser = Firebase.auth.signInWithCredential(credential).user
                            if (firebaseUser == null) {
                                analyticsTracker.logEvent(
                                    "sign_in_google_error",
                                    mapOf("stage" to "firebase_user_null")
                                )
                                onError()
                                return@launch
                            }
                            // Try cached token first; fall back to force-refresh if null
                            val idToken = firebaseUser.getIdToken(false)
                                ?: firebaseUser.getIdToken(true)
                            if (idToken != null) {
                                onIdToken(idToken)
                            } else {
                                analyticsTracker.logEvent(
                                    "sign_in_google_error",
                                    mapOf("stage" to "get_id_token_null")
                                )
                                onError()
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            // signInWithCredential / getIdToken can throw FirebaseAuthException
                            // (e.g. network error, expired credential). Without this catch the
                            // coroutine crashes silently and neither onIdToken nor onError is called.
                            analyticsTracker.logEvent(
                                "sign_in_google_error",
                                mapOf(
                                    "stage" to "firebase_sign_in_exception",
                                    "error_type" to (e::class.simpleName ?: "unknown"),
                                    "error_message" to (e.message ?: "no_message")
                                )
                            )
                            analyticsTracker.logError(e, "google_firebase_sign_in_exception")
                            onError()
                        }
                    }
                },
                onFailure = { error ->
                    analyticsTracker.logEvent(
                        "sign_in_google_error",
                        mapOf(
                            "stage" to "kmpauth_failure",
                            "error_type" to (error::class.simpleName ?: "unknown"),
                            "error_message" to (error.message ?: "no_message")
                        )
                    )
                    onError()
                }
            )
        }
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Theme.dimensions.buttonHeight)
    ) {
        GoogleSignInButton(
            modifier = Modifier
                .height(Theme.dimensions.buttonHeight)
                .fillMaxWidth(),
            fontSize = 16.sp,
            shape = RoundedCornerShape(Theme.shapes.pill),
            mode = googleButtonStyle
        ) {
            googleSignIn.launch()
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(Theme.shapes.pill)
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
