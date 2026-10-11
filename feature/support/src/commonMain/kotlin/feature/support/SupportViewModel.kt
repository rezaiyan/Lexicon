package feature.support

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.fold
import domain.support.model.MAX_SUPPORT_MESSAGE_LENGTH
import domain.support.usecase.MarkSupportReadUseCase
import domain.support.usecase.ObserveSupportMessagesUseCase
import domain.support.usecase.RefreshSupportMessagesUseCase
import domain.support.usecase.RetrySupportMessageUseCase
import domain.support.usecase.SendSupportMessageUseCase
import feature.support.model.SupportState
import feature.support.model.buildSupportTimeline
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Support chat. Shows the local conversation and, while the screen is open, refreshes on every
 * [pollTicks] emission (by default at once, then every 15 s) so replies show up without leaving.
 */
class SupportViewModel(
    observeMessages: ObserveSupportMessagesUseCase,
    private val refreshMessages: RefreshSupportMessagesUseCase,
    private val sendMessage: SendSupportMessageUseCase,
    private val retryMessage: RetrySupportMessageUseCase,
    private val markRead: MarkSupportReadUseCase,
    pollTicks: Flow<Unit> = every(DEFAULT_POLL_INTERVAL_MS),
    clock: Clock = Clock.System,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : BaseViewModel<SupportState, Nothing>() {

    override fun initialState(): SupportState = SupportState()

    init {
        viewModelScope.launch {
            observeMessages(Unit).collect { messages ->
                val today = clock.now().toLocalDateTime(timeZone).date
                val timeline = buildSupportTimeline(messages, today, timeZone)
                updateState { copy(messages = messages, timeline = timeline, isLoading = false) }
                // Everything on screen counts as read, including replies arriving while it is open.
                markRead(Unit)
            }
        }
        viewModelScope.launch {
            pollTicks.collect { refresh() }
        }
    }

    fun onDraftChanged(text: String) {
        updateState { copy(draft = text.take(MAX_SUPPORT_MESSAGE_LENGTH)) }
    }

    fun send() {
        val body = currentState.draft
        if (body.isBlank()) return
        updateState { copy(draft = "") }
        viewModelScope.launch {
            // Stored locally first, so a failed send shows in the list with retry; restore the
            // draft only if it could not even be stored.
            sendMessage(body).fold(onSuccess = {}, onFailure = { updateState { copy(draft = body) } })
        }
    }

    fun retry(messageId: String) {
        viewModelScope.launch { retryMessage(messageId) }
    }

    private suspend fun refresh() {
        refreshMessages(Unit).fold(
            onSuccess = { updateState { copy(isOffline = false) } },
            onFailure = { updateState { copy(isOffline = true) } },
        )
    }

    private companion object {
        const val DEFAULT_POLL_INTERVAL_MS = 15_000L

        /** Emits now, then every [intervalMs], until the collector is cancelled. */
        fun every(intervalMs: Long): Flow<Unit> = flow {
            while (true) {
                emit(Unit)
                delay(intervalMs)
            }
        }
    }
}
