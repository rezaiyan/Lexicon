package feature.study.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.ErrorScreen
import components.LoadingScreen
import feature.study.model.ReviewError
import kotlin.time.Clock
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.back_to_study
import lexicon.resources.generated.resources.close
import lexicon.resources.generated.resources.next_review_in
import lexicon.resources.generated.resources.no_words_scheduled
import lexicon.resources.generated.resources.no_words_to_review
import lexicon.resources.generated.resources.nothing_due_message
import lexicon.resources.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
fun LoadingState() {
    LoadingScreen()
}

@Composable
fun ErrorState(
    error: ReviewError,
    onRetry: () -> Unit,
) {
    ErrorScreen(
        message = when (error) {
            is ReviewError.Network ->
                "You're offline -- your words are stored locally, " +
                    "but we couldn't load them right now. Check your connection and try again."
            is ReviewError.Unknown ->
                error.message.ifEmpty { "Something went wrong loading your words." }
        },
        title = when (error) {
            is ReviewError.Network -> "No Connection"
            is ReviewError.Unknown -> null
        },
        retryLabel = stringResource(Res.string.retry),
        onRetry = onRetry,
    )
}

/** Nothing due: success tile, title, message, next-review pill, and a pinned "Back to study" button. */
@Composable
fun EmptyState(nextDueAt: Long? = null, onDismiss: () -> Unit = {}) {
    val nextLabel = nextDueAt?.let {
        stringResource(Res.string.next_review_in, formatCountdown(it - Clock.System.now().toEpochMilliseconds()))
    }
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onDismiss, modifier = Modifier.padding(Theme.spacing.xxs)) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(Res.string.close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Theme.colors.success.copy(alpha = Theme.opacity.focus)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = Theme.colors.success,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
            ) {
                Text(
                    text = stringResource(Res.string.no_words_to_review),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(
                        if (nextLabel != null) Res.string.nothing_due_message else Res.string.no_words_scheduled
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (nextLabel != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Theme.shapes.pill))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus))
                        .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = nextLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        FilledTonalButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm)
                .heightIn(min = 56.dp),
            shape = RoundedCornerShape(Theme.shapes.pill),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Text(
                text = stringResource(Res.string.back_to_study),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun formatCountdown(remainingMs: Long): String {
    if (remainingMs <= 0) return "a moment"
    val totalMinutes = remainingMs / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "a moment"
    }
}
