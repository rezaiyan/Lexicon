package notification

enum class NotificationCategory(val value: String) {
    USER("user"),
    SYSTEM("system");
    
    companion object {
        fun fromString(value: String?): NotificationCategory {
            return entries.find { it.value == value } ?: SYSTEM
        }
    }
}

