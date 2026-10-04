package notification.payload

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationPayloadHandlerRegistry(
    private val handlers: Map<String, NotificationPayloadHandler>
) {
    fun getHandler(type: String?): NotificationPayloadHandler? {
        return type?.let { handlers[it] }
    }
    
    /** Runs the handler to completion; for platforms that must report when background work is done. */
    suspend fun handleAndAwait(type: String?, data: Map<String, String>) {
        getHandler(type)?.handle(data)
    }

    fun handle(type: String?, body: String, data: Map<String, String>) {
        getHandler(type)?.let { handler ->
            CoroutineScope(Dispatchers.Default).launch {
                handler.handle(data + ("body" to body))
            }
        }
    }
}

