package feature.study.ui.wordrush

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.word_rush
import lexicon.resources.generated.resources.word_rush_add_words
import lexicon.resources.generated.resources.word_rush_best_streak
import lexicon.resources.generated.resources.word_rush_combo_hint
import lexicon.resources.generated.resources.word_rush_play
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

@Composable
fun WordRushCard(
    bestStreak: Int,
    hasEnoughWords: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Theme.elevation.low),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AppColors.tertiary.copy(alpha = 0.06f),
                            AppColors.accentAmber.copy(alpha = 0.04f),
                        ),
                    ),
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Theme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = AppColors.tertiary.copy(alpha = 0.12f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = AppColors.tertiary,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Theme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.word_rush),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (hasEnoughWords) {
                            if (bestStreak > 0) {
                                stringResource(Res.string.word_rush_best_streak, bestStreak)
                            } else {
                                stringResource(Res.string.word_rush_combo_hint)
                            }
                        } else {
                            stringResource(Res.string.word_rush_add_words)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Button(
                    onClick = onPlay,
                    enabled = hasEnoughWords,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.tertiary,
                    ),
                    shape = RoundedCornerShape(Theme.shapes.pill),
                    modifier = Modifier
                        .alpha(if (hasEnoughWords) 1f else 0.5f)
                        .padding(start = Theme.spacing.xs),
                ) {
                    Text(
                        text = stringResource(Res.string.word_rush_play),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
