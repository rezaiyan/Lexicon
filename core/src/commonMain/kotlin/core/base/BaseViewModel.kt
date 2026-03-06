package core.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import core.common.Try
import core.common.fold
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel providing unified state management via Compose snapshot state
 * and one-shot effects via [Channel].
 *
 * @param S The UI state type held as Compose [mutableStateOf].
 * @param F The one-shot effect type (navigation, snackbar, etc.). Use [Nothing] if unused.
 */
abstract class BaseViewModel<S, F> : ViewModel() {

    private val _state = mutableStateOf(initialState())

    /** Current state value for non-composable reads (public for testability). */
    val currentState: S get() = _state.value

    /** Compose snapshot state — triggers recomposition automatically. */
    @Composable
    fun state(): State<S> = _state

    private val _effects = Channel<F>(Channel.BUFFERED)

    /** One-shot effects collected via [Flow.collect] in the UI layer. */
    val effects: Flow<F> = _effects.receiveAsFlow()

    /** Provide the initial UI state for this ViewModel. */
    protected abstract fun initialState(): S

    /** Apply a reducer to the current state. */
    protected fun updateState(reducer: S.() -> S) {
        _state.value = _state.value.reducer()
    }

    /** Send a one-shot effect to the UI layer. */
    protected fun emitEffect(effect: F) {
        viewModelScope.launch { _effects.send(effect) }
    }

    /** [SendChannel] for delegating effect emission to handler classes. */
    protected val effectsSendChannel: SendChannel<F> get() = _effects

    /** Fold a [Try] result directly into a state update. */
    protected fun <T> Try<T>.reduce(
        onSuccess: S.(T) -> S,
        onFailure: S.(Throwable) -> S,
    ) {
        fold(
            onSuccess = { value -> updateState { onSuccess(value) } },
            onFailure = { error -> updateState { onFailure(error) } },
        )
    }

    /**
     * Typed accessor for delegating state reads/writes to handler classes.
     * Avoids exposing [MutableStateFlow] to handlers.
     */
    interface StateAccess<S> {
        val current: S
        fun update(reducer: S.() -> S)
    }

    /** [StateAccess] instance wired to this ViewModel's snapshot state. */
    protected val stateAccess: StateAccess<S> = object : StateAccess<S> {
        override val current get() = currentState
        override fun update(reducer: S.() -> S) = updateState(reducer)
    }
}
