package notification.payload

import domain.subscription.usecase.RefreshSubscriptionStateUseCase
import notification.PushTypes

/**
 * Silent push sent by the backend after a store webhook changed the user's subscription.
 * The push carries no subscription data; the app refetches the authoritative state, and every
 * open screen observing premium updates from the shared cache.
 */
class SubscriptionUpdatedHandler(
    private val refreshSubscriptionState: RefreshSubscriptionStateUseCase,
) : NotificationPayloadHandler {

    override val type: String = PushTypes.SUBSCRIPTION_UPDATED

    override suspend fun handle(data: Map<String, String>) {
        refreshSubscriptionState()
    }
}
