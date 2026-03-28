package feature.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import core.common.UiState
import events.OnEvents
import feature.profile.EditProfileEffect
import feature.profile.EditProfileViewModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.change_photo
import lexicon.resources.generated.resources.edit_profile
import lexicon.resources.generated.resources.save
import lexicon.resources.generated.resources.username
import lexicon.resources.generated.resources.username_description
import lexicon.resources.generated.resources.username_hint
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
fun EditProfileSheetContent(
    viewModel: EditProfileViewModel,
    onChangeAvatar: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.state()

    OnEvents(viewModel.effects) { effect ->
        when (effect) {
            is EditProfileEffect.ProfileSaved -> onSaved()
        }
    }

    val isSaving = state.saveState is UiState.Loading
    val saveError = (state.saveState as? UiState.Error)?.message
    val isBusy = isSaving || state.isUploadingAvatar

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Theme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))

        Text(
            text = stringResource(Res.string.edit_profile),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))

        Box(contentAlignment = Alignment.Center) {
            ProfileAvatar(
                name = state.name,
                email = state.email,
                profileImageUrl = state.profileImageUrl,
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 4.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isBusy) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.primary
                    )
                    .clickable(enabled = !isBusy) { onChangeAvatar() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = stringResource(Res.string.change_photo),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }

            if (state.isUploadingAvatar) {
                Box(
                    modifier = Modifier
                        .size(Theme.dimensions.profilePictureSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))

        OutlinedTextField(
            value = state.displayAlias,
            onValueChange = { viewModel.updateDisplayAlias(it) },
            label = { Text(stringResource(Res.string.username)) },
            placeholder = { Text(stringResource(Res.string.username_hint)) },
            supportingText = { Text(stringResource(Res.string.username_description)) },
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Theme.spacing.sm),
            isError = saveError != null,
        )

        if (saveError != null) {
            Spacer(modifier = Modifier.height(Theme.spacing.xxs))
            Text(
                text = saveError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(Theme.spacing.sm))
        OutlinedTextField(
            value = state.email,
            onValueChange = {},
            label = { Text("Email") },
            readOnly = true,
            enabled = false,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Theme.spacing.sm),
        )

        Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))
        Button(
            onClick = { viewModel.saveProfile() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isUploadingAvatar,
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(stringResource(Res.string.save))
            }
        }

        Spacer(modifier = Modifier.height(Theme.spacing.cardPadding))
    }
}
