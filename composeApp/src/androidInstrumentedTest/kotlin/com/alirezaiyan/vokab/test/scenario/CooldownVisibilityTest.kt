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
import domain.word.usecase.GetDueWordsUseCase
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
 * Critical Test: Verify words in cooldown ARE visible in bucket view
 * 
 * REQUIREMENT:
 * When a word is reviewed and enters cooldown period:
 * - Word should NOT appear in "Due Cards" (review session)
 * - Word SHOULD appear in its bucket (Progress/Level view)
 * 
 * This is the core of the user's requirement.
 */
@RunWith(AndroidJUnit4::class)
class CooldownVisibilityTest {
    
    private lateinit var database: AppDatabase
    private lateinit var repository: WordRepositoryImpl
    private lateinit var reviewUseCase: ReviewWordUseCase
    private lateinit var getDueWordsUseCase: GetDueWordsUseCase
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
        getDueWordsUseCase = GetDueWordsUseCase(repository)
        getWordsByStageUseCase = GetWordsByStageUseCase(repository)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun criticalTest_wordInCooldown_notInDueCards_butVisibleInBucket() = runTest {
        println("\n" + "═".repeat(60))
        println("CRITICAL TEST: Cooldown word visibility")
        println("═".repeat(60))
        
        // GIVEN: Word at Level 1, ready to advance, and DUE for review
        println("\n1️⃣  SETUP: Word at Level 1, due for review")
        val now = Clock.System.now().toEpochMilliseconds()
        val word = TestUtils.createWord(
            id = 0,
            originalWord = "example",
            translation = "ejemplo",
            level = 1,
            repetitions = 1,
            nextReviewDate = now - 1000 // Due 1 second ago
        )
        repository.insertWords(listOf(word))
        
        val initialWord = repository.getAllWords().first().first()
        println("   Word: '${initialWord.originalWord}'")
        println("   Level: ${initialWord.level}")
        println("   Status: DUE for review")
        
        // VERIFY: Word appears in due cards
        println("\n2️⃣  BEFORE REVIEW: Verify word in due cards")
        val dueCardsBefore = getDueWordsUseCase().first()
        assertEquals(1, dueCardsBefore.size, "Word should be in due cards")
        println("   ✓ Word IS in due cards (${dueCardsBefore.size} cards)")
        
        // VERIFY: Word appears in Level 1 bucket
        println("\n3️⃣  BEFORE REVIEW: Verify word in Level 1 bucket")
        val level1Before = getWordsByStageUseCase(LearningStage.LEVEL_1_LEARNING).first()
        assertEquals(1, level1Before.size, "Word should be in Level 1 bucket")
        println("   ✓ Word IS in Level 1 bucket (${level1Before.size} words)")
        
        // ACTION: User reviews word successfully
        println("\n4️⃣  ACTION: User reviews word (REMEMBERED)")
        reviewUseCase(initialWord, quality = 1)
        println("   ✓ Review completed")
        
        // VERIFY: Word updated to Level 2
        println("\n5️⃣  VERIFY: Word advanced to Level 2")
        val updatedWord = repository.getWordById(initialWord.id)!!
        assertEquals(2, updatedWord.level, "Word should advance to Level 2")
        println("   ✓ Word level: ${initialWord.level} → ${updatedWord.level}")
        
        // VERIFY: Word is in COOLDOWN (not due)
        println("\n6️⃣  VERIFY: Word entered cooldown period")
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val isDue = updatedWord.nextReviewDate <= currentTime
        assertFalse(isDue, "Word should be in cooldown (not due)")
        val hoursUntilDue = (updatedWord.nextReviewDate - currentTime) / (1000 * 60 * 60)
        println("   ✓ Word is in COOLDOWN")
        println("   ✓ Next review in ~${hoursUntilDue} hours (1 day cooldown)")
        
        // CRITICAL CHECK 1: Word NOT in due cards
        println("\n7️⃣  CRITICAL: Word NOT in due cards (during cooldown)")
        val dueCardsAfter = getDueWordsUseCase().first()
        assertEquals(0, dueCardsAfter.size, "Cooldown word should NOT be in due cards")
        println("   ✓ Due cards: ${dueCardsBefore.size} → ${dueCardsAfter.size}")
        println("   ✓ Word correctly excluded from review session")
        
        // CRITICAL CHECK 2: Word IS visible in Level 2 bucket
        println("\n8️⃣  CRITICAL: Word IS visible in Level 2 bucket")
        val level2After = getWordsByStageUseCase(LearningStage.LEVEL_2_FAMILIAR).first()
        assertEquals(1, level2After.size, "Word MUST be visible in Level 2 bucket")
        assertEquals("example", level2After[0].originalWord)
        assertEquals(2, level2After[0].level)
        println("   ✓ Level 2 bucket contains ${level2After.size} word(s)")
        println("   ✓ Word '${level2After[0].originalWord}' is visible")
        println("   ✓ Word IS IN COOLDOWN but VISIBLE in bucket view")
        
        // VERIFY: Word NOT in Level 1 bucket anymore
        println("\n9️⃣  VERIFY: Word removed from Level 1 bucket")
        val level1After = getWordsByStageUseCase(LearningStage.LEVEL_1_LEARNING).first()
        assertEquals(0, level1After.size, "Word should not be in Level 1 anymore")
        println("   ✓ Level 1 bucket is now empty")
        
        println("\n" + "═".repeat(60))
        println("✅  TEST PASSED: COOLDOWN LOGIC IS CORRECT")
        println("   • Word in cooldown: NOT in due cards ✓")
        println("   • Word in cooldown: VISIBLE in bucket ✓")
        println("═".repeat(60) + "\n")
    }
    
