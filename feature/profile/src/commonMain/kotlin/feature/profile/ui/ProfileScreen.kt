package feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.more_options
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import theme.Theme

@Composable
fun ProfileScreen(
    snackbarHostState: SnackbarHostState,
    onMoreOptions: () -> Unit,
    onLogout: () -> Unit,
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
        if (isLoggedIn) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onMoreOptions) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(Res.string.more_options),
                        modifier = Modifier.size(Theme.dimensions.iconSize),
                    )
                }
            }
        }

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
                        onLogout = onLogout,
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
