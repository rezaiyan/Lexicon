package notification

interface NotificationDisplayService {
    fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>
    )
}

