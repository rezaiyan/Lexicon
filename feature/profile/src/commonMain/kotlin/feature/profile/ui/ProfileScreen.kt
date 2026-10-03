package feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import core.common.UiState
import feature.profile.ProfileViewModel
import feature.profile.model.ProfileUiData
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    snackbarHostState: SnackbarHostState,
    onEditProfile: () -> Unit,
    onDeleteAccount: () -> Unit,
    onLogout: () -> Unit,
    onOpenSubscription: () -> Unit,
) {
    val profileViewModel = koinViewModel<ProfileViewModel>()

    LaunchedEffect(Unit) { profileViewModel.refreshProfileStats() }

    val uiState by profileViewModel.state()

    LaunchedEffect(uiState) {
        if (uiState is UiState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as UiState.Error).message,
                withDismissAction = true
            )
            profileViewModel.clearError()
        }
    }

    val profileData = (uiState as? UiState.Loaded<ProfileUiData>)?.value
    val isLoggedIn = profileData?.userInfo != null
    val isLoading = uiState is UiState.Loading

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                isLoggedIn -> {
                    ProfileContent(
                        profileData = profileData,
                        onEditProfile = onEditProfile,
                        onDeleteAccount = onDeleteAccount,
                        onLogout = onLogout,
                        onOpenSubscription = onOpenSubscription,
                    )
                }
            }

            if (isLoggedIn && isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
