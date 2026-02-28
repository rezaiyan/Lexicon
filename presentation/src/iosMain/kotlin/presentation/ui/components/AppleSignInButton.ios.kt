package presentation.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.mmk.kmpauth.firebase.apple.AppleButtonUiContainer
import theme.Theme
import com.mmk.kmpauth.firebase.apple.AppleSignInRequestScope
import com.mmk.kmpauth.uihelper.apple.AppleSignInButton
import expects.AppleSignInHelper

@Composable
actual fun AppleSignInButton(
    onSignInSuccess: (idToken: String, fullName: String?, appleUserId: String) -> Unit,
    onSignInFailure: (error: String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier
) {
    val appleSignInHelper = remember { AppleSignInHelper() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Theme.dimensions.buttonHeight),
        contentAlignment = Alignment.Center
    ) {
        AppleButtonUiContainer(
            requestScopes = listOf(AppleSignInRequestScope.Email, AppleSignInRequestScope.FullName),
            onResult = {},
            linkAccount = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(Theme.dimensions.buttonHeight)
        ) {
            AppleSignInButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Theme.dimensions.buttonHeight)
                    .clip(RoundedCornerShape(Theme.shapes.medium))
            ) {
                appleSignInHelper.signIn(
                    onSuccess = { idToken, fullName, appleUserId ->
                        println(" [AppleSignInButton] Success - token obtained")
                        onSignInSuccess(idToken, fullName, appleUserId)
                    },
                    onFailure = { error ->
                        println(" [AppleSignInButton] Failure: $error")
                        onSignInFailure(error)
                    }
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
