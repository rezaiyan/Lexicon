package presentation.ui.components.imports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import components.sheet.SheetBadge
import components.sheet.SheetField
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import components.sheet.SheetSectionLabel
import components.sheet.SheetTonalButton
import domain.tag.model.Tag
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.add_word
import lexicon.resources.generated.resources.add_words_type_title
import lexicon.resources.generated.resources.added_count
import lexicon.resources.generated.resources.added_this_session
import lexicon.resources.generated.resources.done
import lexicon.resources.generated.resources.field_note
import lexicon.resources.generated.resources.field_optional
import lexicon.resources.generated.resources.field_word
import lexicon.resources.generated.resources.note_placeholder
import lexicon.resources.generated.resources.translation_label
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme
import utils.Language

@Composable
internal fun TextImportContent(
    textInputState: TextInputState,
    sourceLanguage: Language,
    targetLanguage: Language,
    tags: List<Tag>,
    selectedTagId: Long?,
    onTagSelected: (Long?) -> Unit,
    onCreateTag: () -> Unit,
    onChangeLanguage: () -> Unit,
    onWordChange: (String) -> Unit,
    onTranslationChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddWord: () -> Unit,
    onDone: () -> Unit,
) {
    val wordFocusRequester = remember { FocusRequester() }
    val translationFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }
    var previousWordsAdded by remember { mutableIntStateOf(textInputState.wordsAddedCount) }

    // Return focus to the word field after each successful add for rapid entry
    LaunchedEffect(textInputState.wordsAddedCount) {
        if (textInputState.wordsAddedCount > previousWordsAdded) {
            wordFocusRequester.requestFocus()
        }
        previousWordsAdded = textInputState.wordsAddedCount
    }

    SheetPage(
        title = stringResource(Res.string.add_words_type_title),
        headerAccessory = {
            LanguagePairChip(source = sourceLanguage, target = targetLanguage, onClick = onChangeLanguage)
        },
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
                SheetTonalButton(
                    text = stringResource(Res.string.done),
                    onClick = onDone,
                    modifier = Modifier.width(IntrinsicSize.Max),
                )
                SheetPrimaryButton(
                    text = stringResource(Res.string.add_word),
                    onClick = onAddWord,
                    enabled = textInputState.isAddEnabled,
                    isLoading = !textInputState.isEnabled,
                    icon = Icons.Default.Add,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
            SheetField(
                label = stringResource(Res.string.field_word),
                value = textInputState.word,
                onValueChange = onWordChange,
                enabled = textInputState.isEnabled,
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                imeAction = ImeAction.Next,
                onImeAction = { translationFocusRequester.requestFocus() },
                modifier = Modifier.focusRequester(wordFocusRequester),
            )
            SheetField(
                label = stringResource(Res.string.translation_label),
                value = textInputState.translation,
                onValueChange = onTranslationChange,
                enabled = textInputState.isEnabled,
                imeAction = ImeAction.Next,
                onImeAction = { descriptionFocusRequester.requestFocus() },
                modifier = Modifier.focusRequester(translationFocusRequester),
            )
            SheetField(
                label = stringResource(Res.string.field_note),
                optionalSuffix = stringResource(Res.string.field_optional),
                value = textInputState.description,
                onValueChange = onDescriptionChange,
                enabled = textInputState.isEnabled,
                placeholder = stringResource(Res.string.note_placeholder),
                imeAction = ImeAction.Done,
                onImeAction = { if (textInputState.isAddEnabled) onAddWord() },
                modifier = Modifier.focusRequester(descriptionFocusRequester),
            )
        }

        ErrorMessage(textInputState.errorMessage)

        TagSelectorRow(
            tags = tags,
            selectedTagId = selectedTagId,
            onTagSelected = onTagSelected,
            onCreateTag = onCreateTag,
        )

        RecentWords(textInputState)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentWords(textInputState: TextInputState) {
    AnimatedVisibility(
        visible = textInputState.recentWords.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column(
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SheetSectionLabel(stringResource(Res.string.added_this_session), Modifier.weight(1f))
                SheetBadge(
                    text = stringResource(Res.string.added_count, textInputState.wordsAddedCount),
                    containerColor = AppColors.secondary.copy(alpha = Theme.opacity.focus),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
            ) {
                textInputState.recentWords.forEach { added ->
                    val muted = MaterialTheme.colorScheme.onSurfaceVariant
                    Text(
                        text = buildAnnotatedString {
                            append(added.word)
                            withStyle(SpanStyle(color = muted)) { append(" · ${added.translation}") }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Theme.shapes.small + Theme.spacing.xxxs))
                            .background(MaterialTheme.colorScheme.surface)
                            .heightIn(min = Theme.spacing.xl)
                            .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xxs + Theme.spacing.xxxs),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ErrorMessage(errorMessage: String?) {
    AnimatedVisibility(
        visible = errorMessage != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Text(
            text = errorMessage.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Assertive }
                .clip(RoundedCornerShape(Theme.shapes.medium))
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
        )
    }
}
