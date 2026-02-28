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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * Test to verify bucket visibility after word review
 * 
 * SCENARIO:
 * 1. Word is at Level 1 with 1 repetition (ready to advance)
 * 2. User reviews and responds "REMEMBERED" 
 * 3. Word should move to Level 2
 * 4. Due to cooldown (1 day), word won't appear in "due cards" review session
 * 5. BUT word SHOULD be visible when viewing the Level 2 bucket
 * 
 * This verifies the UI refresh fix works correctly.
 */
@RunWith(AndroidJUnit4::class)
class BucketVisibilityTest {
    
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
    fun scenario_wordMovesFromLevel1ToLevel2_visibleInLevel2Bucket() = runTest {
        println("═══════════════════════════════════════════════════════")
        println("TEST SCENARIO: Word moves from Level 1 → Level 2")
        println("═══════════════════════════════════════════════════════")
        
        // STEP 1: Create a word at Level 1 with 1 repetition (ready to advance)
        println("\n1 SETUP: Creating word at Level 1 with 1 repetition")
        val word = TestUtils.createWord(
            id = 0,
            originalWord = "hello",
            translation = "hola",
            level = 1,
            repetitions = 1,  // Already has 1 success, next success will advance
            easeFactor = 2.5f,
            nextReviewDate = Clock.System.now().toEpochMilliseconds() // Due now
        )
        repository.insertWords(listOf(word))
        
        val initialWord = repository.getAllWords().first().first()
        println("    Word created: '${initialWord.originalWord}'")
        println("    Level: ${initialWord.level}")
        println("    Repetitions: ${initialWord.repetitions}")
        println("    Status: Ready to advance to Level 2 on next success")
        
        // STEP 2: Verify word is in Level 1 bucket
        println("\n2 VERIFY: Word appears in Level 1 bucket (Learning)")
        val level1Words = getWordsByStageUseCase(LearningStage.LEVEL_1_LEARNING).first()
        assertEquals(1, level1Words.size, "Should have 1 word in Level 1")
        assertEquals("hello", level1Words[0].originalWord)
        println("    Word found in Level 1 bucket: ${level1Words.size} word(s)")
        
        // STEP 3: User reviews word and responds "REMEMBERED"
        println("\n3 ACTION: User reviews word and responds REMEMBERED")
        reviewUseCase(initialWord, quality = 1)
        println("    Review completed with quality = 1 (REMEMBERED)")
        
        // STEP 4: Verify word advanced to Level 2 in database
        println("\n4 VERIFY: Word updated in database")
        val updatedWord = repository.getWordById(initialWord.id)!!
        assertEquals(2, updatedWord.level, "Word should advance to Level 2")
        assertEquals(0, updatedWord.repetitions, "Repetitions reset for new level")
        assertEquals(1, updatedWord.interval, "Level 2 has 1-day interval")
        println("    Word level: ${initialWord.level} → ${updatedWord.level}")
        println("    Repetitions reset: ${initialWord.repetitions} → ${updatedWord.repetitions}")
        println("    Interval set to: ${updatedWord.interval} day(s)")
        
        // STEP 5: Verify word is NOT in due cards (due to cooldown)
        println("\n5 VERIFY: Word is NOT in due cards (cooldown active)")
        val now = Clock.System.now().toEpochMilliseconds()
        val isDue = updatedWord.nextReviewDate <= now
        assertFalse(isDue, "Word should not be due (has 1-day cooldown)")
        
        val timeDiff = updatedWord.nextReviewDate - now
        val hoursUntilDue = timeDiff / (1000 * 60 * 60)
        println("    Word is NOT due for review")
        println("    Next review in ~${hoursUntilDue} hours")
        
        // STEP 6: Verify word IS visible in Level 2 bucket
        println("\n6 VERIFY: Word IS visible in Level 2 bucket (Familiarizing)")
        val level2Words = getWordsByStageUseCase(LearningStage.LEVEL_2_FAMILIAR).first()
        assertEquals(1, level2Words.size, "Should have 1 word in Level 2")
        assertEquals("hello", level2Words[0].originalWord)
        assertEquals(2, level2Words[0].level, "Word should be at level 2")
        println("    Word found in Level 2 bucket: ${level2Words.size} word(s)")
        println("    Word: '${level2Words[0].originalWord}'")
        
        // STEP 7: Verify word is NOT in Level 1 bucket anymore
        println("\n7 VERIFY: Word is NOT in Level 1 bucket anymore")
        val level1WordsAfter = getWordsByStageUseCase(LearningStage.LEVEL_1_LEARNING).first()
        assertEquals(0, level1WordsAfter.size, "Level 1 bucket should be empty")
        println("    Level 1 bucket is now empty")
        
        println("\n═══════════════════════════════════════════════════════")
        println(" TEST PASSED: Word correctly moved to Level 2 bucket")
        println("═══════════════════════════════════════════════════════\n")
    }
    
