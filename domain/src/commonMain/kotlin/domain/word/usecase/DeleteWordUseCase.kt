package domain.word.usecase

import core.common.Try
import core.common.UseCase
import domain.widget.IWidgetRefresher
import domain.widget.usecase.GetDailyWidgetDataUseCase
import domain.word.repository.IWordRepository

/**
 * Use case for deleting a single word.
 * If the deleted word is currently displayed on the widget, refreshes
 * the widget with a new word.
 */
class DeleteWordUseCase(
    private val wordRepository: IWordRepository,
    private val widgetRefresher: IWidgetRefresher,
    private val getDailyWidgetDataUseCase: GetDailyWidgetDataUseCase,
) : UseCase<Int, Unit> {
    override suspend operator fun invoke(params: Int): Try<Unit> {
        return wordRepository.deleteWord(params).also { result ->
            if (result.isSuccess && widgetRefresher.getDisplayedWordId() == params) {
                getDailyWidgetDataUseCase(Unit) // fetches new word and pushes to widget
            }
        }
    }
}
