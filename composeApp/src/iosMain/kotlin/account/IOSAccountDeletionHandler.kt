package account

import data.core.database.LexiconQueries
import data.storage.SecureStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.Foundation.NSLog

/**
 * iOS-specific account deletion handler
 * Provides a way to clear user data when account deletion notification is received
 */
class IOSAccountDeletionHandler(
    private val queries: LexiconQueries,
    private val secureStorage: SecureStorage
) {

    /**
     * Clears all user data from local storage
     */
    fun clearUserData() {
        println("[IOSAccountDeletionHandler] Clearing user data due to account deletion notification")
        NSLog("[IOSAccountDeletionHandler] Clearing user data due to account deletion notification")

        CoroutineScope(Dispatchers.Main).launch {
            // Clear all words
            queries.deleteAllWords()
            println("[IOSAccountDeletionHandler] Cleared all words")
            NSLog("[IOSAccountDeletionHandler] Cleared all words")

            // Clear settings
            queries.clearSettings()
            println("[IOSAccountDeletionHandler] Cleared settings")
            NSLog("[IOSAccountDeletionHandler] Cleared settings")

            // Clear auth tokens
            secureStorage.clearTokens()
            println("[IOSAccountDeletionHandler] Cleared auth tokens")
            NSLog("[IOSAccountDeletionHandler] Cleared auth tokens")

            println("[IOSAccountDeletionHandler] Successfully cleared all user data")
            NSLog("[IOSAccountDeletionHandler] Successfully cleared all user data")
        }
    }
}