    @Test
    fun multipleCooldownWords_allVisibleInTheirBuckets() = runTest {
        println("\n" + "═".repeat(60))
        println("TEST: Multiple cooldown words visibility")
        println("═".repeat(60))
        
        // GIVEN: 5 words, all due for review
        println("\n1️⃣  SETUP: 5 words due for review")
        val now = Clock.System.now().toEpochMilliseconds()
        val words = listOf(
            TestUtils.createWord(id = 0, originalWord = "word1", level = 0, repetitions = 1, nextReviewDate = now),
            TestUtils.createWord(id = 0, originalWord = "word2", level = 1, repetitions = 1, nextReviewDate = now),
            TestUtils.createWord(id = 0, originalWord = "word3", level = 2, repetitions = 1, nextReviewDate = now),
            TestUtils.createWord(id = 0, originalWord = "word4", level = 3, repetitions = 1, nextReviewDate = now),
            TestUtils.createWord(id = 0, originalWord = "word5", level = 4, repetitions = 1, nextReviewDate = now)
        )
        repository.insertWords(words)
        println("   ✓ 5 words created, all due for review")
        
        // VERIFY: All in due cards before review
        val dueCardsBefore = getDueWordsUseCase().first()
        assertEquals(5, dueCardsBefore.size)
        println("   ✓ Due cards before review: ${dueCardsBefore.size}")
        
        // ACTION: Review all words successfully
        println("\n2️⃣  ACTION: Review all 5 words (all REMEMBERED)")
        val allWords = repository.getAllWords().first()
        allWords.forEach { word ->
            reviewUseCase(word, quality = 1)
            println("   Reviewed '${word.originalWord}' at level ${word.level}")
        }
        
        // VERIFY: All advanced one level
        println("\n3️⃣  VERIFY: All words advanced one level")
        val updatedWords = repository.getAllWords().first().sortedBy { it.originalWord }
        assertEquals(1, updatedWords[0].level) // word1: 0→1
        assertEquals(2, updatedWords[1].level) // word2: 1→2
        assertEquals(3, updatedWords[2].level) // word3: 2→3
        assertEquals(4, updatedWords[3].level) // word4: 3→4
        assertEquals(5, updatedWords[4].level) // word5: 4→5
        println("   ✓ All words advanced to next level")
        
        // CRITICAL: No words in due cards (all in cooldown)
        println("\n4️⃣  CRITICAL: All words in cooldown, none in due cards")
        val dueCardsAfter = getDueWordsUseCase().first()
        assertEquals(0, dueCardsAfter.size, "All words should be in cooldown")
        println("   ✓ Due cards: ${dueCardsBefore.size} → ${dueCardsAfter.size}")
        println("   ✓ All words entered cooldown period")
        
        // CRITICAL: All words visible in their NEW buckets
        println("\n5️⃣  CRITICAL: All words visible in their new buckets")
        val level1 = getWordsByStageUseCase(LearningStage.LEVEL_1_LEARNING).first()
        val level2 = getWordsByStageUseCase(LearningStage.LEVEL_2_FAMILIAR).first()
        val level3 = getWordsByStageUseCase(LearningStage.LEVEL_3_BUILDING).first()
        val level4 = getWordsByStageUseCase(LearningStage.LEVEL_4_ALMOST).first()
        val level5 = getWordsByStageUseCase(LearningStage.LEVEL_5_STRONG).first()
        
        assertEquals(1, level1.size, "Level 1 should have word1")
        assertEquals(1, level2.size, "Level 2 should have word2")
        assertEquals(1, level3.size, "Level 3 should have word3")
        assertEquals(1, level4.size, "Level 4 should have word4")
        assertEquals(1, level5.size, "Level 5 should have word5")
        
        println("   ✓ Level 1: ${level1.size} word (word1) - IN COOLDOWN")
        println("   ✓ Level 2: ${level2.size} word (word2) - IN COOLDOWN")
        println("   ✓ Level 3: ${level3.size} word (word3) - IN COOLDOWN")
        println("   ✓ Level 4: ${level4.size} word (word4) - IN COOLDOWN")
        println("   ✓ Level 5: ${level5.size} word (word5) - IN COOLDOWN")
        
        println("\n" + "═".repeat(60))
        println("✅  TEST PASSED: All cooldown words visible in buckets")
        println("═".repeat(60) + "\n")
    }
    
