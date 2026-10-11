package fakes

import core.common.Try
import domain.support.model.SupportDeliveryStatus
import domain.support.model.SupportMessage
import domain.support.model.SupportSender
import domain.support.repository.ISupportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSupportRepository : ISupportRepository {
    val messages = MutableStateFlow<List<SupportMessage>>(emptyList())
    var refreshResult: Try<Unit> = Try.success(Unit)
    var sendResult: Try<Unit> = Try.success(Unit)
    var refreshCount = 0
        private set
    val sent = mutableListOf<String>()
    val retried = mutableListOf<String>()

    override fun observeMessages(): Flow<List<SupportMessage>> = messages

    override suspend fun refresh(): Try<Unit> {
        refreshCount++
        return refreshResult
    }

    override suspend fun send(body: String): Try<Unit> {
        sent += body
        if (sendResult.isSuccess) {
            messages.value = messages.value + SupportMessage(
                id = "local-${sent.size}",
                sender = SupportSender.USER,
                body = body,
                createdAtMillis = 0,
                status = SupportDeliveryStatus.SENT,
            )
        }
        return sendResult
    }

    override suspend fun retry(messageId: String): Try<Unit> {
        retried += messageId
        return Try.success(Unit)
    }
}
