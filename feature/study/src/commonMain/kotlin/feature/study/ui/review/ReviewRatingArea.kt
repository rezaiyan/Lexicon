package feature.study.ui.review

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import feature.study.ui.components.ReviewButton
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.advance
import lexicon.resources.generated.resources.did_you_remember
import lexicon.resources.generated.resources.forgot
import lexicon.resources.generated.resources.remembered
import lexicon.resources.generated.resources.restart
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/**
 * Review-mode rating area.
 *
 * The button row always occupies 68 dp so the layout never shifts.
 * When the card is not yet flipped, the buttons fade + slide down out of view
 * using [graphicsLayer] (which doesn't affect measured size).
 * Once flipped, they ease back to their natural position — nudging the user
 * to rate only after seeing the answer.
 */
@Composable
internal fun ReviewRatingArea(
    isFlipped: Boolean,
    onReview: (Int) -> Unit
) {
    val buttonAlpha by animateFloatAsState(
        targetValue = if (isFlipped) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "buttonAlpha"
    )
    val buttonTranslationY by animateFloatAsState(
        targetValue = if (isFlipped) 0f else 32f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "buttonSlide"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall3)
    ) {
        Text(
            text = stringResource(Res.string.did_you_remember),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Theme.spacing.extraSmall3)
        )

        // Row always has its full 68 dp height; graphicsLayer moves/fades it
        // without affecting sibling layout.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .graphicsLayer {
                    alpha = buttonAlpha
                    translationY = buttonTranslationY
                },
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing)
        ) {
            RatingButtonData.values.forEach { data ->
                ReviewButton(
                    text = stringResource(data.textRes),
                    subText = stringResource(data.subTextRes),
                    color = data.color(),
                    modifier = Modifier.weight(1f),
                    enabled = isFlipped,
                    onClick = { onReview(data.rating) }
                )
            }
        }
    }
}

internal data class RatingButtonData(
    val rating: Int,
    val textRes: org.jetbrains.compose.resources.StringResource,
    val subTextRes: org.jetbrains.compose.resources.StringResource,
    val color: @Composable () -> Color
) {
    companion object {
        val values = listOf(
            RatingButtonData(
                rating = 0,
                textRes = Res.string.forgot,
                subTextRes = Res.string.restart,
                color = { MaterialTheme.colorScheme.error }
            ),
            RatingButtonData(
                rating = 1,
                textRes = Res.string.remembered,
                subTextRes = Res.string.advance,
                color = { MaterialTheme.colorScheme.primary }
            )
        )
    }
}
