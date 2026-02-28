package presentation.ui.screens.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.profile.LottieMotionIcon
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.done
import lexicon.resources.generated.resources.session_complete
import lexicon.resources.generated.resources.you_reviewed_cards

private const val AUTO_DISMISS_MS = 3000L

private const val CELEBRATION_LOTTIE_URL =
    "https://assets-v2.lottiefiles.com/a/a43cbf9c-1176-11ee-91da-9386bbc12f66/xLGIzaka1v.json"

@Composable
fun ReviewCompletionContent(
    reviewedCount: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-dismiss countdown
    var progress by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(Unit) {
        val steps = 30
        val stepMs = AUTO_DISMISS_MS / steps
        for (i in steps downTo 0) {
            progress = i.toFloat() / steps
            delay(stepMs)
        }
        onDismiss()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Theme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            LottieMotionIcon(
                url = CELEBRATION_LOTTIE_URL,
                modifier = Modifier.size(180.dp)
            )
        }

        Spacer(Modifier.height(Theme.spacing.small))

        Text(
            text = stringResource(Res.string.session_complete),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(Theme.spacing.extraSmall2))

        Text(
            text = stringResource(Res.string.you_reviewed_cards, reviewedCount),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(Theme.spacing.medium))

        // Auto-dismiss progress indicator
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(3.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            trackColor = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(Modifier.height(Theme.spacing.medium))

        Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(Theme.shapes.large)
        ) {
            Text(
                text = stringResource(Res.string.done),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
