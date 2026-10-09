package domain.listening.repository

import core.common.Try
import domain.listening.model.ListeningSettings
import kotlinx.coroutines.flow.Flow

interface IListeningSettingsRepository {
    fun observe(): Flow<ListeningSettings>
    suspend fun save(settings: ListeningSettings): Try<Unit>
}
