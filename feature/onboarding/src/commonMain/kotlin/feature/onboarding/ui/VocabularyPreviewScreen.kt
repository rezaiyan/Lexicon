package feature.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import components.sheet.SheetPrimaryButton
import components.sheet.SheetTextButton
import components.sheet.SheetTitle
import domain.onboarding.model.SuggestedVocabulary
import feature.onboarding.model.VocabularyPreviewUiState
import feature.onboarding.ui.components.OnboardingFooter
import feature.onboarding.ui.components.onboardingContentWidth
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.onboarding_add_words
import lexicon.resources.generated.resources.onboarding_start_empty
import lexicon.resources.generated.resources.onboarding_starter_eyebrow
import lexicon.resources.generated.resources.onboarding_starter_highlight
import lexicon.resources.generated.resources.onboarding_starter_subtitle
import lexicon.resources.generated.resources.onboarding_starter_title
import lexicon.resources.generated.resources.onboarding_word_count
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/** Starter words generated from the onboarding answers. */
@Composable
fun VocabularyPreviewScreen(
    state: VocabularyPreviewUiState,
    onAddWords: () -> Unit,
    onStartEmpty: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StarterHeader(state = state, modifier = Modifier.onboardingContentWidth())
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.onboardingContentWidth(),
                contentPadding = PaddingValues(bottom = Theme.spacing.md),
            ) {
                itemsIndexed(state.words, key = { index, word -> "$index-${word.originalWord}" }) { index, word ->
                    StarterWordRow(word = word, showDivider = index < state.words.lastIndex)
                }
            }
        }
        OnboardingFooter {
            SheetPrimaryButton(
                text = stringResource(Res.string.onboarding_add_words, state.words.size),
                onClick = onAddWords,
                enabled = state.words.isNotEmpty(),
            )
            SheetTextButton(text = stringResource(Res.string.onboarding_start_empty), onClick = onStartEmpty)
        }
    }
}

@Composable
private fun StarterHeader(state: VocabularyPreviewUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = Theme.spacing.xxxl, bottom = Theme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.textGap)) {
            Text(
                stringResource(Res.string.onboarding_starter_eyebrow),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            SheetTitle(
                title = stringResource(Res.string.onboarding_starter_title),
                highlight = stringResource(Res.string.onboarding_starter_highlight),
            )
            Text(
                stringResource(Res.string.onboarding_starter_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val learning = state.learningLanguage
        val native = state.nativeLanguage
        if (learning != null && native != null) {
            InfoChip("${learning.displayName} → ${native.displayName}")
        }
        Text(
            stringResource(Res.string.onboarding_word_count, state.words.size),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = Theme.spacing.xs),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay))
    }
}

@Composable
private fun InfoChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(Theme.shapes.pill))
            .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xxs),
    )
}

@Composable
private fun StarterWordRow(word: SuggestedVocabulary, showDivider: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Theme.dimensions.inputFieldHeight)
                .padding(vertical = Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
                Text(
                    word.originalWord,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    word.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay))
        }
    }
}
