package domain.credits.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.credits.ICreditsRepository
import domain.credits.model.CreditBalance

/**
 * Re-reads the balance from the server, e.g. when a screen that spends credits opens, or after a
 * spend (the server may also have refunded it). Observers of [ObserveCreditsUseCase] get the result.
 */
class RefreshCreditsUseCase(
    private val repository: ICreditsRepository,
) : NoParamUseCase<CreditBalance> {

    override suspend operator fun invoke(params: Unit): Try<CreditBalance> = invoke()

    suspend operator fun invoke(): Try<CreditBalance> = repository.refresh()
}
