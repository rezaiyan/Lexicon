package domain.word.usecase

import core.common.getOrThrow
import domain.word.model.ProgressStats
import domain.word.model.ProgressTier
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EvaluateProgressUseCaseTest {

    private val useCase = EvaluateProgressUseCase()

    @Test
    fun `empty stats returns EMPTY tier`() = runTest {
        val stats = ProgressStats(totalWords = 0)

        val result = useCase(stats)

        assertTrue(result.isSuccess)
        val eval = result.getOrThrow()
        assertEquals(ProgressTier.EMPTY, eval.tier)
        assertEquals(0f, eval.progressFraction)
        assertEquals(0, eval.progressPercent)
    }

    @Test
    fun `all words at level 6 returns MASTERED tier`() = runTest {
        val stats = ProgressStats(level6Count = 10, totalWords = 10)

        val result = useCase(stats)

        assertTrue(result.isSuccess)
        val eval = result.getOrThrow()
        assertEquals(ProgressTier.MASTERED, eval.tier)
        assertEquals(100, eval.progressPercent)
    }

    @Test
    fun `all words at level 0 returns GETTING_STARTED tier`() = runTest {
        val stats = ProgressStats(level0Count = 10, totalWords = 10)

        val result = useCase(stats)

        assertTrue(result.isSuccess)
        val eval = result.getOrThrow()
        assertEquals(ProgressTier.GETTING_STARTED, eval.tier)
        assertEquals(0, eval.progressPercent)
    }

    @Test
    fun `mixed levels returns correct weighted score`() = runTest {
        // 5 words at level 3 = 15, 5 words at level 6 = 30, total weight = 45
        // max possible = 10 * 6 = 60, fraction = 45/60 = 0.75 = 75%
        val stats = ProgressStats(level3Count = 5, level6Count = 5, totalWords = 10)

        val result = useCase(stats)

        assertTrue(result.isSuccess)
        val eval = result.getOrThrow()
        assertEquals(ProgressTier.STRONG, eval.tier)
        assertEquals(75, eval.progressPercent)
    }

    @Test
    fun `halfway progress returns HALFWAY tier`() = runTest {
        // 10 words at level 3 = 30, max = 10*6 = 60, fraction = 0.5 = 50%
        val stats = ProgressStats(level3Count = 10, totalWords = 10)

        val result = useCase(stats)

        assertTrue(result.isSuccess)
        assertEquals(ProgressTier.HALFWAY, result.getOrThrow().tier)
        assertEquals(50, result.getOrThrow().progressPercent)
    }

    @Test
    fun `progressing tier at 25 percent`() = runTest {
        // 10 words at level 1.5 avg -> let's use 5 at level 1 + 5 at level 2
        // (5*1 + 5*2) / (10*6) = 15/60 = 25%
        val stats = ProgressStats(level1Count = 5, level2Count = 5, totalWords = 10)

        val result = useCase(stats)

        assertTrue(result.isSuccess)
        assertEquals(ProgressTier.PROGRESSING, result.getOrThrow().tier)
        assertEquals(25, result.getOrThrow().progressPercent)
    }

    @Test
    fun `building tier at 10 percent`() = runTest {
        // 6 words at level 1 = 6, max = 10*6 = 60, fraction = 6/60 = 10%
        val stats = ProgressStats(level0Count = 4, level1Count = 6, totalWords = 10)

        val result = useCase(stats)

        assertTrue(result.isSuccess)
        assertEquals(ProgressTier.BUILDING, result.getOrThrow().tier)
        assertEquals(10, result.getOrThrow().progressPercent)
    }

    @Test
    fun `almost master tier at 90 percent`() = runTest {
        // Need 90%: 10 words, need score of 54 out of 60
        // 6 at level 6 = 36, 4 at level 4.5... let's use 4 at level 5 = 20, total = 36+20 = 56 -> 93%
        // Actually: 4*5 + 6*6 = 20+36 = 56, 56/60 = 93%
        val stats = ProgressStats(level5Count = 4, level6Count = 6, totalWords = 10)

        val result = useCase(stats)

        assertTrue(result.isSuccess)
        assertEquals(ProgressTier.ALMOST_MASTER, result.getOrThrow().tier)
    }

    @Test
    fun `fraction is clamped to 0-1 range`() = runTest {
        val stats = ProgressStats(level6Count = 10, totalWords = 10)

        val result = useCase(stats)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().progressFraction <= 1.0f)
        assertTrue(result.getOrThrow().progressFraction >= 0.0f)
    }
}
