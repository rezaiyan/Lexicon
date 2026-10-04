package feature.subscription.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.runtime.Composable
import components.ErrorScreen
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.retry
import lexicon.resources.generated.resources.subscription_unable_to_load
import org.jetbrains.compose.resources.stringResource

@Composable
fun SubscriptionErrorContent(
    errorMessage: String,
    onRetryClick: () -> Unit,
) {
    ErrorScreen(
        title = stringResource(Res.string.subscription_unable_to_load),
        message = errorMessage,
        icon = Icons.Outlined.CloudOff,
        retryLabel = stringResource(Res.string.retry),
        onRetry = onRetryClick,
    )
}