    @Test
    fun mixedState_someDue_someInCooldown() = runTest {
        println("\n" + "═".repeat(60))
        println("TEST: Mixed state - some due, some in cooldown")
        println("═".repeat(60))
        
        // GIVEN: 4 words at Level 2
        // - 2 are due (ready to review)
        // - 2 are in cooldown (already reviewed recently)
        println("\n1️⃣  SETUP: 4 words at Level 2")
        val now = Clock.System.now().toEpochMilliseconds()
        val oneHourAgo = now - (60 * 60 * 1000)
        val oneHourFuture = now + (60 * 60 * 1000)
        val oneDayFuture = now + (24 * 60 * 60 * 1000)
        
        val words = listOf(
            TestUtils.createWord(id = 0, originalWord = "due1", level = 2, nextReviewDate = oneHourAgo),
            TestUtils.createWord(id = 0, originalWord = "due2", level = 2, nextReviewDate = now),
            TestUtils.createWord(id = 0, originalWord = "cooldown1", level = 2, nextReviewDate = oneHourFuture),
            TestUtils.createWord(id = 0, originalWord = "cooldown2", level = 2, nextReviewDate = oneDayFuture)
        )
        repository.insertWords(words)
        println("   ✓ 2 words due for review")
        println("   ✓ 2 words in cooldown")
        
        // VERIFY: Due cards shows only 2
        println("\n2️⃣  VERIFY: Due cards contains only due words")
        val dueCards = getDueWordsUseCase().first()
        assertEquals(2, dueCards.size, "Should have 2 due words")
        assertTrue(dueCards.any { it.originalWord == "due1" })
        assertTrue(dueCards.any { it.originalWord == "due2" })
        println("   ✓ Due cards: ${dueCards.size} words")
        println("   ✓ Contains: due1, due2")
        println("   ✓ Excludes: cooldown1, cooldown2")
        
        // CRITICAL: Bucket view shows ALL 4 words
        println("\n3️⃣  CRITICAL: Level 2 bucket shows ALL 4 words")
        val level2Words = getWordsByStageUseCase(LearningStage.LEVEL_2_FAMILIAR).first()
        assertEquals(4, level2Words.size, "Level 2 bucket should show ALL 4 words")
        println("   ✓ Level 2 bucket: ${level2Words.size} words")
        println("   ✓ Includes due words: due1, due2")
        println("   ✓ Includes cooldown words: cooldown1, cooldown2")
        
        val wordNames = level2Words.map { it.originalWord }.sorted()
        println("   ✓ Words in bucket: ${wordNames.joinToString(", ")}")
        
        println("\n" + "═".repeat(60))
        println("✅  TEST PASSED: Bucket shows both due and cooldown words")
        println("   • Due cards: Only due words ✓")
        println("   • Bucket view: All words (due + cooldown) ✓")
        println("═".repeat(60) + "\n")
    }
}

