package presentation.ui.components

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
import com.mmk.kmpauth.firebase.google.GoogleButtonUiContainerFirebase
import com.mmk.kmpauth.uihelper.google.GoogleButtonMode
import com.mmk.kmpauth.uihelper.google.GoogleSignInButton
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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

    GoogleButtonUiContainerFirebase(
        linkAccount = false,
        filterByAuthorizedAccounts = false,
        onResult = { result ->
            result.fold(
                onSuccess = { firebaseUser ->
                    if (firebaseUser != null) {
                        coroutineScope.launch {
                            try {
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
                            } catch (e: Exception) {
                                // getIdToken can throw FirebaseAuthException (e.g. network error,
                                // expired credential). Without this catch the coroutine crashes
                                // silently and neither onIdToken nor onError is ever called.
                                analyticsTracker.logEvent(
                                    "sign_in_google_error",
                                    mapOf(
                                        "stage" to "get_id_token_exception",
                                        "error_type" to (e::class.simpleName ?: "unknown"),
                                        "error_message" to (e.message ?: "no_message")
                                    )
                                )
                                analyticsTracker.logError(e, "google_get_id_token_exception")
                                onError()
                            }
                        }
                    } else {
                        analyticsTracker.logEvent(
                            "sign_in_google_error",
                            mapOf("stage" to "firebase_user_null")
                        )
                        onError()
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
        },
        modifier = modifier.height(56.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            GoogleSignInButton(
                modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth(),
                fontSize = 18.sp,
                mode = googleButtonStyle
            ) {
                this@GoogleButtonUiContainerFirebase.onClick()
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(20.dp)
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
}
