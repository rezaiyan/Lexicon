package domain.auth.usecase

import core.common.NoParamFlowUseCase
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.PremiumSource
import domain.auth.repository.IAuthRepository
import domain.subscription.ISubscriptionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Single source of truth for feature access, including the premium gate.
 *
 * Premium = backend grant (server-side status, test users, other platforms)
 *        OR an active store entitlement on this device.
 *
 * The store side makes premium unlock the moment a purchase/restore completes, without waiting
 * for the RevenueCat → backend webhook round trip (and keeps paid users unlocked when the
 * feature-access request fails). Server-enforced features still rely on the backend status.
 */
class GetFeatureAccessUseCase(
    private val authRepository: IAuthRepository,
    private val subscriptionManager: ISubscriptionManager,
) : NoParamFlowUseCase<FeatureAccessResponse> {

    operator fun invoke(): Flow<FeatureAccessResponse> =
        combine(
            authRepository.getFeatureAccessAsFlow(),
            subscriptionManager.isSubscribed().distinctUntilChanged(),
        ) { backend, storeSubscribed ->
            if (storeSubscribed && !backend.userAccess.hasPremiumAccess) {
                backend.copy(
                    userAccess = backend.userAccess.copy(hasPremiumAccess = true, source = PremiumSource.STORE.name)
                )
            } else {
                backend
            }
        }.distinctUntilChanged()

    override operator fun invoke(params: Unit) = invoke()
}
