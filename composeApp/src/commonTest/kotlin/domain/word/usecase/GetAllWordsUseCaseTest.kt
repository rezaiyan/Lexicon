package domain.word.usecase

import core.common.Try
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAllWordsUseCaseTest {

    private val repository = FakeWordRepository()
    private val useCase = GetAllWordsUseCase(repository)

    @Test
    fun `returns all words flow from repository`() = runTest {
        val words = listOf(
            createWord(id = 1, original = "uno"),
            createWord(id = 2, original = "dos")
        )
        repository.setAllWords(words)

        val emitted = useCase().first()

        assertEquals(words, emitted)
        assertEquals(1, repository.allWordsCallCount)
    }

    private fun createWord(
        id: Int,
        original: String,
        translation: String = "translation"
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
        private val allWordsFlow = MutableStateFlow<List<Word>>(emptyList())
        var allWordsCallCount = 0

        fun setAllWords(words: List<Word>) {
            allWordsFlow.value = words
        }

        override suspend fun getAllWordsAsync(): List<Word> = allWordsFlow.value

        override fun getAllWords(): Flow<List<Word>> {
            allWordsCallCount++
            return allWordsFlow
        }

        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
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
        override suspend fun getTotalCount(): Int = allWordsFlow.value.size
        override suspend fun getDueCount(): Int = 0
        override fun updateWordsLanguages(ids: List<Int>, sourceLanguage: String, targetLanguage: String): Flow<UpdateWordsLanguagesProgress> = flow { emit(UpdateWordsLanguagesProgress.Completed(ids.size)) }
    }
}

