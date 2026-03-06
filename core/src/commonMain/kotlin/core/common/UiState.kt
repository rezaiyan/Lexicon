package core.common

import androidx.compose.runtime.Stable

@Stable
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>
    data class Loaded<T>(val value: T) : UiState<T>
}

fun <T> UiState<T>.isLoading(): Boolean = this is UiState.Loading
fun <T> UiState<T>.isError(): Boolean = this is UiState.Error
fun <T> UiState<T>.isLoaded(): Boolean = this is UiState.Loaded<*>

inline fun <T> UiState<T>.onLoading(action: () -> Unit): UiState<T> {
    if (this is UiState.Loading) action()
    return this
}

inline fun <T> UiState<T>.onError(action: (message: String, throwable: Throwable?) -> Unit): UiState<T> {
    if (this is UiState.Error) action(message, throwable)
    return this
}

inline fun <T> UiState<T>.onLoaded(action: (T) -> Unit): UiState<T> {
    if (this is UiState.Loaded<*>) {
        (this as? UiState.Loaded<T>)?.value?.let(action)
    }
    return this
}
