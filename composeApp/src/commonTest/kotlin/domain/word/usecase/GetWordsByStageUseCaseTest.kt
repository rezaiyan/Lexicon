package domain.word.usecase

import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetWordsByStageUseCaseTest {

    private val repository = FakeWordRepository()
    private val useCase = GetWordsByStageUseCase(repository)

    @Test
    fun `returns words for specified learning stage`() = runTest {
        val stage = LearningStage.LEVEL_2_FAMILIAR
        val words = listOf(
            createWord(id = 1, level = stage.level, original = "alpha"),
            createWord(id = 2, level = stage.level, original = "beta")
        )
        repository.setWordsForStage(stage, words)

        val emitted = useCase(stage).first()

        assertEquals(words, emitted)
        assertEquals(1, repository.stageCallCount[stage])
    }

    private fun createWord(
        id: Int,
        level: Int,
        original: String,
        translation: String = "translation"
    ) = Word(
        id = id,
        originalWord = original,
        translation = translation,
        description = "",
        sourceLanguage = "en",
        targetLanguage = "es",
        level = level,
        easeFactor = 2.5f,
        interval = 0,
        repetitions = 0,
        lastReviewDate = 0L,
        nextReviewDate = 0L
    )

    private class FakeWordRepository : IWordRepository {
        private val stageFlows = mutableMapOf<LearningStage, MutableStateFlow<List<Word>>>()
        val stageCallCount = mutableMapOf<LearningStage, Int>()

        fun setWordsForStage(stage: LearningStage, words: List<Word>) {
            val flow = stageFlows.getOrPut(stage) { MutableStateFlow(emptyList()) }
            flow.value = words
        }

        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> {
            stageCallCount[stage] = (stageCallCount[stage] ?: 0) + 1
            return stageFlows.getOrPut(stage) { MutableStateFlow(emptyList()) }
        }

        override suspend fun getAllWordsAsync(): List<Word> = emptyList()

        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun updateWord(word: Word) {}
        override suspend fun insertWords(words: List<Word>): Int = words.size
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf(DeleteWordsProgress.Completed(0))
        override suspend fun deleteWord(id: Int) {}
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun deleteAllWords(): Result<Unit> = Result.success(Unit)
        override suspend fun syncWithRemote(): Result<Unit> = Result.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Result<Unit> = Result.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
        override suspend fun getTotalCount(): Int = 0
        override suspend fun getDueCount(): Int = 0
    }
}

