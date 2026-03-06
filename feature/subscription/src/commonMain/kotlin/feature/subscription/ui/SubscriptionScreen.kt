package feature.subscription.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import core.getPlatformName
import domain.subscription.model.SubscriptionCustomerInfo
import domain.subscription.model.SubscriptionPackage
import org.jetbrains.compose.resources.stringResource
import core.common.UiState
import components.scaffold.LexiconColumn
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.manage_subscription_app_store
import lexicon.resources.generated.resources.manage_subscription_device_settings
import lexicon.resources.generated.resources.manage_subscription_google_play
import lexicon.resources.generated.resources.no_purchases_to_restore
import lexicon.resources.generated.resources.purchase_failed
import lexicon.resources.generated.resources.purchases_restored_success
import lexicon.resources.generated.resources.restore_purchases_failed
import lexicon.resources.generated.resources.subscription_cancelled
import lexicon.resources.generated.resources.subscription_info_unavailable
import lexicon.resources.generated.resources.subscription_load_failed
import lexicon.resources.generated.resources.subscription_screen_title

@Composable
fun SubscriptionScreen(
    state: UiState<SubscriptionData>,
    isPurchasing: Boolean,
    errorMessage: String?,
    successMessage: String?,
    actions: SubscriptionScreenActions,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
    onNavigateBack: () -> Unit
) {
    val localizedErrorMessage = errorMessage?.let { getLocalizedErrorMessage(it) }
    val localizedSuccessMessage = successMessage?.let { getLocalizedSuccessMessage(it) }

    LaunchedEffect(localizedErrorMessage) {
        localizedErrorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            actions.onDismissError()
        }
    }

    LaunchedEffect(localizedSuccessMessage) {
        localizedSuccessMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            actions.onDismissSuccess()
        }
    }

    LexiconColumn(
        title = stringResource(Res.string.subscription_screen_title),
        showNavigationIcon = true,
        onNavigationClick = onNavigateBack,
        scrollable = true
    ) {
        when (state) {
            is UiState.Loading -> {
                SubscriptionLoadingContent()
            }

            is UiState.Error -> {
                SubscriptionErrorContent(
                    errorMessage = getLocalizedErrorMessage(state.message),
                    onRetryClick = actions.onRetryClick
                )
            }

            is UiState.Loaded -> {
                val subscriptionData = state.value
                if (subscriptionData.isSubscribed) {
                    SubscriptionActiveContent(
                        customerInfo = subscriptionData.customerInfo,
                        formattedExpirationDate = subscriptionData.formattedExpirationDate,
                        willRenew = subscriptionData.willRenew,
                        onManageSubscription = actions.onManageSubscription,
                        onCancelSubscription = if (subscriptionData.willRenew) actions.onCancelSubscription else null
                    )
                } else {
                    SubscriptionNotSubscribedContent(
                        packages = subscriptionData.packages,
                        isPurchasing = isPurchasing,
                        onPurchaseClick = actions.onPurchaseClick,
                        onRestoreClick = actions.onRestoreClick
                    )
                }
            }
        }
    }
}



data class SubscriptionData(
    val packages: List<SubscriptionPackage>,
    val isSubscribed: Boolean,
    val customerInfo: SubscriptionCustomerInfo?,
    val formattedExpirationDate: String? = null,
    val willRenew: Boolean = true
)

data class SubscriptionScreenActions(
    val onPurchaseClick: (SubscriptionPackage) -> Unit,
    val onRestoreClick: () -> Unit,
    val onRetryClick: () -> Unit,
    val onDismissError: () -> Unit,
    val onDismissSuccess: () -> Unit,
    val onManageSubscription: () -> Unit,
    val onCancelSubscription: (() -> Unit)? = null
)

@Composable
internal fun getLocalizedErrorMessage(error: String): String {
    return when (error) {
        "SUBSCRIPTION_LOAD_FAILED" -> stringResource(Res.string.subscription_load_failed)
        "PURCHASE_FAILED" -> stringResource(Res.string.purchase_failed)
        "NO_PURCHASES_TO_RESTORE" -> stringResource(Res.string.no_purchases_to_restore)
        "RESTORE_PURCHASES_FAILED" -> stringResource(Res.string.restore_purchases_failed)
        "SUBSCRIPTION_INFO_UNAVAILABLE" -> stringResource(Res.string.subscription_info_unavailable)
        "CANCEL_SUBSCRIPTION_FAILED" -> stringResource(Res.string.subscription_cancelled)
        else -> error
    }
}

@Composable
internal fun getLocalizedSuccessMessage(message: String): String {
    return when (message) {
        "PURCHASES_RESTORED_SUCCESS" -> stringResource(Res.string.purchases_restored_success)
        "MANAGE_SUBSCRIPTION_DEVICE_SETTINGS" -> {
            val platform = getPlatformName()
            when (platform) {
                "Android" -> stringResource(Res.string.manage_subscription_google_play)
                "iOS" -> stringResource(Res.string.manage_subscription_app_store)
                else -> stringResource(Res.string.manage_subscription_device_settings)
            }
        }
        "SUBSCRIPTION_MANAGEMENT_OPENED" -> {
            val platform = getPlatformName()
            when (platform) {
                "Android" -> stringResource(Res.string.manage_subscription_google_play)
                "iOS" -> stringResource(Res.string.manage_subscription_app_store)
                else -> stringResource(Res.string.manage_subscription_device_settings)
            }
        }
        else -> message
    }
}
