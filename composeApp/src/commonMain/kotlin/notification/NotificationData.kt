package notification

data class NotificationData(
    val title: String,
    val body: String,
    val data: Map<String, String>,
    val category: NotificationCategory = NotificationCategory.SYSTEM,
    val type: String? = null
) {
    companion object {
        fun fromMap(
            title: String,
            body: String,
            data: Map<String, String>
        ): NotificationData {
            val category = NotificationCategory.fromString(data["category"])
            val type = data["type"]
            return NotificationData(
                title = title,
                body = body,
                data = data,
                category = category,
                type = type
            )
        }
    }
}

