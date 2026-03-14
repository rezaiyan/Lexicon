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
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.no_words_to_review
import lexicon.resources.generated.resources.retry
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoadingState() {
    LoadingScreen()
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    val isNetworkError = message.contains("timeout", ignoreCase = true) ||
        message.contains("connect", ignoreCase = true) ||
        message.contains("network", ignoreCase = true) ||
        message.contains("internet", ignoreCase = true)

    ErrorScreen(
        message = if (isNetworkError) {
            "You're offline -- your words are stored locally, but we couldn't load them right now. Check your connection and try again."
        } else {
            message.ifEmpty { "Something went wrong loading your words." }
        },
        title = if (isNetworkError) "No Connection" else null,
        retryLabel = stringResource(Res.string.retry),
        onRetry = onRetry
    )
}

@Composable
fun EmptyState() {
    EmptyScreen(
        title = stringResource(Res.string.no_words_to_review),
        subtitle = "All caught up! Import more words to keep learning.",
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
