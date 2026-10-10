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
            val payload = payloadWithBody(body, data)
            CoroutineScope(Dispatchers.Default).launch {
                handler.handle(payload)
            }
        }
    }

    internal companion object {
        /**
         * [body] is the displayed notification's text; a data-only (silent) push has none, so its
         * own "body" data entry (e.g. today's insight) is kept instead of being blanked out.
         */
        fun payloadWithBody(body: String, data: Map<String, String>): Map<String, String> =
            if (body.isBlank()) data else data + ("body" to body)
    }
}

