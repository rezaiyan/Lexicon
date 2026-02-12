package notification

/**
 * Factory to create platform-specific notification managers
 */
expect fun createNotificationManager(): INotificationManager

