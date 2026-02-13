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
import com.mmk.kmpauth.firebase.google.GoogleButtonUiContainerFirebase
import com.mmk.kmpauth.uihelper.google.GoogleButtonMode
import com.mmk.kmpauth.uihelper.google.GoogleSignInButton
import kotlinx.coroutines.launch

@Composable
actual fun GoogleSignInContainer(
    onIdToken: suspend (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()
    val googleButtonStyle = if (isDarkTheme) GoogleButtonMode.Light else GoogleButtonMode.Dark

    GoogleButtonUiContainerFirebase(
        onResult = { result ->
            result.onSuccess { firebaseUser ->
                if (firebaseUser != null) {
                    coroutineScope.launch {
                        val idToken = firebaseUser.getIdToken(false)
                        if (idToken != null) {
                            onIdToken(idToken)
                        }
                    }
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
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
