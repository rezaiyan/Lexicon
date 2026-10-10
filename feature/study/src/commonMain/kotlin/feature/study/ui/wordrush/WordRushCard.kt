package feature.study.ui.wordrush

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import feature.study.ui.components.PracticeModeTile
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.word_rush
import lexicon.resources.generated.resources.word_rush_add_words
import lexicon.resources.generated.resources.word_rush_best_streak
import lexicon.resources.generated.resources.word_rush_combo_hint
import org.jetbrains.compose.resources.stringResource
import theme.AppColors

/** Study-tab entry point for the Word Rush game. */
@Composable
fun WordRushCard(
    bestStreak: Int,
    hasEnoughWords: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PracticeModeTile(
        title = stringResource(Res.string.word_rush),
        subtitle = when {
            !hasEnoughWords -> stringResource(Res.string.word_rush_add_words)
            bestStreak > 0 -> stringResource(Res.string.word_rush_best_streak, bestStreak)
            else -> stringResource(Res.string.word_rush_combo_hint)
        },
        icon = Icons.Rounded.Bolt,
        accent = AppColors.tertiary,
        enabled = hasEnoughWords,
        onClick = onPlay,
        modifier = modifier,
    )
}
