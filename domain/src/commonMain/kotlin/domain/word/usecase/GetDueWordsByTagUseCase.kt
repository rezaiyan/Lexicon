package domain.word.usecase

import core.common.FlowUseCase
import domain.word.model.Word
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow

class GetDueWordsByTagUseCase(
    private val wordRepository: IWordRepository
) : FlowUseCase<Long, List<Word>> {
    override operator fun invoke(params: Long): Flow<List<Word>> {
        return wordRepository.getDueCardsByTag(params)
    }
}
