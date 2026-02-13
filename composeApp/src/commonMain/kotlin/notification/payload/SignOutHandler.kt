package notification.payload

import data.storage.SecureStorage
import domain.auth.session.ISessionManager
import domain.settings.repository.ISettingsRepository
import domain.word.repository.IWordRepository

class SignOutHandler(
    private val wordRepository: IWordRepository,
    private val settingsRepository: ISettingsRepository,
    private val secureStorage: SecureStorage,
    private val sessionManager: ISessionManager
) : NotificationPayloadHandler {

    override val type: String = "sign_out"

    override suspend fun handle(data: Map<String, String>) {
        clearAllUserData()
    }

    private suspend fun clearAllUserData() {
        wordRepository.deleteAllWords()
        settingsRepository.clearSettings()
        secureStorage.clearTokens()
        secureStorage.clearDailyInsightData()
        sessionManager.setAuthenticated(false)
    }
}

