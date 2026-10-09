package fakes

import core.common.Try
import domain.listening.model.ListeningSettings
import domain.listening.repository.IListeningSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeListeningSettingsRepository(
    initial: ListeningSettings = ListeningSettings(),
) : IListeningSettingsRepository {
    val settings = MutableStateFlow(initial)

    override fun observe(): Flow<ListeningSettings> = settings

    override suspend fun save(settings: ListeningSettings): Try<Unit> {
        this.settings.value = settings
        return Try.success(Unit)
    }
}