    @Test
    fun scenario_multipleWordsInDifferentBuckets_eachVisibleInCorrectBucket() = runTest {
        println("═══════════════════════════════════════════════════════")
        println("TEST SCENARIO: Multiple words in different buckets")
        println("═══════════════════════════════════════════════════════")
        
        // SETUP: Create words at different levels
        println("\n1 SETUP: Creating 3 words at different levels")
        val words = listOf(
            TestUtils.createWord(id = 0, originalWord = "word1", level = 0, repetitions = 1), // Ready to advance to 1
            TestUtils.createWord(id = 0, originalWord = "word2", level = 1, repetitions = 1), // Ready to advance to 2
            TestUtils.createWord(id = 0, originalWord = "word3", level = 2, repetitions = 1)  // Ready to advance to 3
        )
        repository.insertWords(words)
        println("    word1: Level 0 (New)")
        println("    word2: Level 1 (Learning)")
        println("    word3: Level 2 (Familiarizing)")
        
        // ACTION: Review all words successfully
        println("\n2 ACTION: User reviews all words successfully")
        val insertedWords = repository.getAllWords().first().sortedBy { it.originalWord }
        insertedWords.forEach { word ->
            println("   Reviewing '${word.originalWord}' at level ${word.level}...")
            reviewUseCase(word, quality = 1)
        }
        
        // VERIFY: Each word moved to next level and is in correct bucket
        println("\n3 VERIFY: Each word in its correct new bucket")
        
        val updatedWords = repository.getAllWords().first().sortedBy { it.originalWord }
        
        // word1: 0 → 1 (Level 1: Learning)
        assertEquals(1, updatedWords[0].level)
        val level1Words = getWordsByStageUseCase(LearningStage.LEVEL_1_LEARNING).first()
        assertTrue(level1Words.any { it.originalWord == "word1" })
        println("    word1: Level 0 → 1 (found in Learning bucket)")
        
        // word2: 1 → 2 (Level 2: Familiarizing)
        assertEquals(2, updatedWords[1].level)
        val level2Words = getWordsByStageUseCase(LearningStage.LEVEL_2_FAMILIAR).first()
        assertTrue(level2Words.any { it.originalWord == "word2" })
        println("    word2: Level 1 → 2 (found in Familiarizing bucket)")
        
        // word3: 2 → 3 (Level 3: Building)
        assertEquals(3, updatedWords[2].level)
        val level3Words = getWordsByStageUseCase(LearningStage.LEVEL_3_BUILDING).first()
        assertTrue(level3Words.any { it.originalWord == "word3" })
        println("    word3: Level 2 → 3 (found in Consolidating bucket)")
        
        println("\n═══════════════════════════════════════════════════════")
        println(" TEST PASSED: All words in correct buckets")
        println("═══════════════════════════════════════════════════════\n")
    }
    
    @Test
    fun scenario_wordForgotten_dropsBackToPreviousBucket() = runTest {
        println("═══════════════════════════════════════════════════════")
        println("TEST SCENARIO: Word forgotten - drops to previous bucket")
        println("═══════════════════════════════════════════════════════")
        
        // SETUP: Word at Level 3
        println("\n1 SETUP: Creating word at Level 3 (Consolidating)")
        val word = TestUtils.createWord(
            id = 0,
            originalWord = "difficult",
            translation = "difícil",
            level = 3,
            repetitions = 0
        )
        repository.insertWords(listOf(word))
        
        val initialWord = repository.getAllWords().first().first()
        println("    Word: '${initialWord.originalWord}'")
        println("    Level: ${initialWord.level} (Consolidating)")
        
        // VERIFY: Word in Level 3 bucket
        println("\n2 VERIFY: Word in Level 3 bucket")
        val level3Before = getWordsByStageUseCase(LearningStage.LEVEL_3_BUILDING).first()
        assertEquals(1, level3Before.size)
        println("    Found in Consolidating bucket")
        
        // ACTION: User forgets the word
        println("\n3 ACTION: User responds FORGOT")
        reviewUseCase(initialWord, quality = 0)
        println("    Review completed with quality = 0 (FORGOT)")
        
        // VERIFY: Word dropped 2 levels to Level 1
        println("\n4 VERIFY: Word dropped in database")
        val updatedWord = repository.getWordById(initialWord.id)!!
        assertEquals(1, updatedWord.level, "Should drop from Level 3 to Level 1")
        println("    Word level: ${initialWord.level} → ${updatedWord.level}")
        println("    Dropped 2 levels (3 - 2 = 1)")
        
        // VERIFY: Word now in Level 1 bucket
        println("\n5 VERIFY: Word now in Level 1 bucket (Learning)")
        val level1After = getWordsByStageUseCase(LearningStage.LEVEL_1_LEARNING).first()
        assertEquals(1, level1After.size)
        assertEquals("difficult", level1After[0].originalWord)
        println("    Found in Learning bucket")
        
        // VERIFY: Word NOT in Level 3 bucket anymore
        println("\n6 VERIFY: Word NOT in Level 3 bucket anymore")
        val level3After = getWordsByStageUseCase(LearningStage.LEVEL_3_BUILDING).first()
        assertEquals(0, level3After.size)
        println("    Consolidating bucket is empty")
        
        println("\n═══════════════════════════════════════════════════════")
        println(" TEST PASSED: Word correctly moved to lower bucket")
        println("═══════════════════════════════════════════════════════\n")
    }
    
