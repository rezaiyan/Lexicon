package events

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun <T : Any> OnEvents(
    events: Flow<T>,
    handleEvent: suspend (T) -> Unit
) {
    // Validate flow type - events should not be cached
    require(events !is StateFlow<*>) { "Events flow can't be StateFlow" }
    if (events is SharedFlow<*>) {
        require(events.replayCache.isEmpty()) { "Events flow can't be SharedFlow with replay cache" }
    }
    
    // Always use latest handler without restarting collector
    val latestHandler = rememberUpdatedState(handleEvent)
    
    LaunchedEffect(events) {
        events.collect { event ->
            try {
                latestHandler.value(event)
            } catch (t: Throwable) {
                println("Error handling event: ${t.message}")
            }
        }
    }
}

