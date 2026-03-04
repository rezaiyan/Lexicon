package domain.word.usecase

import core.common.FlowUseCase
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Use case for deleting multiple words at once using batch operation
 * Uses Flow for reactive state management and proper sequential execution
 *
 * Flow sequence:
 * 1. Deleting (initial state)
 * 2. DeletingFromBackend (backend deletion in progress)
 * 3. DeletingFromLocal (local database deletion in progress)
 * 4. Success/Error (final state)
 */
class DeleteWordsUseCase(
    private val wordRepository: IWordRepository
) : FlowUseCase<List<Int>, DeleteWordsResult> {
    override operator fun invoke(wordIds: List<Int>): Flow<DeleteWordsResult> {
        if (wordIds.isEmpty()) {
            return flowOf(DeleteWordsResult.Error("No words selected"))
        }
        
        // Chain repository Flow and map progress to result states
        return wordRepository.deleteWords(wordIds)
            .map { progress ->
                when (progress) {
                    is DeleteWordsProgress.DeletingFromBackend -> 
                        DeleteWordsResult.DeletingBackend(progress.count)
                    
                    is DeleteWordsProgress.DeletingFromLocal -> 
                        DeleteWordsResult.DeletingLocal(progress.count)
                    
                    is DeleteWordsProgress.Completed -> 
                        DeleteWordsResult.Success(progress.count)
                    
                    is DeleteWordsProgress.Failed -> 
                        DeleteWordsResult.Error(progress.error)
                }
            }
            .onStart {
                // Emit initial deleting state
                emit(DeleteWordsResult.Deleting(wordIds.size))
            }
    }
}

sealed class DeleteWordsResult {
    data class Deleting(val count: Int) : DeleteWordsResult()
    data class DeletingBackend(val count: Int) : DeleteWordsResult()
    data class DeletingLocal(val count: Int) : DeleteWordsResult()
    data class Success(val count: Int) : DeleteWordsResult()
    data class Error(val message: String) : DeleteWordsResult()
}

