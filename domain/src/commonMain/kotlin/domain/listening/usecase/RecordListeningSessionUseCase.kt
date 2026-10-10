package domain.listening.usecase

import core.common.Try
import core.common.UseCase
import domain.listening.model.ListeningSessionRecord
import domain.listening.repository.IListeningRecorder

/** Stores a listening session for study insights. Sessions where nothing was heard are dropped. */
class RecordListeningSessionUseCase(
    private val recorder: IListeningRecorder,
) : UseCase<ListeningSessionRecord, Unit> {
    override suspend fun invoke(params: ListeningSessionRecord): Try<Unit> =
        if (params.heardWords.isEmpty()) Try.success(Unit) else recorder.recordSession(params)
}
