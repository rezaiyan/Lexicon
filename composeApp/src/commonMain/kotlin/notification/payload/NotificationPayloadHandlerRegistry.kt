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
    
    fun handle(type: String?, data: Map<String, String>) {
        getHandler(type)?.let { handler ->
            CoroutineScope(Dispatchers.Default).launch {
                handler.handle(data)
            }
        }
    }
}

