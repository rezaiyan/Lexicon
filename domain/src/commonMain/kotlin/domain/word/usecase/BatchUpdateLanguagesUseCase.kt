package domain.word.usecase

import core.common.FlowUseCase
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Use case for batch updating source/target languages on multiple words
 * Uses Flow for reactive state management and proper sequential execution
 *
 * Flow sequence:
 * 1. Updating (initial state)
 * 2. UpdatingBackend (backend update in progress)
 * 3. UpdatingLocal (local database update in progress)
 * 4. Success/Error (final state)
 */
class BatchUpdateLanguagesUseCase(
    private val wordRepository: IWordRepository
) : FlowUseCase<BatchUpdateLanguagesUseCase.Params, BatchUpdateLanguagesResult> {
    data class Params(val wordIds: List<Int>, val sourceLanguage: String, val targetLanguage: String)

    override operator fun invoke(params: Params) = invoke(params.wordIds, params.sourceLanguage, params.targetLanguage)

    operator fun invoke(
        wordIds: List<Int>,
        sourceLanguage: String,
        targetLanguage: String
    ): Flow<BatchUpdateLanguagesResult> {
        if (wordIds.isEmpty()) {
            return flowOf(BatchUpdateLanguagesResult.Error("No words selected"))
        }

        return wordRepository.updateWordsLanguages(wordIds, sourceLanguage, targetLanguage)
            .map { progress ->
                when (progress) {
                    is UpdateWordsLanguagesProgress.UpdatingBackend ->
                        BatchUpdateLanguagesResult.UpdatingBackend(progress.count)

                    is UpdateWordsLanguagesProgress.UpdatingLocal ->
                        BatchUpdateLanguagesResult.UpdatingLocal(progress.count)

                    is UpdateWordsLanguagesProgress.Completed ->
                        BatchUpdateLanguagesResult.Success(progress.count)

                    is UpdateWordsLanguagesProgress.Failed ->
                        BatchUpdateLanguagesResult.Error(progress.error)
                }
            }
            .onStart {
                emit(BatchUpdateLanguagesResult.Updating(wordIds.size))
            }
    }
}

sealed class BatchUpdateLanguagesResult {
    data class Updating(val count: Int) : BatchUpdateLanguagesResult()
    data class UpdatingBackend(val count: Int) : BatchUpdateLanguagesResult()
    data class UpdatingLocal(val count: Int) : BatchUpdateLanguagesResult()
    data class Success(val count: Int) : BatchUpdateLanguagesResult()
    data class Error(val message: String) : BatchUpdateLanguagesResult()
}
