package presentation.ui.components.imports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.add_a_word
import lexicon.resources.generated.resources.add_word
import lexicon.resources.generated.resources.add_word_description
import lexicon.resources.generated.resources.description_optional
import lexicon.resources.generated.resources.original_language
import lexicon.resources.generated.resources.original_word
import lexicon.resources.generated.resources.translation_label
import lexicon.resources.generated.resources.translation_language
import lexicon.resources.generated.resources.word_added_count_singular
import lexicon.resources.generated.resources.words_added_count
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.Language

@Composable
internal fun TextImportContent(
    textInputState: TextInputState,
    sourceLanguage: Language,
    targetLanguage: Language,
    onWordChange: (String) -> Unit,
    onTranslationChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddWord: () -> Unit,
    onShowSourceLanguage: () -> Unit,
    onShowTargetLanguage: () -> Unit,
) {
    val isAddEnabled by derivedStateOf { textInputState.isAddEnabled }
    val wordFocusRequester = remember { FocusRequester() }
    val translationFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }
    var previousWordsAdded by remember { mutableStateOf(textInputState.wordsAddedCount) }

    LaunchedEffect(textInputState.wordsAddedCount) {
        if (textInputState.wordsAddedCount > previousWordsAdded) {
            wordFocusRequester.requestFocus()
        }
        previousWordsAdded = textInputState.wordsAddedCount
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)
        ) {
            ImportInfoCard(
                title = stringResource(Res.string.add_a_word),
                description = stringResource(Res.string.add_word_description),
                icon = Icons.Filled.Edit,
            )

            LanguageSelectorRow(
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                onShowSourceLanguage = onShowSourceLanguage,
                onShowTargetLanguage = onShowTargetLanguage,
            )

            WordInputFields(
                textInputState = textInputState,
                wordFocusRequester = wordFocusRequester,
                translationFocusRequester = translationFocusRequester,
                descriptionFocusRequester = descriptionFocusRequester,
                isAddEnabled = isAddEnabled,
                onWordChange = onWordChange,
                onTranslationChange = onTranslationChange,
                onDescriptionChange = onDescriptionChange,
                onAddWord = onAddWord,
            )

            WordsAddedCounter(textInputState)

            ErrorMessage(textInputState.errorMessage)
        }

        Spacer(modifier = Modifier.height(Theme.spacing.sm))

        Button(
            onClick = onAddWord,
            enabled = isAddEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(Theme.dimensions.buttonHeight),
            shape = RoundedCornerShape(Theme.shapes.medium),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(Theme.dimensions.iconSizeMedium))
            Spacer(modifier = Modifier.width(Theme.spacing.xs))
            Text(
                stringResource(Res.string.add_word),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LanguageSelectorRow(
    sourceLanguage: Language,
    targetLanguage: Language,
    onShowSourceLanguage: () -> Unit,
    onShowTargetLanguage: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)
    ) {
        CompactLanguageSelector(
            label = stringResource(Res.string.original_language),
            language = sourceLanguage,
            onClick = onShowSourceLanguage,
            modifier = Modifier.weight(1f)
        )
        CompactLanguageSelector(
            label = stringResource(Res.string.translation_language),
            language = targetLanguage,
            onClick = onShowTargetLanguage,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompactLanguageSelector(
    label: String,
    language: Language,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            Theme.dimensions.hairlineThickness,
            MaterialTheme.colorScheme.outlineVariant
        ),
        shape = RoundedCornerShape(Theme.shapes.medium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xs)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WordInputFields(
    textInputState: TextInputState,
    wordFocusRequester: FocusRequester,
    translationFocusRequester: FocusRequester,
    descriptionFocusRequester: FocusRequester,
    isAddEnabled: Boolean,
    onWordChange: (String) -> Unit,
    onTranslationChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddWord: () -> Unit,
) {
    val fieldShape = RoundedCornerShape(Theme.shapes.medium)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    )

    OutlinedTextField(
        value = textInputState.word,
        onValueChange = onWordChange,
        label = { Text(stringResource(Res.string.original_word)) },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(wordFocusRequester),
        singleLine = true,
        enabled = textInputState.isEnabled,
        shape = fieldShape,
        colors = fieldColors,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(
            onNext = { translationFocusRequester.requestFocus() }
        )
    )

    OutlinedTextField(
        value = textInputState.translation,
        onValueChange = onTranslationChange,
        label = { Text(stringResource(Res.string.translation_label)) },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(translationFocusRequester),
        singleLine = true,
        enabled = textInputState.isEnabled,
        shape = fieldShape,
        colors = fieldColors,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(
            onNext = { descriptionFocusRequester.requestFocus() }
        )
    )

    OutlinedTextField(
        value = textInputState.description,
        onValueChange = onDescriptionChange,
        label = { Text(stringResource(Res.string.description_optional)) },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(descriptionFocusRequester),
        singleLine = true,
        enabled = textInputState.isEnabled,
        shape = fieldShape,
        colors = fieldColors,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { if (isAddEnabled) onAddWord() }
        )
    )
}

@Composable
private fun WordsAddedCounter(textInputState: TextInputState) {
    AnimatedVisibility(
        visible = textInputState.wordsAddedCount > 0,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(Theme.shapes.medium)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = textInputState.showSuccessIndicator,
                    enter = fadeIn() + scaleIn(initialScale = 0.5f),
                    exit = fadeOut()
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(Theme.dimensions.iconSizeMedium)
                            .padding(end = Theme.spacing.xxs)
                    )
                }
                val count = textInputState.wordsAddedCount
                val displayText = if (count == 1) {
                    stringResource(Res.string.word_added_count_singular)
                } else {
                    val countText = stringResource(Res.string.words_added_count)
                    val placeholder = "%1" + '$' + "d"
                    countText.replace(placeholder, count.toString())
                }
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ErrorMessage(errorMessage: String?) {
    AnimatedVisibility(
        visible = errorMessage != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(Theme.shapes.medium)
        ) {
            Text(
                text = errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(
                    horizontal = Theme.spacing.md,
                    vertical = Theme.spacing.sm
                )
            )
        }
    }
}