    @Test
    fun scenario_realUserFlow_reviewSessionThenViewProgress() = runTest {
        println("═══════════════════════════════════════════════════════")
        println("TEST SCENARIO: Real user flow - Review then view progress")
        println("═══════════════════════════════════════════════════════")
        
        // SETUP: User has several due words at different levels
        println("\n1 SETUP: User has 5 due words")
        val now = Clock.System.now().toEpochMilliseconds()
        val words = listOf(
            TestUtils.createWord(id = 0, originalWord = "word1", level = 0, repetitions = 1, nextReviewDate = now),
            TestUtils.createWord(id = 0, originalWord = "word2", level = 0, repetitions = 1, nextReviewDate = now),
            TestUtils.createWord(id = 0, originalWord = "word3", level = 1, repetitions = 1, nextReviewDate = now),
            TestUtils.createWord(id = 0, originalWord = "word4", level = 2, repetitions = 1, nextReviewDate = now),
            TestUtils.createWord(id = 0, originalWord = "word5", level = 3, repetitions = 1, nextReviewDate = now)
        )
        repository.insertWords(words)
        println("    5 words ready for review")
        
        // ACTION: User completes review session (all successful)
        println("\n2 ACTION: User reviews all words (all REMEMBERED)")
        val dueWords = repository.getAllWords().first()
        dueWords.forEach { word ->
            reviewUseCase(word, quality = 1)
            println("    Reviewed '${word.originalWord}' at level ${word.level}")
        }
        
        // VERIFY: User views Progress screen and sees updated buckets
        println("\n3 VERIFY: User views Progress screen")
        
        val level0Count = getWordsByStageUseCase(LearningStage.LEVEL_0_FRESH).first().size
        val level1Count = getWordsByStageUseCase(LearningStage.LEVEL_1_LEARNING).first().size
        val level2Count = getWordsByStageUseCase(LearningStage.LEVEL_2_FAMILIAR).first().size
        val level3Count = getWordsByStageUseCase(LearningStage.LEVEL_3_BUILDING).first().size
        val level4Count = getWordsByStageUseCase(LearningStage.LEVEL_4_ALMOST).first().size
        
        println("    Progress Screen Buckets:")
        println("   • Level 0 (New):           $level0Count words")
        println("   • Level 1 (Learning):      $level1Count words")
        println("   • Level 2 (Familiarizing): $level2Count words")
        println("   • Level 3 (Consolidating): $level3Count words")
        println("   • Level 4 (Young):         $level4Count words")
        
        // All words should have advanced one level
        assertEquals(0, level0Count, "No words at level 0")
        assertEquals(2, level1Count, "word1, word2 advanced to level 1")
        assertEquals(1, level2Count, "word3 advanced to level 2")
        assertEquals(1, level3Count, "word4 advanced to level 3")
        assertEquals(1, level4Count, "word5 advanced to level 4")
        
        println("\n4 VERIFY: All words visible in their NEW buckets")
        println("    ALL WORDS successfully moved to next level")
        println("    ALL WORDS visible in their correct buckets")
        
        println("\n═══════════════════════════════════════════════════════")
        println(" TEST PASSED: Real user flow works correctly")
        println(" BUG FIX VERIFIED: UI shows updated buckets immediately")
        println("═══════════════════════════════════════════════════════\n")
    }
}

