package presentation.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

fun shareContentAsFile(title: String, text: String, filename: String) {
    expects.shareTextAsFile(title, text, filename)
}

fun <T> Flow<T>.stateInEagerly(
    scope: CoroutineScope,
    initialValue: T
): StateFlow<T> = stateIn(scope, SharingStarted.Eagerly, initialValue)

fun <T> Flow<T>.stateInWhileSubscribed(
    scope: CoroutineScope,
    stopTimeoutMillis: Long = 5000L,
    initialValue: T
): StateFlow<T> = stateIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis), initialValue)