package feature.profile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import components.sheet.SheetField
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import events.OnEvents
import feature.profile.EditProfileEffect
import feature.profile.EditProfileViewModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.edit_profile
import lexicon.resources.generated.resources.email_label
import lexicon.resources.generated.resources.save
import lexicon.resources.generated.resources.username
import lexicon.resources.generated.resources.username_description
import lexicon.resources.generated.resources.username_hint
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
fun EditProfileSheetContent(
    viewModel: EditProfileViewModel,
    onSaved: () -> Unit,
) {
    val state by viewModel.state()

    OnEvents(viewModel.effects) { effect ->
        when (effect) {
            is EditProfileEffect.ProfileSaved -> onSaved()
        }
    }

    val isSaving = state.isSaving
    val saveError = state.errorMessage

    SheetPage(
        title = stringResource(Res.string.edit_profile),
        footer = {
            SheetPrimaryButton(
                text = stringResource(Res.string.save),
                onClick = viewModel::saveProfile,
                isLoading = isSaving,
            )
        },
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            ProfileAvatar(name = state.name, email = state.email)
        }

        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
            SheetField(
                label = stringResource(Res.string.username),
                value = state.displayAlias,
                onValueChange = viewModel::updateDisplayAlias,
                placeholder = stringResource(Res.string.username_hint),
                supportingText = saveError ?: stringResource(Res.string.username_description),
                isError = saveError != null,
                enabled = !isSaving,
                imeAction = ImeAction.Done,
                onImeAction = { if (!isSaving) viewModel.saveProfile() },
            )
            SheetField(
                label = stringResource(Res.string.email_label),
                value = state.email,
                onValueChange = {},
                readOnly = true,
            )
        }
    }
}
