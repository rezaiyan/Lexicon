package expects

/**
 * Shared interface for platform-specific share functionality
 */
interface ShareService {
    fun share(title: String, text: String)
}

/**
 * Provides platform-specific share service implementation
 * Use this via Koin dependency injection
 */
expect fun getShareService(): ShareService

