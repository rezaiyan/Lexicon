package domain.subscription.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.subscription.ISubscriptionAccessRepository

/**
 * Re-reads premium from the backend when the app returns to the foreground, so grants,
 * expirations and purchases made elsewhere show up without a restart. Throttled by the
 * repository; safe to call on every resume.
 */
class RefreshFeatureAccessUseCase(
    private val repository: ISubscriptionAccessRepository,
) : NoParamUseCase<Unit> {

    override suspend operator fun invoke(params: Unit): Try<Unit> = invoke()

    suspend operator fun invoke(): Try<Unit> = repository.refresh()
}
