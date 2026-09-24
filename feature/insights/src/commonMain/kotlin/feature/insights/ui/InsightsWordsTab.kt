package feature.insights.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import components.ErrorScreen
import components.LoadingScreen
import components.Pill
import core.common.onError
import core.common.onLoaded
import core.common.onLoading
import domain.analytics.model.WordDifficulty
import feature.insights.InsightsState
import kotlin.math.roundToInt
import kotlinx.datetime.minus
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.insights_loading_words
import lexicon.resources.generated.resources.insights_study_these_words
import lexicon.resources.generated.resources.insights_most_difficult_words
import lexicon.resources.generated.resources.insights_reviews_format
import org.jetbrains.compose.resources.stringResource
import theme.Theme

// region Words

@Composable
internal fun WordsTab(state: InsightsState, onStudyDifficultWords: () -> Unit = {}) {
    state.difficultWords
        .onLoading { LoadingScreen(message = stringResource(Res.string.insights_loading_words)) }
        .onError { msg, _ -> ErrorScreen(message = msg) }
        .onLoaded { words ->
            if (words.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
                    SectionLabel(stringResource(Res.string.insights_most_difficult_words))
                    words.forEachIndexed { index, word ->
                        DifficultWordRow(word)
                        if (index < words.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = Theme.spacing.xl),
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = Theme.dimensions.hairlineThickness,
                            )
                        }
                    }
                    Button(
                        onClick = onStudyDifficultWords,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.insights_study_these_words))
                    }
                }
            }
        }
}

@Composable
private fun DifficultWordRow(word: WordDifficulty) {
    val errorPercent = (word.errorRate * 100).roundToInt()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Theme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                word.wordText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                word.wordTranslation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(Theme.spacing.sm))
        Column(horizontalAlignment = Alignment.End) {
            // Error rate as styled pill using design-system Pill component
            Pill(
                text = "$errorPercent% error",
                color = MaterialTheme.colorScheme.error,
                backgroundColor = MaterialTheme.colorScheme.errorContainer,
                height = 22.dp,
                cornerRadius = Theme.shapes.extraSmall,
            )
            Spacer(modifier = Modifier.height(Theme.spacing.xxs))
            Text(
                stringResource(Res.string.insights_reviews_format, word.totalReviews),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// endregion
