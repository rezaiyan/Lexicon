package com.alirezaiyan.vokab.test.scenario

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import data.core.database.AppDatabase
import com.alirezaiyan.vokab.test.utils.TestUtils
import com.alirezaiyan.vokab.test.utils.createTestReviewSettingsUseCase
import data.word.repository.WordRepositoryImpl
import domain.word.model.LearningStage
import domain.word.usecase.GetWordsByStageUseCase
import domain.word.usecase.ReviewWordUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/**
 * Test to verify word advances after first success
 * 
 * UPDATED BEHAVIOR:
 * Words now advance to the next bucket after just 1 REMEMBERED response.
 * This matches user expectations for faster progression.
 * 
 * BEHAVIOR:
 * - "REMEMBERED": Word advances to next bucket immediately
 * - "FORGOT": Word drops 2 levels
 */
@RunWith(AndroidJUnit4::class)
class FirstSuccessStaysInBucketTest {
    
    private lateinit var database: AppDatabase
    private lateinit var repository: WordRepositoryImpl
    private lateinit var reviewUseCase: ReviewWordUseCase
    private lateinit var getWordsByStageUseCase: GetWordsByStageUseCase
    
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
        getWordsByStageUseCase = GetWordsByStageUseCase(repository)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun firstSuccess_wordAdvancesToNextBucket_immediately() = runTest {
        println("\n" + "═".repeat(70))
        println("USER SCENARIO: Word advances immediately after REMEMBERED")
        println("═".repeat(70))
        
        // GIVEN: NEW word at Level 0 (First Bucket)
        println("\n1️⃣  SETUP: New word in First Bucket (Level 0)")
        val word = TestUtils.createWord(
            id = 0,
            originalWord = "apple",
            translation = "manzana",
            level = 0,
            repetitions = 0  // No previous reviews
        )
        repository.insertWords(listOf(word))
        
        val initialWord = repository.getAllWords().first().first()
        println("   Word: '${initialWord.originalWord}'")
        println("   Level: ${initialWord.level} (First Bucket)")
        println("   Repetitions: ${initialWord.repetitions}")
        
        // Verify word is in Level 0 bucket
        val level0Before = getWordsByStageUseCase(LearningStage.LEVEL_0_FRESH).first()
        assertEquals(1, level0Before.size)
        println("   ✓ Word IS in First Bucket (Level 0)")
        
        // WHEN: User reviews and responds "REMEMBERED"
        println("\n2️⃣  ACTION: User reviews word - responds REMEMBERED")
        reviewUseCase(initialWord, quality = 1)
        println("   ✓ Review completed with quality = 1 (REMEMBERED)")
        
        // THEN: Word advances to Level 1 immediately
        println("\n3️⃣  RESULT: Word advances to Second Bucket immediately")
        val afterReview = repository.getWordById(initialWord.id)!!
        assertEquals(1, afterReview.level, "Word should advance to Level 1")
        assertEquals(0, afterReview.repetitions, "Repetitions reset for new level")
        
        // Verify word is NOW in Level 1 bucket
        val level1After = getWordsByStageUseCase(LearningStage.LEVEL_1_LEARNING).first()
        assertEquals(1, level1After.size)
        
        // Verify word is NOT in Level 0 anymore
        val level0After = getWordsByStageUseCase(LearningStage.LEVEL_0_FRESH).first()
        assertEquals(0, level0After.size)
        
        println("   ✅ Word NOW in Second Bucket (Level 1)")
        println("   ✓ Level: 0 → ${afterReview.level} (advanced!)")
        println("   ✓ Repetitions: ${afterReview.repetitions} (reset)")
        println("   ✓ Removed from First Bucket")
        
        println("\n" + "═".repeat(70))
        println("✅ BEHAVIOR: Word advances after 1 REMEMBERED response")
        println("   REMEMBERED → Advance to next level immediately")
        println("   FORGOT → Drop 2 levels")
        println("═".repeat(70) + "\n")
    }
    
    @Test
    fun forgotAtLevel1_dropsToLevel0() = runTest {
        println("\n" + "═".repeat(70))
        println("SCENARIO: What if user forgets at Level 1?")
        println("═".repeat(70))
        
        // GIVEN: Word at Level 1 (already advanced once)
        println("\n1️⃣  SETUP: Word at Level 1")
        val word = TestUtils.createWord(
            id = 0,
            originalWord = "difficult",
            translation = "difícil",
            level = 1,
            repetitions = 0
        )
        repository.insertWords(listOf(word))
        
        println("   Word: '${word.originalWord}'")
        println("   Level: ${word.level} (Second Bucket)")
        
        // WHEN: User forgets the word
        println("\n2️⃣  ACTION: User forgets the word")
        val currentWord = repository.getAllWords().first().first()
        reviewUseCase(currentWord, quality = 0)
        
        // THEN: Word drops 2 levels (1 - 2 = -1, min 0)
        println("\n3️⃣  RESULT: Word drops back to Level 0")
        val afterForgetting = repository.getWordById(currentWord.id)!!
        assertEquals(0, afterForgetting.level, "Word drops from Level 1 to Level 0")
        assertEquals(0, afterForgetting.repetitions, "Repetitions reset to 0")
        
        println("   ⚠️  Word back in First Bucket (Level 0)")
        println("   ✓ Level: ${word.level} → ${afterForgetting.level} (dropped 2 levels)")
        println("   ✓ Repetitions: ${afterForgetting.repetitions} (reset)")
        println("\n   📖 WHY? Forgetting drops 2 levels and resets progress")
        println("   💡 User must review again to advance")
        
        println("\n" + "═".repeat(70))
        println("✅ BEHAVIOR: FORGOT drops 2 levels, resets repetitions")
        println("═".repeat(70) + "\n")
    }
    
    @Test
    fun oneSuccessRequired_appliesToAllLevels() = runTest {
        println("\n" + "═".repeat(70))
        println("TEST: 1-success requirement applies to ALL levels")
        println("═".repeat(70))
        
        // Test each level 0-5 (level 6 is special)
        for (testLevel in 0..5) {
            println("\n--- Testing Level $testLevel ---")
            
            // GIVEN: Word at current level with 0 repetitions
            val word = TestUtils.createWord(
                id = 0,
                originalWord = "test_word_$testLevel",
                level = testLevel,
                repetitions = 0
            )
            repository.insertWords(listOf(word))
            
            val initialWord = repository.getAllWords().first().first()
            
            // WHEN: First success
            reviewUseCase(initialWord, quality = 1)
            val afterFirst = repository.getWordById(initialWord.id)!!
            
            // THEN: Should ADVANCE to next level immediately
            assertEquals(testLevel + 1, afterFirst.level, "Level $testLevel: Should advance after 1 success")
            assertEquals(0, afterFirst.repetitions, "Reps reset for new level")
            println("  1st success: Level ${testLevel + 1} (advanced!), Reps = 0")
            
            // Clean up for next iteration
            repository.deleteWord(initialWord.id)
        }
        
        println("\n" + "═".repeat(70))
        println("✅ VERIFIED: All levels 0-5 advance after 1 success")
        println("═".repeat(70) + "\n")
    }
}

