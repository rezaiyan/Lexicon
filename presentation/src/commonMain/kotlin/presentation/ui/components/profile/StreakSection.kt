package presentation.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import org.kodein.emoji.compose.m3.TextWithNotoImageEmoji
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import domain.streak.model.StreakData
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.best_streak
import lexicon.resources.generated.resources.day_streak

@Composable
fun StreakSection(
    streak: StreakData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.cardPadding),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.cardPadding),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StreakItem(
                emoji = "🔥",
                value = streak.currentStreak,
                label = stringResource(Res.string.day_streak),
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .width(Theme.dimensions.borderWidth)
                    .height(Theme.dimensions.iconSizeMassive)
                    .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
            )

            StreakItem(
                emoji = "🏆",
                value = streak.highestStreak,
                label = stringResource(Res.string.best_streak),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StreakItem(
    emoji: String,
    value: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        TextWithNotoImageEmoji(
            text = emoji,
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))
        Text(
            text = "$value",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

