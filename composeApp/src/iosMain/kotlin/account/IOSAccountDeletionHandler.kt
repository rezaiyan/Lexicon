package account

import data.core.database.LexiconDao
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
    private val dao: LexiconDao,
    private val secureStorage: SecureStorage
) {
    
    /**
     * Clears all user data from local storage
     */
    fun clearUserData() {
        println("🗑️ [IOSAccountDeletionHandler] Clearing user data due to account deletion notification")
        NSLog("🗑️ [IOSAccountDeletionHandler] Clearing user data due to account deletion notification")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Clear all words
                dao.deleteAllWords()
                println("✅ [IOSAccountDeletionHandler] Cleared all words")
                NSLog("✅ [IOSAccountDeletionHandler] Cleared all words")
                
                // Clear settings (including insight data)
                dao.clearSettings()
                println("✅ [IOSAccountDeletionHandler] Cleared settings")
                NSLog("✅ [IOSAccountDeletionHandler] Cleared settings")
                
                // Clear old backups
                dao.clearOldBackups()
                println("✅ [IOSAccountDeletionHandler] Cleared old backups")
                NSLog("✅ [IOSAccountDeletionHandler] Cleared old backups")
                
                // Clear auth tokens
                secureStorage.clearTokens()
                println("✅ [IOSAccountDeletionHandler] Cleared auth tokens")
                NSLog("✅ [IOSAccountDeletionHandler] Cleared auth tokens")
                
                println("✅ [IOSAccountDeletionHandler] Successfully cleared all user data")
                NSLog("✅ [IOSAccountDeletionHandler] Successfully cleared all user data")
                
            } catch (e: Exception) {
                println("❌ [IOSAccountDeletionHandler] Failed to clear user data: ${e.message}")
                NSLog("❌ [IOSAccountDeletionHandler] Failed to clear user data: ${e.message}")
            }
        }
    }
}
