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
import kotlin.test.Test
import kotlin.test.assertEquals

class GetProgressStatsUseCaseTest {

    private val repository = FakeWordRepository()
    private val useCase = GetProgressStatsUseCase(repository)

    @Test
    fun `returns progress stats emitted by repository`() = runTest {
        val stats = ProgressStats(
            level0Count = 1,
            level1Count = 2,
            level2Count = 3,
            totalWords = 6,
            dueCards = 2
        )
        repository.setStats(stats)

        val emitted = useCase().first()

        assertEquals(stats, emitted)
        assertEquals(1, repository.statsCallCount)
    }

    private class FakeWordRepository : IWordRepository {
        private val statsFlow = MutableStateFlow(ProgressStats())
        var statsCallCount = 0

        fun setStats(stats: ProgressStats) {
            statsFlow.value = stats
        }

        override fun getProgressStats(): Flow<ProgressStats> {
            statsCallCount++
            return statsFlow
        }

        override suspend fun getAllWordsAsync(): List<Word> = emptyList()

        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
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
        override suspend fun getTotalCount(): Int = 0
        override suspend fun getDueCount(): Int = 0
    }
}

