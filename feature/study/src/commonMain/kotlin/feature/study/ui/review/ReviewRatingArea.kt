package feature.study.ui.review

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import feature.study.ui.components.ReviewButton
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.advance
import lexicon.resources.generated.resources.did_you_remember
import lexicon.resources.generated.resources.forgot
import lexicon.resources.generated.resources.remembered
import lexicon.resources.generated.resources.restart
import lexicon.resources.generated.resources.show_answer
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private val FooterMinHeight = 96.dp

/**
 * Review-mode footer. Before the flip it offers a single "Show answer" button; after the flip it
 * cross-fades to the two rating buttons. Both states share a minimum height so the card above never jumps.
 */
@Composable
internal fun ReviewRatingArea(
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onReview: (Int) -> Unit,
) {
    AnimatedContent(
        targetState = isFlipped,
        transitionSpec = {
            val enter = fadeIn(tween(220, delayMillis = 80)) +
                slideInVertically(tween(280, easing = FastOutSlowInEasing)) { it / 4 }
            enter.togetherWith(fadeOut(tween(120)))
        },
        contentAlignment = Alignment.BottomCenter,
        label = "reviewFooter",
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FooterMinHeight),
    ) { flipped ->
        Box(Modifier.fillMaxWidth().heightIn(min = FooterMinHeight), contentAlignment = Alignment.BottomCenter) {
            if (flipped) RatingButtons(onReview) else ShowAnswerButton(onFlip)
        }
    }
}

@Composable
private fun ShowAnswerButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(Theme.shapes.pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        ),
    ) {
        Text(
            text = stringResource(Res.string.show_answer),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RatingButtons(onReview: (Int) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        Text(
            text = stringResource(Res.string.did_you_remember),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            ReviewButton(
                text = stringResource(Res.string.forgot),
                subText = stringResource(Res.string.restart),
                icon = Icons.Default.Close,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                onClick = { onReview(RatingForgot) },
                modifier = Modifier.weight(1f),
            )
            ReviewButton(
                text = stringResource(Res.string.remembered),
                subText = stringResource(Res.string.advance),
                icon = Icons.Default.Check,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = { onReview(RatingRemembered) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private const val RatingForgot = 0
private const val RatingRemembered = 1
