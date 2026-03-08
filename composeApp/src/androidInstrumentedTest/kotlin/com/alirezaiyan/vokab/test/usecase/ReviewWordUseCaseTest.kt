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
 * Comprehensive tests for ReviewWordUseCase
 * Tests all review scenarios including:
 * - FORGOT response (quality = 0) - drops 2 levels
 * - REMEMBERED response (quality = 1) - advances with 2+ successes
 * - Bucket progression through all 7 levels (0-6)
 * - Resting time verification for each level
 * - Ease factor changes
 * - Repetition counting
 * - Level 6 exponential growth
 */
@RunWith(AndroidJUnit4::class)
class ReviewWordUseCaseTest {
    
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
    
    // ========== FORGOT Response Tests (quality = 0) ==========
    
    @Test
    fun forgotResponse_level0_staysAtLevel0() = runTest {
        // Given: A word at level 0
        val word = TestUtils.createWord(id = 1, level = 0, repetitions = 0)
        repository.insertWords(listOf(word))
        
        // When: User forgets (quality = 0)
        reviewUseCase(word, quality = 0)
        
        // Then: Should stay at level 0 (can't go below 0)
        val updated = repository.getWordById(1)!!
        assertEquals(0, updated.level, "Word should stay at level 0")
        assertEquals(0, updated.repetitions, "Repetitions should reset to 0")
        assertTrue(updated.easeFactor < word.easeFactor, "Ease factor should decrease")
    }
    
    @Test
    fun forgotResponse_level2_dropsTo0() = runTest {
        // Given: A word at level 2
        val word = TestUtils.createWord(id = 1, level = 2, repetitions = 1, easeFactor = 2.5f)
        repository.insertWords(listOf(word))
        
        // When: User forgets
        reviewUseCase(word, quality = 0)
        
        // Then: Should drop by 2 levels to level 0
        val updated = repository.getWordById(1)!!
        assertEquals(0, updated.level, "Word should drop from level 2 to 0")
        assertEquals(0, updated.repetitions, "Repetitions should reset")
        assertEquals(2.3f, updated.easeFactor, 0.01f, "Ease factor should decrease by 0.2")
    }
    
    @Test
    fun forgotResponse_level5_dropsTo3() = runTest {
        // Given: A word at level 5
        val word = TestUtils.createWord(id = 1, level = 5, repetitions = 3, easeFactor = 2.5f)
        repository.insertWords(listOf(word))
        
        // When: User forgets
        reviewUseCase(word, quality = 0)
        
        // Then: Should drop by 2 levels to level 3
        val updated = repository.getWordById(1)!!
        assertEquals(3, updated.level, "Word should drop from level 5 to 3")
        assertEquals(0, updated.repetitions, "Repetitions should reset")
        assertTrue(updated.easeFactor < word.easeFactor)
    }
    
    @Test
    fun forgotResponse_level6_dropsTo4() = runTest {
        // Given: A mastered word at level 6
        val word = TestUtils.createWord(id = 1, level = 6, repetitions = 5, easeFactor = 2.5f)
        repository.insertWords(listOf(word))
        
        // When: User forgets
        reviewUseCase(word, quality = 0)
        
        // Then: Should drop by 2 levels to level 4
        val updated = repository.getWordById(1)!!
        assertEquals(4, updated.level, "Word should drop from level 6 to 4")
        assertEquals(0, updated.repetitions, "Repetitions should reset")
    }
    
    @Test
    fun forgotResponse_easeFactorMinimumIs1_3() = runTest {
        // Given: A word with low ease factor
        val word = TestUtils.createWord(id = 1, level = 2, easeFactor = 1.4f)
        repository.insertWords(listOf(word))
        
        // When: User forgets (would go below 1.3)
        reviewUseCase(word, quality = 0)
        
        // Then: Ease factor should not go below 1.3
        val updated = repository.getWordById(1)!!
        assertTrue(updated.easeFactor >= 1.3f, "Ease factor should not go below 1.3")
        assertEquals(1.3f, updated.easeFactor, 0.01f)
    }
    
    // ========== REMEMBERED Response Tests (quality = 1) ==========
    
