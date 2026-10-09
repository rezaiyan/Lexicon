package domain.credits.usecase

import core.common.NoParamFlowUseCase
import domain.credits.ICreditsRepository
import domain.credits.model.CreditBalance
import kotlinx.coroutines.flow.Flow

/** The signed-in user's AI credits; null while unknown. Pair with [RefreshCreditsUseCase] to load. */
class ObserveCreditsUseCase(
    private val repository: ICreditsRepository,
) : NoParamFlowUseCase<CreditBalance?> {

    override operator fun invoke(params: Unit): Flow<CreditBalance?> = invoke()

    operator fun invoke(): Flow<CreditBalance?> = repository.observeBalance()
}
