package presentation.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import components.sheet.SheetCheckboxRow
import components.sheet.SheetGroup
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import domain.tag.model.Tag
import domain.word.model.Word
import events.OnEvents
import feature.words.WordTagAssignmentViewModel
import feature.words.model.WordTagAssignmentEffect
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.assign_tags
import lexicon.resources.generated.resources.no_tags
import lexicon.resources.generated.resources.word_count_label
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.ui.LocalSnackbarHostState

@Composable
internal fun TagAssignmentSheetContent(
    word: Word,
    onDismiss: () -> Unit,
) {
    val viewModel = koinViewModel<WordTagAssignmentViewModel>()
    val state by viewModel.state()
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(word.id) {
        viewModel.initialize(word.id, word.tagIds)
    }

    OnEvents(viewModel.effects) { effect ->
        when (effect) {
            is WordTagAssignmentEffect.TagsAssigned -> onDismiss()
            is WordTagAssignmentEffect.Error -> snackbarHostState.showSnackbar(effect.message)
        }
    }

    TagChecklistContent(
        title = stringResource(Res.string.assign_tags),
        subtitle = word.originalWord,
        tags = state.tags,
        selectedTagIds = state.selectedTagIds,
        onToggle = viewModel::toggleTag,
        onApply = viewModel::save,
        isLoading = state.isLoading,
        isSaving = state.isSaving,
    )
}

/** Multi-select tag list shared by single-word and batch assignment. */
@Composable
internal fun TagChecklistContent(
    title: String,
    subtitle: String?,
    tags: List<Tag>,
    selectedTagIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onApply: () -> Unit,
    isLoading: Boolean = false,
    isSaving: Boolean = false,
    onClose: (() -> Unit)? = null,
) {
    SheetPage(
        title = title,
        subtitle = subtitle,
        onClose = onClose,
        footer = {
            SheetPrimaryButton(
                text = stringResource(Res.string.assign_tags),
                onClick = onApply,
                enabled = !isLoading && tags.isNotEmpty(),
                isLoading = isSaving,
            )
        },
    ) {
        when {
            isLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            tags.isEmpty() -> Text(
                text = stringResource(Res.string.no_tags),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> SheetGroup {
                tags.forEachIndexed { index, tag ->
                    SheetCheckboxRow(
                        title = tag.name,
                        subtitle = stringResource(Res.string.word_count_label, tag.wordCount.toInt()),
                        icon = Icons.AutoMirrored.Filled.Label,
                        checked = tag.id in selectedTagIds,
                        onCheckedChange = { onToggle(tag.id) },
                        showDivider = index < tags.lastIndex,
                    )
                }
            }
        }
    }
}