    @Test
    fun rememberedResponse_firstSuccess_advancesToNextLevel() = runTest {
        // Given: A word at level 0 with 0 repetitions
        val word = TestUtils.createWord(id = 1, level = 0, repetitions = 0)
        repository.insertWords(listOf(word))
        
        // When: User remembers (first success)
        reviewUseCase(word, quality = 1)
        
        // Then: Should advance immediately (1 success needed)
        val updated = repository.getWordById(1)!!
        assertEquals(1, updated.level, "Should advance to level 1 after 1 success")
        assertEquals(0, updated.repetitions, "Repetitions reset for new level")
    }
    
    @Test
    fun rememberedResponse_alwaysAdvances_withOneSuccess() = runTest {
        // Given: A word at level 0 with 0 repetitions
        val word = TestUtils.createWord(id = 1, level = 0, repetitions = 0, easeFactor = 2.4f)
        repository.insertWords(listOf(word))
        
        // When: User remembers (first success)
        reviewUseCase(word, quality = 1)
        
        // Then: Should advance to level 1 immediately
        val updated = repository.getWordById(1)!!
        assertEquals(1, updated.level, "Should advance from level 0 to level 1 after 1 success")
        assertEquals(0, updated.repetitions, "Repetitions should reset to 0 for new level")
        assertTrue(updated.easeFactor >= word.easeFactor, "Ease factor should increase or stay at max (2.5)")
        assertEquals(2.5f, updated.easeFactor, 0.01f, "Ease factor should reach 2.5")
    }
    
    @Test
    fun rememberedResponse_easeFactorMaximumIs2_5() = runTest {
        // Given: A word with high ease factor
        val word = TestUtils.createWord(id = 1, level = 2, repetitions = 1, easeFactor = 2.5f)
        repository.insertWords(listOf(word))
        
        // When: User remembers (would go above 2.5)
        reviewUseCase(word, quality = 1)
        
        // Then: Ease factor should not exceed 2.5
        val updated = repository.getWordById(1)!!
        assertTrue(updated.easeFactor <= 2.5f, "Ease factor should not exceed 2.5")
        assertEquals(2.5f, updated.easeFactor, 0.01f)
    }
    
    // ========== Complete Bucket Progression Tests ==========
    
    @Test
    fun bucketProgression_level0_to_level1() = runTest {
        // Given: Word at level 0, 0 repetitions
        val word = TestUtils.createWord(id = 1, level = 0, repetitions = 0)
        repository.insertWords(listOf(word))
        
        val timeBefore = Clock.System.now().toEpochMilliseconds()
        
        // When: First success
        reviewUseCase(word, quality = 1)
        
        // Then: Should advance to level 1 with 10-minute interval
        val updated = repository.getWordById(1)!!
        assertEquals(1, updated.level)
        assertEquals(10, updated.interval, "Level 1 interval should be 10 minutes")
        
        // Verify next review is ~10 minutes from now
        val expectedInterval = 10 * 60 * 1000L // 10 minutes in milliseconds
        val actualInterval = updated.nextReviewDate - timeBefore
        val tolerance = 1000L // 1 second tolerance
        assertTrue(
            abs(actualInterval - expectedInterval) < tolerance,
            "Next review should be ~10 minutes away. Expected: $expectedInterval, Actual: $actualInterval"
        )
    }
    
    @Test
    fun bucketProgression_level1_to_level2() = runTest {
        // Given: Word at level 1, 0 repetitions
        val word = TestUtils.createWord(id = 1, level = 1, repetitions = 0)
        repository.insertWords(listOf(word))
        
        val timeBefore = Clock.System.now().toEpochMilliseconds()
        
        // When: First success
        reviewUseCase(word, quality = 1)
        
        // Then: Should advance to level 2 with 1-day interval
        val updated = repository.getWordById(1)!!
        assertEquals(2, updated.level)
        assertEquals(1, updated.interval, "Level 2 interval should be 1 day")
        
        // Verify next review is ~1 day from now
        val expectedInterval = 1 * 24 * 60 * 60 * 1000L // 1 day in milliseconds
        val actualInterval = updated.nextReviewDate - timeBefore
        val tolerance = 1000L
        assertTrue(
            abs(actualInterval - expectedInterval) < tolerance,
            "Next review should be ~1 day away"
        )
    }
    
