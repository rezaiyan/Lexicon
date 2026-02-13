package presentation.ui.screens.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import presentation.ui.screens.getLocalizedErrorMessage
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.retry
import lexicon.resources.generated.resources.subscription_unable_to_load

@Composable
fun SubscriptionErrorContent(
    errorMessage: String,
    onRetryClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(Theme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSizeXLarge),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(Res.string.subscription_unable_to_load),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = getLocalizedErrorMessage(errorMessage),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )

            Button(
                onClick = onRetryClick,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(Theme.spacing.extraSmall2)
            ) {
                Text(stringResource(Res.string.retry), fontWeight = FontWeight.Bold)
            }
        }
    }
}

