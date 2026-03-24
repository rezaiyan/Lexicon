package presentation.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.word.model.Word
import events.OnEvents
import feature.words.WordTagAssignmentViewModel
import feature.words.model.WordTagAssignmentEffect
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.assign_tags
import lexicon.resources.generated.resources.no_tags
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.ui.LocalSnackbarHostState
import theme.AppColors
import theme.Theme

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Theme.spacing.lg, vertical = Theme.spacing.medium)
    ) {
        Text(
            text = stringResource(Res.string.assign_tags),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = word.originalWord,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Theme.spacing.medium))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(Theme.spacing.sm))

        when {
            state.isLoading -> {
                Spacer(modifier = Modifier.height(Theme.spacing.lg))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(Theme.spacing.lg))
            }

            state.tags.isEmpty() -> {
                Spacer(modifier = Modifier.height(Theme.spacing.medium))
                Text(
                    text = stringResource(Res.string.no_tags),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(Theme.spacing.medium))
            }

            else -> {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)
                ) {
                    state.tags.forEach { tag ->
                        val selected = state.selectedTagIds.contains(tag.id)
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.toggleTag(tag.id) },
                            label = {
                                Text(
                                    text = if (tag.wordCount > 0) "${tag.name} · ${tag.wordCount}" else tag.name,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else null,
                            shape = RoundedCornerShape(Theme.shapes.pill),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.settingsTagManagerIcon.copy(alpha = 0.15f),
                                selectedLabelColor = AppColors.settingsTagManagerIcon,
                                selectedLeadingIconColor = AppColors.settingsTagManagerIcon
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = AppColors.settingsTagManagerIcon.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Theme.spacing.medium))

        Button(
            onClick = { viewModel.save() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving && !state.isLoading
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = Theme.dimensions.borderWidth
                )
            } else {
                Text(stringResource(Res.string.assign_tags))
            }
        }
    }
}
