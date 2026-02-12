package notification.payload

class NoOpHandler(
    override val type: String
) : NotificationPayloadHandler {
    
    override suspend fun handle(data: Map<String, String>) {
        // No operation - for notification types that don't require special handling
    }
}

