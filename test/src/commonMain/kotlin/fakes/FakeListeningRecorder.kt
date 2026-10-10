package fakes

import core.common.Try
import domain.listening.model.ListeningSessionRecord
import domain.listening.repository.IListeningRecorder

class FakeListeningRecorder : IListeningRecorder {
    val recorded = mutableListOf<ListeningSessionRecord>()

    override suspend fun recordSession(session: ListeningSessionRecord): Try<Unit> {
        recorded += session
        return Try.success(Unit)
    }
}
