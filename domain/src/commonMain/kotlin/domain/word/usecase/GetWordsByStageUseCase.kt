package domain.word.usecase

import domain.word.model.LearningStage
import domain.word.model.Word
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for retrieving words by learning stage
 */
class GetWordsByStageUseCase(
    private val wordRepository: IWordRepository
) {
    operator fun invoke(stage: LearningStage): Flow<List<Word>> {
        return wordRepository.getWordsByStage(stage)
    }
}

