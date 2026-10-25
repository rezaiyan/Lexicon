package domain.connectivity.usecase

import core.common.FlowUseCase
import core.common.Try
import core.common.UseCase
import domain.connectivity.model.ServerStatus
import domain.connectivity.repository.IServerStatusRepository
import kotlinx.coroutines.flow.Flow

class ObserveServerStatusUseCase(
    private val repository: IServerStatusRepository,
) : FlowUseCase<Unit, ServerStatus> {
    override fun invoke(params: Unit): Flow<ServerStatus> = repository.observeStatus()
}

class CheckServerStatusUseCase(
    private val repository: IServerStatusRepository,
) : UseCase<Unit, ServerStatus> {
    override suspend fun invoke(params: Unit): Try<ServerStatus> = repository.check()
}
