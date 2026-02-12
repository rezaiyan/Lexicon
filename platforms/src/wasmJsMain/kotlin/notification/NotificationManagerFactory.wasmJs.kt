package notification

/**
 * WasmJs factory function for notification manager
 */
actual fun createNotificationManager(): INotificationManager {
    return WasmJsNotificationManager()
}
