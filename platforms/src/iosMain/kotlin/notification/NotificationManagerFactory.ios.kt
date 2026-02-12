package notification

actual fun createNotificationManager(): INotificationManager {
    return IosNotificationManager()
}

