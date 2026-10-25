package platform

import domain.connectivity.INetworkMonitor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/** `navigator.onLine` and the window's online/offline events. */
class WasmJsNetworkMonitor : INetworkMonitor {

    override val isConnected: Flow<Boolean> = callbackFlow {
        trySend(isOnline())
        val unsubscribe = subscribe { online -> trySend(online) }
        awaitClose { invoke(unsubscribe) }
    }
        .distinctUntilChanged()
        .conflate()
}

private fun isOnline(): Boolean = js("navigator.onLine")

/** Returns the function that removes both listeners. */
@Suppress("UnusedParameter") // read by the js() body
private fun subscribe(onChange: (Boolean) -> Unit): JsAny = js(
    """{
        const handler = () => onChange(navigator.onLine);
        window.addEventListener('online', handler);
        window.addEventListener('offline', handler);
        return () => {
            window.removeEventListener('online', handler);
            window.removeEventListener('offline', handler);
        };
    }"""
)

@Suppress("UnusedParameter") // read by the js() body
private fun invoke(fn: JsAny): Unit = js("fn()")