    @Test
    fun bucketProgression_level2_to_level3() = runTest {
        // Given: Word at level 2
        val word = TestUtils.createWord(id = 1, level = 2, repetitions = 0)
        repository.insertWords(listOf(word))
        
        // When: First success
        reviewUseCase(word, quality = 1)
        
        // Then: Should advance to level 3 with 3-day interval
        val updated = repository.getWordById(1)!!
        assertEquals(3, updated.level)
        assertEquals(3, updated.interval, "Level 3 interval should be 3 days")
    }
    
    @Test
    fun bucketProgression_level3_to_level4() = runTest {
        // Given: Word at level 3
        val word = TestUtils.createWord(id = 1, level = 3, repetitions = 0)
        repository.insertWords(listOf(word))
        
        // When: First success
        reviewUseCase(word, quality = 1)
        
        // Then: Should advance to level 4 with 7-day interval
        val updated = repository.getWordById(1)!!
        assertEquals(4, updated.level)
        assertEquals(7, updated.interval, "Level 4 interval should be 7 days")
    }
    
    @Test
    fun bucketProgression_level4_to_level5() = runTest {
        // Given: Word at level 4
        val word = TestUtils.createWord(id = 1, level = 4, repetitions = 0)
        repository.insertWords(listOf(word))
        
        // When: First success
        reviewUseCase(word, quality = 1)
        
        // Then: Should advance to level 5 with 14-day interval
        val updated = repository.getWordById(1)!!
        assertEquals(5, updated.level)
        assertEquals(14, updated.interval, "Level 5 interval should be 14 days")
    }
    
    @Test
    fun bucketProgression_level5_to_level6() = runTest {
        // Given: Word at level 5
        val word = TestUtils.createWord(id = 1, level = 5, repetitions = 0)
        repository.insertWords(listOf(word))
        
        // When: First success
        reviewUseCase(word, quality = 1)
        
        // Then: Should advance to level 6 (Mastered) with 30-day interval
        val updated = repository.getWordById(1)!!
        assertEquals(6, updated.level, "Should reach mastered level")
        assertEquals(30, updated.interval, "Level 6 initial interval should be 30 days")
    }
    
    @Test
    fun bucketProgression_level6_staysAtLevel6_intervalGrows() = runTest {
        // Given: A mastered word at level 6
        val word = TestUtils.createWord(
            id = 1, 
            level = 6, 
            repetitions = 0, 
            interval = 30,
            easeFactor = 2.0f
        )
        repository.insertWords(listOf(word))
        
        // When: Success at level 6 (advances repetitions)
        reviewUseCase(word, quality = 1)
        
        // Then: Should stay at level 6 but interval should grow exponentially
        val updated = repository.getWordById(1)!!
        assertEquals(6, updated.level, "Should stay at level 6 (mastered)")
        assertEquals(1, updated.repetitions, "Repetitions should increment")
        assertTrue(updated.interval > 30, "Interval should grow from 30")
        // interval = 30 * 2.0 (easeFactor) = 60
        assertEquals(60, updated.interval, "Interval should be 60 days (30 * 2.0)")
    }
    
    @Test
    fun bucketProgression_level6_intervalCapAt365Days() = runTest {
        // Given: A mastered word with very large interval
        val word = TestUtils.createWord(
            id = 1, 
            level = 6, 
            repetitions = 1, 
            interval = 300, // Very large interval
            easeFactor = 2.5f
        )
        repository.insertWords(listOf(word))
        
        // When: Another success (would calculate 300 * 2.5 = 750 days)
        reviewUseCase(word, quality = 1)
        
        // Then: Interval should be capped at 365 days
        val updated = repository.getWordById(1)!!
        assertEquals(365, updated.interval, "Interval should be capped at 365 days (1 year)")
    }
    
    // ========== Resting Time Verification Tests ==========
    
    @Test
    fun restingTime_level0_is1Minute() = runTest {
        // Given: Word advancing to level 0 (after forgot)
        val word = TestUtils.createWord(id = 1, level = 2, repetitions = 0)
        repository.insertWords(listOf(word))
        
        val timeBefore = Clock.System.now().toEpochMilliseconds()
        
        // When: Forgot (drops to level 0)
        reviewUseCase(word, quality = 0)
        
        // Then: Next review should be 1 minute away
        val updated = repository.getWordById(1)!!
        val minutesUntilReview = TestUtils.millisToMinutes(
            updated.nextReviewDate - timeBefore
        )
        assertEquals(1L, minutesUntilReview, "Level 0 resting time should be 1 minute")
    }
    
