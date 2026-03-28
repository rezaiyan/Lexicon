package domain.wordrush.usecase

import core.common.Try
import core.common.UseCase
import domain.wordrush.model.WordRushGameRecord
import domain.wordrush.repository.IWordRushRecorder

class RecordWordRushGameUseCase(
    private val recorder: IWordRushRecorder,
) : UseCase<WordRushGameRecord, Unit> {
    override suspend fun invoke(params: WordRushGameRecord): Try<Unit> =
        recorder.recordGame(params)
}
