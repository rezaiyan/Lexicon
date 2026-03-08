package com.alirezaiyan.vokab.test.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import data.core.database.AppDatabase
import com.alirezaiyan.vokab.test.utils.TestUtils
import com.alirezaiyan.vokab.test.utils.createTestReviewSettingsUseCase
import data.word.repository.WordRepositoryImpl
import domain.word.usecase.ReviewWordUseCase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * Tests for ReviewWordUseCase bucket progression and timing:
 * - Bucket progression through all 7 levels (0-6)
 * - Resting time verification for each level
 * - Level 6 exponential growth and cap
 * - Complete journey tests
 */
@RunWith(AndroidJUnit4::class)
class ReviewWordProgressionTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: WordRepositoryImpl
    private lateinit var reviewUseCase: ReviewWordUseCase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        repository = TestUtils.createWordRepository(database.getDao())
        val reviewSettings = createTestReviewSettingsUseCase()
        reviewUseCase = ReviewWordUseCase(repository, reviewSettings)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ========== Complete Bucket Progression Tests ==========

    @Test
    fun bucketProgression_level0_to_level1() = runTest {
        val word = TestUtils.createWord(id = 1, level = 0, repetitions = 0)
        repository.insertWords(listOf(word))

        val timeBefore = Clock.System.now().toEpochMilliseconds()

        reviewUseCase(word, quality = 1)

        val updated = repository.getWordById(1)!!
        assertEquals(1, updated.level)
        assertEquals(10, updated.interval, "Level 1 interval should be 10 minutes")

        val expectedInterval = 10 * 60 * 1000L
        val actualInterval = updated.nextReviewDate - timeBefore
        val tolerance = 1000L
        assertTrue(
            abs(actualInterval - expectedInterval) < tolerance,
            "Next review should be ~10 minutes away. Expected: $expectedInterval, Actual: $actualInterval"
        )
    }

    @Test
    fun bucketProgression_level1_to_level2() = runTest {
        val word = TestUtils.createWord(id = 1, level = 1, repetitions = 0)
        repository.insertWords(listOf(word))

        val timeBefore = Clock.System.now().toEpochMilliseconds()

        reviewUseCase(word, quality = 1)

        val updated = repository.getWordById(1)!!
        assertEquals(2, updated.level)
        assertEquals(1, updated.interval, "Level 2 interval should be 1 day")

        val expectedInterval = 1 * 24 * 60 * 60 * 1000L
        val actualInterval = updated.nextReviewDate - timeBefore
        val tolerance = 1000L
        assertTrue(
            abs(actualInterval - expectedInterval) < tolerance,
            "Next review should be ~1 day away"
        )
    }

    @Test
    fun bucketProgression_level2_to_level3() = runTest {
        val word = TestUtils.createWord(id = 1, level = 2, repetitions = 0)
        repository.insertWords(listOf(word))

        reviewUseCase(word, quality = 1)

        val updated = repository.getWordById(1)!!
        assertEquals(3, updated.level)
        assertEquals(3, updated.interval, "Level 3 interval should be 3 days")
    }

    @Test
    fun bucketProgression_level3_to_level4() = runTest {
        val word = TestUtils.createWord(id = 1, level = 3, repetitions = 0)
        repository.insertWords(listOf(word))

        reviewUseCase(word, quality = 1)

        val updated = repository.getWordById(1)!!
        assertEquals(4, updated.level)
        assertEquals(7, updated.interval, "Level 4 interval should be 7 days")
    }

    @Test
    fun bucketProgression_level4_to_level5() = runTest {
        val word = TestUtils.createWord(id = 1, level = 4, repetitions = 0)
        repository.insertWords(listOf(word))

        reviewUseCase(word, quality = 1)

        val updated = repository.getWordById(1)!!
        assertEquals(5, updated.level)
        assertEquals(14, updated.interval, "Level 5 interval should be 14 days")
    }

    @Test
    fun bucketProgression_level5_to_level6() = runTest {
        val word = TestUtils.createWord(id = 1, level = 5, repetitions = 0)
        repository.insertWords(listOf(word))

        reviewUseCase(word, quality = 1)

        val updated = repository.getWordById(1)!!
        assertEquals(6, updated.level, "Should reach mastered level")
        assertEquals(30, updated.interval, "Level 6 initial interval should be 30 days")
    }

    @Test
    fun bucketProgression_level6_staysAtLevel6_intervalGrows() = runTest {
        val word = TestUtils.createWord(
            id = 1,
            level = 6,
            repetitions = 0,
            interval = 30,
            easeFactor = 2.0f
        )
        repository.insertWords(listOf(word))

        reviewUseCase(word, quality = 1)

        val updated = repository.getWordById(1)!!
        assertEquals(6, updated.level, "Should stay at level 6 (mastered)")
        assertEquals(1, updated.repetitions, "Repetitions should increment")
        assertTrue(updated.interval > 30, "Interval should grow from 30")
        assertEquals(60, updated.interval, "Interval should be 60 days (30 * 2.0)")
    }

    @Test
    fun bucketProgression_level6_intervalCapAt365Days() = runTest {
        val word = TestUtils.createWord(
            id = 1,
            level = 6,
            repetitions = 1,
            interval = 300,
            easeFactor = 2.5f
        )
        repository.insertWords(listOf(word))

        reviewUseCase(word, quality = 1)

        val updated = repository.getWordById(1)!!
        assertEquals(365, updated.interval, "Interval should be capped at 365 days (1 year)")
    }

    // ========== Resting Time Verification Tests ==========

    @Test
    fun restingTime_level0_is1Minute() = runTest {
        val word = TestUtils.createWord(id = 1, level = 2, repetitions = 0)
        repository.insertWords(listOf(word))

        val timeBefore = Clock.System.now().toEpochMilliseconds()

        reviewUseCase(word, quality = 0)

        val updated = repository.getWordById(1)!!
        val minutesUntilReview = TestUtils.millisToMinutes(
            updated.nextReviewDate - timeBefore
        )
        assertEquals(1L, minutesUntilReview, "Level 0 resting time should be 1 minute")
    }

    @Test
    fun restingTime_level1_is10Minutes() = runTest {
        val word = TestUtils.createWord(id = 1, level = 0, repetitions = 1)
        repository.insertWords(listOf(word))

        val timeBefore = Clock.System.now().toEpochMilliseconds()

        reviewUseCase(word, quality = 1)

        val updated = repository.getWordById(1)!!
        val minutesUntilReview = TestUtils.millisToMinutes(
            updated.nextReviewDate - timeBefore
        )
        assertEquals(10L, minutesUntilReview, "Level 1 resting time should be 10 minutes")
    }

    @Test
    fun restingTime_level2_is1Day() = runTest {
        val word = TestUtils.createWord(id = 1, level = 1, repetitions = 1)
        repository.insertWords(listOf(word))

        val timeBefore = Clock.System.now().toEpochMilliseconds()

        reviewUseCase(word, quality = 1)

        val updated = repository.getWordById(1)!!
        val daysUntilReview = TestUtils.millisToDays(
            updated.nextReviewDate - timeBefore
        )
        assertEquals(1L, daysUntilReview, "Level 2 resting time should be 1 day")
    }

    @Test
    fun restingTime_level3_is3Days() = runTest {
        val word = TestUtils.createWord(id = 1, level = 2, repetitions = 1)
        repository.insertWords(listOf(word))

        val timeBefore = Clock.System.now().toEpochMilliseconds()
        reviewUseCase(word, quality = 1)

        val updated = repository.getWordById(1)!!
        val daysUntilReview = TestUtils.millisToDays(updated.nextReviewDate - timeBefore)
        assertEquals(3L, daysUntilReview, "Level 3 resting time should be 3 days")
    }

    @Test
    fun restingTime_level4_is7Days() = runTest {
        val word = TestUtils.createWord(id = 1, level = 3, repetitions = 1)
        repository.insertWords(listOf(word))

        val timeBefore = Clock.System.now().toEpochMilliseconds()
        reviewUseCase(word, quality = 1)

        val updated = repository.getWordById(1)!!
        val daysUntilReview = TestUtils.millisToDays(updated.nextReviewDate - timeBefore)
        assertEquals(7L, daysUntilReview, "Level 4 resting time should be 7 days")
    }

    @Test
    fun restingTime_level5_is14Days() = runTest {
        val word = TestUtils.createWord(id = 1, level = 4, repetitions = 1)
        repository.insertWords(listOf(word))

        val timeBefore = Clock.System.now().toEpochMilliseconds()
        reviewUseCase(word, quality = 1)

        val updated = repository.getWordById(1)!!
        val daysUntilReview = TestUtils.millisToDays(updated.nextReviewDate - timeBefore)
        assertEquals(14L, daysUntilReview, "Level 5 resting time should be 14 days")
    }

    @Test
    fun restingTime_level6_is30Days() = runTest {
        val word = TestUtils.createWord(id = 1, level = 5, repetitions = 1)
        repository.insertWords(listOf(word))

        val timeBefore = Clock.System.now().toEpochMilliseconds()
        reviewUseCase(word, quality = 1)

        val updated = repository.getWordById(1)!!
        val daysUntilReview = TestUtils.millisToDays(updated.nextReviewDate - timeBefore)
        assertEquals(30L, daysUntilReview, "Level 6 initial resting time should be 30 days")
    }

    // ========== Complete Journey Tests ==========

    @Test
    fun completeJourney_fromLevel0ToLevel6_withAllSuccesses() = runTest {
        var word = TestUtils.createWord(id = 1, level = 0, repetitions = 0)
        repository.insertWords(listOf(word))

        for (i in 0 until 6) {
            word = repository.getWordById(1)!!
            reviewUseCase(word, quality = 1)

            val updated = repository.getWordById(1)!!
            println(
                "Review ${i + 1}: Level ${updated.level}, " +
                    "Reps ${updated.repetitions}, Interval ${updated.interval}"
            )
        }

        val final = repository.getWordById(1)!!
        assertEquals(6, final.level, "After 6 successful reviews, should reach level 6")
    }

    @Test
    fun completeJourney_withForgetting_takesLonger() = runTest {
        var word = TestUtils.createWord(id = 1, level = 0, repetitions = 0)
        repository.insertWords(listOf(word))

        repeat(3) {
            word = repository.getWordById(1)!!
            reviewUseCase(word, quality = 1)
        }

        val atLevel3 = repository.getWordById(1)!!
        assertEquals(3, atLevel3.level)

        reviewUseCase(atLevel3, quality = 0)

        val afterForgetting = repository.getWordById(1)!!
        assertEquals(1, afterForgetting.level, "Should drop from level 3 to level 1")
        assertEquals(0, afterForgetting.repetitions, "Repetitions should reset")

        var reviewCount = 0
        word = afterForgetting
        while (word.level < 6 && reviewCount < 20) {
            word = repository.getWordById(1)!!
            reviewUseCase(word, quality = 1)
            reviewCount++
        }

        val final = repository.getWordById(1)!!
        assertEquals(6, final.level)
        assertTrue(reviewCount >= 5, "Should take 5 more reviews after forgetting")
    }
}
