package feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import feature.profile.EditProfileEffect
import feature.profile.EditProfileViewModel
import components.scaffold.LexiconColumn
import feature.profile.ui.components.ProfileAvatar
import overlay.OverlayHost
import overlay.bottomsheet.showFullscreenBottomSheet
import theme.Theme
import utils.rememberImagePickerLauncher
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.edit_profile
import lexicon.resources.generated.resources.username
import lexicon.resources.generated.resources.username_hint
import lexicon.resources.generated.resources.username_description
import lexicon.resources.generated.resources.save
import lexicon.resources.generated.resources.change_photo
import lexicon.resources.generated.resources.remove_photo
import lexicon.resources.generated.resources.choose_from_gallery
import lexicon.resources.generated.resources.profile_updated

@Composable
fun EditProfileScreen(
    snackbarHostState: SnackbarHostState,
    overlayHost: OverlayHost,
    onNavigateBack: () -> Unit
) {
    val viewModel = koinViewModel<EditProfileViewModel>()
    val state by viewModel.state()
    val profileUpdatedMessage = stringResource(Res.string.profile_updated)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is EditProfileEffect.ProfileSaved -> {
                    snackbarHostState.showSnackbar(profileUpdatedMessage)
                    onNavigateBack()
                }
            }
        }
    }

    val imagePickerLauncher = rememberImagePickerLauncher { bytes ->
        if (bytes != null) {
            viewModel.uploadAvatar(bytes, "image/jpeg")
        }
    }

    LexiconColumn(
        title = stringResource(Res.string.edit_profile),
        showNavigationIcon = true,
        onNavigationClick = onNavigateBack,
        scrollable = true
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))

            // Avatar with edit overlay
            Box(contentAlignment = Alignment.Center) {
                ProfileAvatar(
                    name = state.name,
                    email = state.email,
                    profileImageUrl = state.profileImageUrl
                )

                // Camera icon overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 4.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            overlayHost.showFullscreenBottomSheet(tag = "avatar-options") { navigator ->
                                AvatarOptionsSheet(
                                    hasExistingAvatar = state.profileImageUrl != null,
                                    onChooseFromGallery = {
                                        navigator.dismiss()
                                        imagePickerLauncher()
                                    },
                                    onRemovePhoto = {
                                        navigator.dismiss()
                                        viewModel.deleteAvatar()
                                    }
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = stringResource(Res.string.change_photo),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                if (state.isUploadingAvatar) {
                    Box(
                        modifier = Modifier
                            .size(Theme.dimensions.profilePictureSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))

            // Username field
            OutlinedTextField(
                value = state.displayAlias,
                onValueChange = { viewModel.updateDisplayAlias(it) },
                label = { Text(stringResource(Res.string.username)) },
                placeholder = { Text(stringResource(Res.string.username_hint)) },
                supportingText = { Text(stringResource(Res.string.username_description)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Theme.spacing.sm),
                isError = state.errorMessage != null
            )

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(Theme.spacing.xxs))
                Text(
                    text = state.errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Read-only email
            Spacer(modifier = Modifier.height(Theme.spacing.sm))
            OutlinedTextField(
                value = state.email,
                onValueChange = {},
                label = { Text("Email") },
                readOnly = true,
                enabled = false,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Theme.spacing.sm)
            )

            // Save button
            Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))
            Button(
                onClick = { viewModel.saveProfile() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(Res.string.save))
                }
            }

            Spacer(modifier = Modifier.height(Theme.spacing.cardPadding))
        }
    }
}

@Composable
private fun AvatarOptionsSheet(
    hasExistingAvatar: Boolean,
    onChooseFromGallery: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Theme.spacing.sectionSpacing)
    ) {
        Text(
            text = stringResource(Res.string.change_photo),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(Theme.spacing.sectionSpacing)
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        Text(
            text = stringResource(Res.string.choose_from_gallery),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onChooseFromGallery)
                .padding(
                    horizontal = Theme.spacing.sectionSpacing,
                    vertical = Theme.spacing.cardPadding
                )
        )

        if (hasExistingAvatar) {
            Text(
                text = stringResource(Res.string.remove_photo),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRemovePhoto)
                    .padding(
                        horizontal = Theme.spacing.sectionSpacing,
                        vertical = Theme.spacing.cardPadding
                    )
            )
        }
    }
}
