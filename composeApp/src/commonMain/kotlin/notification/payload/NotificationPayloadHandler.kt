package notification.payload

interface NotificationPayloadHandler {
    val type: String
    suspend fun handle(data: Map<String, String>)
}

