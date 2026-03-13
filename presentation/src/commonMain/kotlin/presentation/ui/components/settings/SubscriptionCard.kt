package presentation.ui.components.settings

import components.icons.LexiconIcons
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import components.Pill
import domain.auth.manager.IUserManager
import domain.subscription.ISubscriptionManager
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import presentation.ui.components.SettingsCard
import theme.AppColors
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.subscription
import lexicon.resources.generated.resources.upgrade
import lexicon.resources.generated.resources.upgrade_to_premium

@Composable
fun SubscriptionCard(
    onClick: () -> Unit,
    subscriptionManager: ISubscriptionManager = koinInject(),
    userManager: IUserManager = koinInject()
) {
    val currentUser by userManager.observeUser().collectAsState(initial = null)
    val isSubscribed by subscriptionManager.isSubscribed().collectAsState(false)

    if (currentUser != null) {
        SettingsCard(
            icon = LexiconIcons.Diamond,
            title = stringResource(Res.string.subscription),
            subtitle = if (isSubscribed.not()) {
                stringResource(Res.string.upgrade_to_premium)
            } else {
                null
            },
            iconBackgroundColor = AppColors.settingsSubscriptionIcon,
            solidIconBackground = true,
            onClick = onClick,
            trailingContent = if (isSubscribed.not()) {
                {
                    Pill(
                        text = stringResource(Res.string.upgrade),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                null
            }
        )
    }
}
