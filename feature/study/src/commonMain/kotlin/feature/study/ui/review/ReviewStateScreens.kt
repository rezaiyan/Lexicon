package feature.study.ui.review

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import components.EmptyScreen
import components.ErrorScreen
import components.LoadingScreen
import feature.study.model.ReviewError
import kotlin.time.Clock
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.next_review_in
import lexicon.resources.generated.resources.no_words_scheduled
import lexicon.resources.generated.resources.no_words_to_review
import lexicon.resources.generated.resources.retry
import org.jetbrains.compose.resources.stringResource

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

@Composable
fun EmptyState(nextDueAt: Long? = null) {
    val subtitle = if (nextDueAt != null) {
        val remainingMs = nextDueAt - Clock.System.now().toEpochMilliseconds()
        val countdownLabel = formatCountdown(remainingMs)
        stringResource(Res.string.next_review_in, countdownLabel)
    } else {
        stringResource(Res.string.no_words_scheduled)
    }
    EmptyScreen(
        title = stringResource(Res.string.no_words_to_review),
        subtitle = subtitle,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )
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