    @Test
    fun restingTime_level1_is10Minutes() = runTest {
        // Given: Word at level 0 with 1 repetition
        val word = TestUtils.createWord(id = 1, level = 0, repetitions = 1)
        repository.insertWords(listOf(word))
        
        val timeBefore = Clock.System.now().toEpochMilliseconds()
        
        // When: Advances to level 1
        reviewUseCase(word, quality = 1)
        
        // Then: Next review should be 10 minutes away
        val updated = repository.getWordById(1)!!
        val minutesUntilReview = TestUtils.millisToMinutes(
            updated.nextReviewDate - timeBefore
        )
        assertEquals(10L, minutesUntilReview, "Level 1 resting time should be 10 minutes")
    }
    
    @Test
    fun restingTime_level2_is1Day() = runTest {
        // Given: Word at level 1 with 1 repetition
        val word = TestUtils.createWord(id = 1, level = 1, repetitions = 1)
        repository.insertWords(listOf(word))
        
        val timeBefore = Clock.System.now().toEpochMilliseconds()
        
        // When: Advances to level 2
        reviewUseCase(word, quality = 1)
        
        // Then: Next review should be 1 day away
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
        // Given: A new word at level 0
        var word = TestUtils.createWord(id = 1, level = 0, repetitions = 0)
        repository.insertWords(listOf(word))
        
        // Journey through all levels with successful reviews
        // With 1 success needed per level: 0→1→2→3→4→5→6 = 6 reviews
        for (i in 0 until 6) {
            word = repository.getWordById(1)!!
            reviewUseCase(word, quality = 1)
            
            val updated = repository.getWordById(1)!!
            println("Review ${i + 1}: Level ${updated.level}, Reps ${updated.repetitions}, Interval ${updated.interval}")
        }
        
        // Then: Should be at level 6 (mastered)
        val final = repository.getWordById(1)!!
        assertEquals(6, final.level, "After 6 successful reviews, should reach level 6")
    }
    
    @Test
    fun completeJourney_withForgetting_takesLonger() = runTest {
        // Given: A new word
        var word = TestUtils.createWord(id = 1, level = 0, repetitions = 0)
        repository.insertWords(listOf(word))
        
        // Progress to level 3 (3 reviews: 0→1→2→3)
        repeat(3) {
            word = repository.getWordById(1)!!
            reviewUseCase(word, quality = 1)
        }
        
        val atLevel3 = repository.getWordById(1)!!
        assertEquals(3, atLevel3.level)
        
        // Forget at level 3 (drops to level 1)
        reviewUseCase(atLevel3, quality = 0)
        
        val afterForgetting = repository.getWordById(1)!!
        assertEquals(1, afterForgetting.level, "Should drop from level 3 to level 1")
        assertEquals(0, afterForgetting.repetitions, "Repetitions should reset")
        
        // Now need more reviews to get back to level 6 (1→2→3→4→5→6 = 5 reviews)
        var reviewCount = 0
        word = afterForgetting
        while (word.level < 6 && reviewCount < 20) {
            word = repository.getWordById(1)!!
            reviewUseCase(word, quality = 1)
            reviewCount++
        }
        
        val final = repository.getWordById(1)!!
        assertEquals(6, final.level)
        // Total: 3 + 1 (forgot) + 5 = 9 reviews
        // Without forgetting: 6 reviews
        assertTrue(reviewCount >= 5, "Should take 5 more reviews after forgetting")
    }
    
    // ========== Edge Cases ==========
    
    @Test
    fun invalidQuality_treatedAsForgot() = runTest {
        // Given: A word at level 2
        val word = TestUtils.createWord(id = 1, level = 2, repetitions = 0)
        repository.insertWords(listOf(word))
        
        // When: Invalid quality value
        reviewUseCase(word, quality = 99)
        
        // Then: Should treat as FORGOT (drop 2 levels)
        val updated = repository.getWordById(1)!!
        assertEquals(0, updated.level, "Invalid quality should be treated as FORGOT")
    }
}

