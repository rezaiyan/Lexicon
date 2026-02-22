package presentation.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import domain.auth.manager.IUserManager
import domain.subscription.ISubscriptionManager
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import presentation.ui.components.SettingsCard
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.subscription
import lexicon.resources.generated.resources.upgrade_to_premium

@Composable
fun SubscriptionCard(
    onClick: () -> Unit,
    subscriptionManager: ISubscriptionManager = koinInject(),
    userManager: IUserManager = koinInject()
) {
    val currentUser by userManager.observeUser().collectAsStateWithLifecycle(initialValue = null)
    val isSubscribed by subscriptionManager.isSubscribed().collectAsStateWithLifecycle(false)

    if (currentUser != null) {
        SettingsCard(
            icon = Icons.Default.Star,
            title = stringResource(Res.string.subscription),
            subtitle = if (isSubscribed.not()) {
                stringResource(Res.string.upgrade_to_premium)
            } else {
                null
            },
            onClick = onClick
        )
    }
}
