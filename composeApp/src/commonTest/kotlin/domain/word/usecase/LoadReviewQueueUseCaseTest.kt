package domain.word.usecase

import core.common.Try
import core.common.getOrNull
import domain.word.model.LearningStage
import domain.word.model.ReviewSource
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoadReviewQueueUseCaseTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun testWord(
        id: Int,
        original: String = "word$id",
        stage: LearningStage = LearningStage.LEVEL_0_FRESH,
        tagIds: List<Long> = emptyList(),
    ) = Word(
        id = id,
        originalWord = original,
        translation = "translation$id",
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.ENGLISH,
        level = stage.level,
        nextReviewDate = 0L,
        tagIds = tagIds,
    )

    /**
     * Configurable fake repo used directly by sub-use-cases.
     */
    private inner class ConfigurableWordRepository(
        private val dueCards: List<Word> = emptyList(),
        private val dueCardsByTag: Map<Long, List<Word>> = emptyMap(),
        private val wordsByStage: Map<LearningStage, List<Word>> = emptyMap(),
    ) : IWordRepository {
        override fun getDueCards(): Flow<List<Word>> = flowOf(dueCards)
        override fun getDueCardsByTag(tagId: Long): Flow<List<Word>> =
            flowOf(dueCardsByTag[tagId] ?: emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> =
            flowOf(wordsByStage[stage] ?: emptyList())

        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun insertWords(words: List<Word>): Try<Int> = Try.success(words.size)
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> =
            flowOf(DeleteWordsProgress.Completed(ids.size))
        override fun updateWordsLanguages(
            ids: List<Int>,
            sourceLanguage: String,
            targetLanguage: String,
        ): Flow<UpdateWordsLanguagesProgress> = flow {
            emit(UpdateWordsLanguagesProgress.Completed(ids.size))
        }
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats() = flowOf(domain.word.model.ProgressStats())
        override suspend fun getTotalCount(): Try<Int> = Try.success(0)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
        override suspend fun getNextDueAt(): Try<Long?> = Try.success(null)
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
        override suspend fun updateWordLocal(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun batchSyncWords(words: List<Word>): Try<Unit> = Try.success(Unit)
    }

    private fun buildUseCase(repo: IWordRepository): LoadReviewQueueUseCase {
        val getDueWords = GetDueWordsUseCase(repo)
        val getWordsByStage = GetWordsByStageUseCase(repo)
        val getDueWordsByTag = GetDueWordsByTagUseCase(repo)
        return LoadReviewQueueUseCase(getDueWords, getWordsByStage, getDueWordsByTag)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `DueCards source returns due words`() = runTest {
        val due = listOf(testWord(1), testWord(2))
        val repo = ConfigurableWordRepository(dueCards = due)
        val useCase = buildUseCase(repo)

        val result = useCase(ReviewSource.DueCards)

        assertTrue(result.isSuccess)
        assertEquals(due, result.getOrNull())
    }

    @Test
    fun `ByStage source returns words for that stage`() = runTest {
        val stage = LearningStage.LEVEL_2_FAMILIAR
        val stageWords = listOf(testWord(10, stage = stage), testWord(11, stage = stage))
        val repo = ConfigurableWordRepository(wordsByStage = mapOf(stage to stageWords))
        val useCase = buildUseCase(repo)

        val result = useCase(ReviewSource.ByStage(stage))

        assertTrue(result.isSuccess)
        assertEquals(stageWords, result.getOrNull())
    }

    @Test
    fun `ByTag source returns due words filtered by tagId`() = runTest {
        val tagId = 42L
        val taggedWords = listOf(testWord(20, tagIds = listOf(tagId)), testWord(21, tagIds = listOf(tagId)))
        val repo = ConfigurableWordRepository(dueCardsByTag = mapOf(tagId to taggedWords))
        val useCase = buildUseCase(repo)

        val result = useCase(ReviewSource.ByTag(tagId))

        assertTrue(result.isSuccess)
        assertEquals(taggedWords, result.getOrNull())
    }

    @Test
    fun `ByStageAndTag source returns words matching both stage and tagId`() = runTest {
        val stage = LearningStage.LEVEL_3_BUILDING
        val tagId = 7L
        // Some words with the stage, only some also have the tag
        val matchingWord = testWord(30, stage = stage, tagIds = listOf(tagId))
        val nonMatchingWord = testWord(31, stage = stage, tagIds = listOf(99L))
        val repo = ConfigurableWordRepository(
            wordsByStage = mapOf(stage to listOf(matchingWord, nonMatchingWord))
        )
        val useCase = buildUseCase(repo)

        val result = useCase(ReviewSource.ByStageAndTag(stage, tagId))

        assertTrue(result.isSuccess)
        val words = result.getOrNull()!!
        assertEquals(1, words.size)
        assertEquals(matchingWord, words.first())
    }
}
