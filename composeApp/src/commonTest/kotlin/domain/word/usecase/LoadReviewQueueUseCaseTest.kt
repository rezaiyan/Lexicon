package domain.word.usecase

import core.common.Try
import core.common.getOrNull
import domain.focus.model.LearningFocus
import domain.focus.usecase.ObserveLearningFocusUseCase
import fakes.FakeLearningFocusRepository
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.GetDailyGoalWordsUseCase
import domain.settings.model.ThemeMode
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
        language: Language = Language.ENGLISH,
    ) = Word(
        id = id,
        originalWord = original,
        translation = "translation$id",
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = language,
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
        private val allWords: List<Word> = emptyList(),
    ) : IWordRepository {
        override fun getDueCards(): Flow<List<Word>> = flowOf(dueCards)
        override fun getDueCardsByTag(tagId: Long): Flow<List<Word>> =
            flowOf(dueCardsByTag[tagId] ?: emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> =
            flowOf(wordsByStage[stage] ?: emptyList())

        override fun getAllWords(): Flow<List<Word>> = flowOf(allWords)
        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(allWords)
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

    private inner class FakeSettingsRepository(
        private val dailyGoal: Int = Int.MAX_VALUE,
    ) : ISettingsRepository {
        override fun getLanguage(): Flow<Language> = flowOf(Language.ENGLISH)
        override suspend fun setLanguage(language: Language) = Try.success(Unit)
        override fun getThemeMode(): Flow<ThemeMode> = flowOf(ThemeMode.AUTO)
        override suspend fun setThemeMode(mode: ThemeMode) = Try.success(Unit)
        override suspend fun clearSettings() = Try.success(Unit)
        override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(false)
        override suspend fun setNotificationsEnabled(enabled: Boolean) = Try.success(Unit)
        override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(false)
        override suspend fun setReviewRemindersEnabled(enabled: Boolean) = Try.success(Unit)
        override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(false)
        override suspend fun setMotivationalMessagesEnabled(enabled: Boolean) = Try.success(Unit)
        override suspend fun getDailyReminderTime() = Try.success("09:00")
        override suspend fun setDailyReminderTime(time: String) = Try.success(Unit)
        override suspend fun getMinimumDueCards() = Try.success(1)
        override suspend fun setMinimumDueCards(count: Int) = Try.success(Unit)
        override suspend fun getDailyGoalWords() = Try.success(dailyGoal)
        override suspend fun setDailyGoalWords(count: Int) = Try.success(Unit)
    }

    private fun buildUseCase(
        repo: IWordRepository,
        dailyGoal: Int = Int.MAX_VALUE,
        focus: LearningFocus? = null,
    ): LoadReviewQueueUseCase {
        val getDueWords = GetDueWordsUseCase(repo)
        val getWordsByStage = GetWordsByStageUseCase(repo)
        val getDueWordsByTag = GetDueWordsByTagUseCase(repo)
        val getDailyGoalWords = GetDailyGoalWordsUseCase(FakeSettingsRepository(dailyGoal))
        val observeLearningFocus = ObserveLearningFocusUseCase(repo, FakeLearningFocusRepository(focus))
        return LoadReviewQueueUseCase(
            getDueWords,
            getWordsByStage,
            getDueWordsByTag,
            getDailyGoalWords,
            observeLearningFocus,
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `DueCards with Single focus returns only focused language before applying daily goal`() = runTest {
        val german = (1..3).map { testWord(it, language = Language.GERMAN) }
        val spanish = (10..12).map { testWord(it, language = Language.SPANISH) }
        val mixedDue = listOf(spanish[0], german[0], spanish[1], german[1], german[2], spanish[2])
        val repo = ConfigurableWordRepository(dueCards = mixedDue, allWords = german + spanish)

        val result = buildUseCase(repo, dailyGoal = 2, focus = LearningFocus.Single(Language.GERMAN))(
            ReviewSource.DueCards
        )

        assertEquals(listOf(1, 2), result.getOrNull()?.map { it.id })
    }

    @Test
    fun `DueCards with All focus keeps every language`() = runTest {
        val words = listOf(testWord(1, language = Language.GERMAN), testWord(2, language = Language.SPANISH))
        val repo = ConfigurableWordRepository(dueCards = words, allWords = words)

        val result = buildUseCase(repo, focus = LearningFocus.All)(ReviewSource.DueCards)

        assertEquals(listOf(1, 2), result.getOrNull()?.map { it.id })
    }

    @Test
    fun `ByStageAndTag with Single focus drops other languages`() = runTest {
        val stage = LearningStage.LEVEL_2_FAMILIAR
        val words = listOf(
            testWord(1, stage = stage, tagIds = listOf(5L), language = Language.GERMAN),
            testWord(2, stage = stage, tagIds = listOf(5L), language = Language.SPANISH),
        )
        val repo = ConfigurableWordRepository(wordsByStage = mapOf(stage to words), allWords = words)

        val result = buildUseCase(repo, focus = LearningFocus.Single(Language.SPANISH))(
            ReviewSource.ByStageAndTag(stage, 5L)
        )

        assertEquals(listOf(2), result.getOrNull()?.map { it.id })
    }

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

    @Test
    fun `DueCards source caps queue at daily goal`() = runTest {
        val dueWords = listOf(testWord(1), testWord(2), testWord(3), testWord(4), testWord(5))
        val repo = ConfigurableWordRepository(dueCards = dueWords)
        val useCase = buildUseCase(repo, dailyGoal = 3)

        val result = useCase(ReviewSource.DueCards)

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()!!.size)
    }
}
