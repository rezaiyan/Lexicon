package feature.subscription.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import components.scaffold.LexiconColumn
import core.common.UiState
import events.OnEvents
import feature.subscription.SubscriptionViewModel
import feature.subscription.model.SubscriptionContent
import feature.subscription.model.SubscriptionEffect
import feature.subscription.model.SubscriptionScreenState
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.no_purchases_to_restore
import lexicon.resources.generated.resources.purchase_failed
import lexicon.resources.generated.resources.purchases_restored_success
import lexicon.resources.generated.resources.restore_purchases_failed
import lexicon.resources.generated.resources.subscription
import lexicon.resources.generated.resources.subscription_info_unavailable
import lexicon.resources.generated.resources.subscription_load_failed
import lexicon.resources.generated.resources.subscription_screen_title
import lexicon.resources.generated.resources.web_subscriptions_not_available
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Stateful entry: wires the ViewModel, one-shot messages and store-return refresh. */
@Composable
fun SubscriptionScreen(
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    viewModel: SubscriptionViewModel = koinViewModel(),
) {
    val state by viewModel.state()

    OnEvents(viewModel.effects) { effect ->
        snackbarHostState.showSnackbar(effect.message())
    }

    // Coming back from the store page is a resume; that's when a cancel or resubscribe shows up.
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    SubscriptionScreen(
        state = state,
        actions = SubscriptionActions(
            onSelectPlan = viewModel::selectPlan,
            onPurchase = viewModel::purchaseSelectedPlan,
            onRestore = viewModel::restorePurchases,
            onRetry = viewModel::retry,
            onManage = viewModel::manageSubscription,
        ),
        onNavigateBack = onNavigateBack,
    )
}

@Immutable
data class SubscriptionActions(
    val onSelectPlan: (String) -> Unit,
    val onPurchase: () -> Unit,
    val onRestore: () -> Unit,
    val onRetry: () -> Unit,
    /** Opens the store page, where users cancel, resubscribe or change plan. */
    val onManage: () -> Unit,
)

@Composable
fun SubscriptionScreen(
    state: SubscriptionScreenState,
    actions: SubscriptionActions,
    onNavigateBack: () -> Unit,
) {
    val content = state.content
    val isMember = (content as? UiState.Loaded)?.value.let {
        it is SubscriptionContent.Member || it is SubscriptionContent.Paused
    }

    LexiconColumn(
        title = stringResource(if (isMember) Res.string.subscription else Res.string.subscription_screen_title),
        showNavigationIcon = true,
        onNavigationClick = onNavigateBack,
        scrollable = true,
    ) {
        when (content) {
            UiState.Loading -> SubscriptionLoadingContent()
            is UiState.Error -> SubscriptionErrorContent(
                errorMessage = localizedMessage(content.message),
                onRetryClick = actions.onRetry,
            )
            is UiState.Loaded -> when (val value = content.value) {
                is SubscriptionContent.Member -> SubscriptionActiveContent(
                    membership = value.membership,
                    monthlyAiCredits = state.monthlyAiCredits,
                    onManage = actions.onManage,
                )
                is SubscriptionContent.Paused -> SubscriptionPausedContent(
                    resumesOn = value.resumesOn,
                    daysLeft = value.daysLeft,
                    monthlyAiCredits = state.monthlyAiCredits,
                    onResume = actions.onManage,
                )
                is SubscriptionContent.Paywall -> SubscriptionNotSubscribedContent(
                    plans = value.plans,
                    selectedPlanId = state.selectedPlanId,
                    isPurchasing = state.isPurchasing,
                    monthlyAiCredits = state.monthlyAiCredits,
                    onSelectPlan = actions.onSelectPlan,
                    onPurchase = actions.onPurchase,
                    onRestoreClick = actions.onRestore,
                )
            }
        }
    }
}

private suspend fun SubscriptionEffect.message(): String = when (this) {
    SubscriptionEffect.PurchasesRestored -> getString(Res.string.purchases_restored_success)
    SubscriptionEffect.NothingToRestore -> getString(Res.string.no_purchases_to_restore)
    is SubscriptionEffect.Failure -> messageKeyResource(message)?.let { getString(it) } ?: message
}

@Composable
internal fun localizedMessage(message: String): String =
    messageKeyResource(message)?.let { stringResource(it) } ?: message

/** Maps the message keys the data layer reports to localized strings; other text passes through. */
private fun messageKeyResource(key: String): StringResource? = when (key) {
    "SUBSCRIPTION_LOAD_FAILED" -> Res.string.subscription_load_failed
    "PURCHASE_FAILED" -> Res.string.purchase_failed
    "NO_PURCHASES_TO_RESTORE" -> Res.string.no_purchases_to_restore
    "RESTORE_PURCHASES_FAILED" -> Res.string.restore_purchases_failed
    "SUBSCRIPTION_INFO_UNAVAILABLE" -> Res.string.subscription_info_unavailable
    "WEB_SUBSCRIPTIONS_NOT_AVAILABLE" -> Res.string.web_subscriptions_not_available
    else -> null
}
