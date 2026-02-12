package platform

/**
 * Platform-agnostic interface for retrieving the app version
 * Following clean architecture principles, this interface is defined in the domain/common layer
 */
interface IAppVersionProvider {
    /**
     * Gets the current app version as a string (e.g., "1.0.0")
     * @return The version string from the platform's configuration
     */
    fun getVersion(): String
}

