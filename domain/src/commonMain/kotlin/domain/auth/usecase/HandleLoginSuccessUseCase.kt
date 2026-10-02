package domain.auth.usecase

import core.common.Try
import core.common.UseCase
import core.common.getOrNull
import core.common.getOrThrow
import domain.auth.model.AuthUser
import domain.notifications.usecase.InitializePushNotificationsUseCase
import domain.subscription.ISubscriptionManager
import domain.subscription.usecase.SyncSubscriptionWithServerUseCase
import domain.tag.usecase.SyncTagsFromRemoteUseCase
import domain.word.usecase.SyncRemoteToLocalUseCase

/**
 * Orchestrates all domain side-effects that must run after a successful login or session restore.
 * Centralises the post-login sequence so the ViewModel doesn't know the order of operations.
 *
 * @param syncData true for fresh logins (remote → local sync needed); false for session restores.
 */
class HandleLoginSuccessUseCase(
    private val subscriptionManager: ISubscriptionManager,
    private val syncSubscriptionWithServerUseCase: SyncSubscriptionWithServerUseCase,
    private val syncTagsFromRemoteUseCase: SyncTagsFromRemoteUseCase,
    private val syncRemoteToLocalUseCase: SyncRemoteToLocalUseCase,
    private val initializePushNotificationsUseCase: InitializePushNotificationsUseCase,
) : UseCase<HandleLoginSuccessUseCase.Params, Unit> {

    data class Params(val user: AuthUser, val syncData: Boolean = true)

    override suspend operator fun invoke(params: Params): Try<Unit> = Try {
        // Best effort: a store outage must not skip data sync. Every session restore retries the
        // logIn, and RevenueCat aliases any purchase made meanwhile once it succeeds.
        val storeCustomer = subscriptionManager.logIn(params.user.id.toString()).getOrNull()
        if (storeCustomer?.isSubscribed == true) {
            // Store says premium: make sure the server agrees (covers missed webhooks). Best effort.
            syncSubscriptionWithServerUseCase()
        }
        if (params.syncData) {
            syncTagsFromRemoteUseCase(Unit).getOrThrow()
            syncRemoteToLocalUseCase(clearFirst = false).getOrThrow()
        }
        initializePushNotificationsUseCase(Unit).getOrThrow()
    }
}
