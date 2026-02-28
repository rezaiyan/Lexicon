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
 * Test exact user scenario:
 * 
 * SCENARIO:
 * 1. Import word1, word2, word3 (all new, level 0)
 * 2. Start review session
 * 3. word1: REMEMBERED
 * 4. word2: FORGOT  
 * 5. word3: FORGOT
 * 6. Close review
 * 
 * EXPECTED RESULT:
 * - First bucket (Level 0): word2, word3
 * - Second bucket (Level 1): word1
 * 
 * Let's verify if this is what actually happens or if there's a bug.
 */
@RunWith(AndroidJUnit4::class)
class ThreeWordsReviewScenarioTest {
    
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
    fun userScenario_word1Remembered_word2And3Forgot() = runTest {
        println("\n" + "═".repeat(70))
        println("USER SCENARIO: 3 words - 1 remembered, 2 forgot")
        println("═".repeat(70))
        
        // STEP 1: Import 3 words
        println("\n1  IMPORT: 3 new words")
        val words = listOf(
            TestUtils.createWord(id = 0, originalWord = "word1", translation = "palabra1", level = 0, repetitions = 0),
            TestUtils.createWord(id = 0, originalWord = "word2", translation = "palabra2", level = 0, repetitions = 0),
            TestUtils.createWord(id = 0, originalWord = "word3", translation = "palabra3", level = 0, repetitions = 0)
        )
        repository.insertWords(words)
        println("    word1, word2, word3 imported")
        println("    All at Level 0 (First Bucket)")
        
        // Verify all in first bucket
        val firstBucketBefore = getWordsByStageUseCase(LearningStage.LEVEL_0_FRESH).first()
        assertEquals(3, firstBucketBefore.size)
        println("    First bucket: 3 words")
        
        // STEP 2: Start review session
        println("\n2  START REVIEW SESSION")
        val allWords = repository.getAllWords().first().sortedBy { it.originalWord }
        val word1 = allWords[0]
        val word2 = allWords[1]
        val word3 = allWords[2]
        
        println("   Reviewing word1...")
        println("   Reviewing word2...")
        println("   Reviewing word3...")
        
        // STEP 3: Review each word
        println("\n3  REVIEW RESULTS:")
        
        // word1: REMEMBERED
        reviewUseCase(word1, quality = 1)
        println("   word1: REMEMBERED (quality = 1)")
        
        // word2: FORGOT
        reviewUseCase(word2, quality = 0)
        println("   word2: FORGOT (quality = 0)")
        
        // word3: FORGOT
        reviewUseCase(word3, quality = 0)
        println("   word3: FORGOT (quality = 0)")
        
        // STEP 4: Check word states after review
        println("\n4  WORD STATES AFTER REVIEW:")
        val word1After = repository.getWordById(word1.id)!!
        val word2After = repository.getWordById(word2.id)!!
        val word3After = repository.getWordById(word3.id)!!
        
        println("   word1: Level ${word1After.level}, Reps ${word1After.repetitions}")
        println("   word2: Level ${word2After.level}, Reps ${word2After.repetitions}")
        println("   word3: Level ${word3After.level}, Reps ${word3After.repetitions}")
        
        // STEP 5: Verify bucket distribution
        println("\n5  VERIFY BUCKET DISTRIBUTION:")
        
        val firstBucketAfter = getWordsByStageUseCase(LearningStage.LEVEL_0_FRESH).first()
        val secondBucketAfter = getWordsByStageUseCase(LearningStage.LEVEL_1_LEARNING).first()
        
        println("\n    ACTUAL RESULT:")
        println("   First Bucket (Level 0): ${firstBucketAfter.map { it.originalWord }}")
        println("   Second Bucket (Level 1): ${secondBucketAfter.map { it.originalWord }}")
        
        println("\n    USER EXPECTED:")
        println("   First Bucket (Level 0): [word2, word3]")
        println("   Second Bucket (Level 1): [word1]")
        
        // ANALYZE THE RESULTS
        println("\n6  ANALYSIS:")
        
        // word1 REMEMBERED: Level 0, rep 0 -> Level 0, rep 1
        // Needs 2 successes to advance, so STAYS at Level 0
        if (word1After.level == 0) {
            println("\n     CURRENT BEHAVIOR:")
            println("   word1 (REMEMBERED) STAYS in First Bucket")
            println("   Reason: Needs 2 consecutive successes to advance")
            println("   Current state: 1 success, needs 1 more")
            println("\n    POTENTIAL BUG: User expects word1 in Second Bucket")
            println("   Should words advance after FIRST success?")
        } else {
            println("\n    word1 advanced to Level ${word1After.level}")
        }
        
        // Verify actual state
        assertEquals(1, word1After.level, "word1 should advance to Level 1 (1 success needed)")
        assertEquals(0, word1After.repetitions, "word1 reps reset for new level")
        
        assertEquals(0, word2After.level, "word2 should stay at Level 0")
        assertEquals(0, word2After.repetitions, "word2 reps reset to 0")
        
        assertEquals(0, word3After.level, "word3 should stay at Level 0")
        assertEquals(0, word3After.repetitions, "word3 reps reset to 0")
        
        // word1 in second bucket, word2 & word3 in first bucket
        assertEquals(2, firstBucketAfter.size, "First Bucket should have word2, word3")
        assertEquals(1, secondBucketAfter.size, "Second Bucket should have word1")
        
        println("\n" + "═".repeat(70))
        println(" USER EXPECTATION MET:")
        println("   Expected: word1 in Second Bucket ")
        println("   Expected: word2, word3 in First Bucket ")
        println("\n FIX APPLIED: Words now advance after FIRST success")
        println("   REMEMBERED once → Advance to next level")
        println("   FORGOT → Drop 2 levels and reset")
        println("═".repeat(70) + "\n")
    }
}

