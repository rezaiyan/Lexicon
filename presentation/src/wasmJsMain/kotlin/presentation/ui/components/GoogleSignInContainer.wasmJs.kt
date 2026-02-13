package presentation.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Web stub for Google Sign-In.
 * TODO: Integrate Firebase JS SDK signInWithPopup for real Google auth on web.
 */
@Composable
actual fun GoogleSignInContainer(
    onIdToken: suspend (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    Button(
        onClick = {
            coroutineScope.launch {
                // TODO: Implement Firebase JS SDK signInWithPopup
                // val auth = getAuth(firebaseApp)
                // val result = signInWithPopup(auth, GoogleAuthProvider())
                // val idToken = result.user.getIdToken()
                // onIdToken(idToken)
            }
        },
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(if (isLoading) "Signing in..." else "Sign in with Google")
    }
}
