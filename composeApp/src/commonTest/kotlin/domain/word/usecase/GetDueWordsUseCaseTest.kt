package domain.word.usecase

import domain.common.Try
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
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals

class GetDueWordsUseCaseTest {

    private val repository = FakeWordRepository()
    private val useCase = GetDueWordsUseCase(repository)

    @Test
    fun `returns flow from repository`() = runTest {
        val words = listOf(
            createWord(id = 1, original = "Hello"),
            createWord(id = 2, original = "World")
        )
        repository.setDueWords(words)

        val emitted = useCase().first()

        assertEquals(words, emitted)
        assertEquals(1, repository.dueCardsCallCount)
    }

    private fun createWord(
        id: Int,
        original: String,
        translation: String = "translated"
    ) = Word(
        id = id,
        originalWord = original,
        translation = translation,
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.SPANISH,
        level = 0,
        easeFactor = 2.5f,
        interval = 0,
        repetitions = 0,
        lastReviewDate = 0L,
        nextReviewDate = 0L
    )

    private class FakeWordRepository : IWordRepository {
        private val dueWordsFlow = MutableStateFlow<List<Word>>(emptyList())
        var dueCardsCallCount = 0

        fun setDueWords(words: List<Word>) {
            dueWordsFlow.value = words
        }

        override fun getDueCards(): Flow<List<Word>> {
            dueCardsCallCount++
            return dueWordsFlow
        }

        override suspend fun getAllWordsAsync(): List<Word> = dueWordsFlow.value

        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun updateWord(word: Word) {}
        override suspend fun insertWords(words: List<Word>): Int = words.size
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf(DeleteWordsProgress.Completed(0))
        override suspend fun deleteWord(id: Int) {}
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
        override suspend fun getTotalCount(): Int = 0
        override suspend fun getDueCount(): Int = dueWordsFlow.value.size
    }
}

