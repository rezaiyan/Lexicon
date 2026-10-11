package domain.support.usecase

import core.common.FlowUseCase
import core.common.Try
import core.common.UseCase
import core.error.DomainError
import domain.support.model.MAX_SUPPORT_MESSAGE_LENGTH
import domain.support.model.SupportMessage
import domain.support.repository.ISupportRepository
import kotlinx.coroutines.flow.Flow

class ObserveSupportMessagesUseCase(
    private val repository: ISupportRepository,
) : FlowUseCase<Unit, List<SupportMessage>> {
    override fun invoke(params: Unit): Flow<List<SupportMessage>> = repository.observeMessages()
}

class RefreshSupportMessagesUseCase(
    private val repository: ISupportRepository,
) : UseCase<Unit, Unit> {
    override suspend fun invoke(params: Unit): Try<Unit> = repository.refresh()
}

/** Sends a trimmed message; blank text is rejected, overlong text is cut to the backend limit. */
class SendSupportMessageUseCase(
    private val repository: ISupportRepository,
) : UseCase<String, Unit> {
    override suspend fun invoke(params: String): Try<Unit> {
        val body = params.trim()
        if (body.isEmpty()) return Try.failure(DomainError.Validation.BlankField("message"))
        return repository.send(body.take(MAX_SUPPORT_MESSAGE_LENGTH))
    }
}

class RetrySupportMessageUseCase(
    private val repository: ISupportRepository,
) : UseCase<String, Unit> {
    override suspend fun invoke(params: String): Try<Unit> = repository.retry(params)
}
