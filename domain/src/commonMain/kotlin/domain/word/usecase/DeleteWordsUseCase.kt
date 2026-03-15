package domain.word.usecase

import core.common.FlowUseCase
import domain.widget.IWidgetRefresher
import domain.widget.usecase.GetDailyWidgetDataUseCase
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

/**
 * Use case for deleting multiple words at once using batch operation.
 * If any deleted word is the one currently displayed on the widget,
 * refreshes the widget with a new word.
 */
class DeleteWordsUseCase(
    private val wordRepository: IWordRepository,
    private val widgetRefresher: IWidgetRefresher,
    private val getDailyWidgetDataUseCase: GetDailyWidgetDataUseCase,
) : FlowUseCase<List<Int>, DeleteWordsResult> {
    override operator fun invoke(wordIds: List<Int>): Flow<DeleteWordsResult> {
        if (wordIds.isEmpty()) {
            return flowOf(DeleteWordsResult.Error("No words selected"))
        }

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
            .onEach { result ->
                if (result is DeleteWordsResult.Success) {
                    val displayedId = widgetRefresher.getDisplayedWordId()
                    if (displayedId != null && displayedId in wordIds) {
                        getDailyWidgetDataUseCase(Unit)
                    }
                }
            }
            .onStart {
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
