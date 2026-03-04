package presentation.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ThrottledAction(
    private val scope: CoroutineScope,
    private val interval: Duration,
    private val action: suspend () -> Unit
) {
    private val mutex = Mutex()
    private var lastExecutionTime = Clock.System.now() - interval
    private var pendingJob: Job? = null

    fun request() {
        scope.launch {
            mutex.withLock {
                val now = Clock.System.now()
                val elapsed = now - lastExecutionTime

                if (elapsed >= interval) {
                    lastExecutionTime = now
                    action()
                } else {
                    pendingJob?.cancel()
                    val remaining = interval - elapsed
                    pendingJob = scope.launch {
                        delay(remaining)
                        mutex.withLock {
                            lastExecutionTime = Clock.System.now()
                            action()
                        }
                    }
                }
            }
        }
    }
}
