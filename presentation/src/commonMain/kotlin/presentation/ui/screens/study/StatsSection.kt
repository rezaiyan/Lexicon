package presentation.ui.screens.study

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import domain.word.model.ProgressStats
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.cards_due_for_review
import vokab.resources.generated.resources.total_words

@Composable
fun StatsSection(
    modifier: Modifier = Modifier,
    stats: ProgressStats,
    onStartReview: () -> Unit
) {
    Card(
        modifier = modifier
            .padding(bottom = Theme.spacing.sectionSpacing)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(Res.string.total_words),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                stats.totalWords.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))
            Text(
                stringResource(Res.string.cards_due_for_review, stats.dueCards),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            ReviewActionSection(
                hasDueCards = stats.dueCards > 0,
                onStartReview = onStartReview
            )

        }
    }
}



