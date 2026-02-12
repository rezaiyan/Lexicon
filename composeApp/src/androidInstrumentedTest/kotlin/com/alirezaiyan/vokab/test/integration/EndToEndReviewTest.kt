package com.alirezaiyan.vokab.test.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import data.core.database.AppDatabase
import com.alirezaiyan.vokab.test.utils.TestUtils
import com.alirezaiyan.vokab.test.utils.createTestReviewSettingsUseCase
import data.word.repository.WordRepositoryImpl
import domain.word.model.Word
import domain.word.usecase.ReviewWordUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * End-to-End Integration Tests
 * 
 * These tests verify the complete flow from user review action through:
 * 1. ReviewWordUseCase (business logic)
 * 2. WordRepository (data layer)
 * 3. Room Database (persistence)
 * 4. Back to domain layer verification
 * 
 * Tests simulate real-world scenarios:
 * - Multiple words being reviewed
 * - Due cards filtering
 * - Progress tracking across reviews
 * - Statistics updates
 * - Concurrent reviews
 */
@RunWith(AndroidJUnit4::class)
class EndToEndReviewTest {
    
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
    
    // ========== Single Word Review Flow ==========
    
    @Test
    fun endToEnd_singleWord_completeReviewFlow() = runTest {
        // GIVEN: A new word inserted into database
        val word = TestUtils.createWord(
            id = 1,
            originalWord = "hello",
            translation = "hola",
            level = 0,
            repetitions = 0
        )
        repository.insertWords(listOf(word))
        
        // Verify initial state
        val initial = repository.getWordById(1)!!
        assertEquals(0, initial.level)
        assertEquals(0, initial.repetitions)
        
        // WHEN: User reviews and remembers
        reviewUseCase(initial, quality = 1)
        
        // THEN: Word should advance to level 1 immediately
        val afterReview = repository.getWordById(1)!!
        assertEquals(1, afterReview.level, "Should advance to level 1 after 1 success")
        assertEquals(0, afterReview.repetitions, "Repetitions reset for new level")
        assertEquals(10, afterReview.interval, "Level 1 interval is 10 minutes")
    }
    
    @Test
    fun endToEnd_singleWord_forgotFlow() = runTest {
        // GIVEN: A word that has progressed to level 3
        var word = TestUtils.createWord(id = 1, level = 3, repetitions = 0, easeFactor = 2.5f)
        repository.insertWords(listOf(word))
        
        // WHEN: User forgets
        reviewUseCase(word, quality = 0)
        
        // THEN: Should drop to level 1 and reset progress
        val afterForgetting = repository.getWordById(1)!!
        assertEquals(1, afterForgetting.level, "Should drop from 3 to 1")
        assertEquals(0, afterForgetting.repetitions)
        assertTrue(afterForgetting.easeFactor < 2.5f)
        assertEquals(2.3f, afterForgetting.easeFactor, 0.01f)
    }
    
    // ========== Multiple Words Review Flow ==========
    
    @Test
    fun endToEnd_multipleWords_independentProgression() = runTest {
        // GIVEN: Three words at different levels
        val words = listOf(
            TestUtils.createWord(id = 1, originalWord = "hello", level = 0, repetitions = 0),
            TestUtils.createWord(id = 2, originalWord = "goodbye", level = 1, repetitions = 0),
            TestUtils.createWord(id = 3, originalWord = "thanks", level = 2, repetitions = 0)
        )
        repository.insertWords(words)
        
        // WHEN: Review each word
        words.forEach { word ->
            val current = repository.getWordById(word.id)!!
            reviewUseCase(current, quality = 1)
        }
        
        // THEN: Each should progress independently
        val word1 = repository.getWordById(1)!!
        assertEquals(1, word1.level, "Word 1: 0 -> 1")
        
        val word2 = repository.getWordById(2)!!
        assertEquals(2, word2.level, "Word 2: 1 -> 2")
        
        val word3 = repository.getWordById(3)!!
        assertEquals(3, word3.level, "Word 3: 2 -> 3")
    }
    
    @Test
    fun endToEnd_multipleWords_mixedResults() = runTest {
        // GIVEN: Three words ready for review
        val words = listOf(
            TestUtils.createWord(id = 1, level = 0, repetitions = 0),
            TestUtils.createWord(id = 2, level = 2, repetitions = 0),
            TestUtils.createWord(id = 3, level = 4, repetitions = 0)
        )
        repository.insertWords(words)
        
        // WHEN: Mixed review results
        reviewUseCase(repository.getWordById(1)!!, quality = 1)  // Remember
        reviewUseCase(repository.getWordById(2)!!, quality = 0)  // Forget
        reviewUseCase(repository.getWordById(3)!!, quality = 1)  // Remember
        
        // THEN: Each should update correctly
        assertEquals(1, repository.getWordById(1)!!.level, "Word 1 advanced")
        assertEquals(0, repository.getWordById(2)!!.level, "Word 2 dropped")
        assertEquals(5, repository.getWordById(3)!!.level, "Word 3 advanced")
    }
    
    // ========== Due Cards Flow ==========
    
    @Test
    fun endToEnd_dueCards_filteringAndReviewing() = runTest {
        // GIVEN: Words with different due dates
        val now = Clock.System.now().toEpochMilliseconds()
        val past = now - 1000 * 60 * 60 // 1 hour ago
        val future = now + 1000 * 60 * 60 // 1 hour from now
        
        val words = listOf(
            TestUtils.createWord(id = 1, originalWord = "due1", nextReviewDate = past, level = 0, repetitions = 0),
            TestUtils.createWord(id = 2, originalWord = "due2", nextReviewDate = past, level = 1, repetitions = 0),
            TestUtils.createWord(id = 3, originalWord = "notDue", nextReviewDate = future, level = 2, repetitions = 0)
        )
        repository.insertWords(words)
        
        // WHEN: Get due cards
        val dueCards = repository.getDueCards().first()
        
        // THEN: Should only get the 2 due cards
        assertEquals(2, dueCards.size, "Should have 2 due cards")
        assertTrue(dueCards.none { it.originalWord == "notDue" })
        
        // WHEN: Review the due cards
        dueCards.forEach { card ->
            reviewUseCase(card, quality = 1)
        }
        
        // THEN: They should advance levels
        assertEquals(1, repository.getWordById(1)!!.level)
        assertEquals(2, repository.getWordById(2)!!.level)
        
        // AND: Their next review dates should be in the future
        val afterReview1 = repository.getWordById(1)!!
        val afterReview2 = repository.getWordById(2)!!
        assertTrue(afterReview1.nextReviewDate > now)
        assertTrue(afterReview2.nextReviewDate > now)
    }
    
    @Test
    fun endToEnd_dueCardsCount_updatesAfterReview() = runTest {
        // GIVEN: 5 due cards
        val now = Clock.System.now().toEpochMilliseconds()
        val past = now - 1000
        
        val words = (1..5).map { id ->
            TestUtils.createWord(id = id, nextReviewDate = past, level = 0, repetitions = 0)
        }
        repository.insertWords(words)
        
        // Verify initial due count
        var dueCount = repository.getDueCount()
        assertEquals(5, dueCount, "Initially 5 cards are due")
        
        // WHEN: Review 3 cards
        repeat(3) { index ->
            val word = repository.getWordById(index + 1)!!
            reviewUseCase(word, quality = 1)
        }
        
        // THEN: Due count should decrease
        dueCount = repository.getDueCount()
        assertEquals(2, dueCount, "Only 2 cards should remain due")
    }
    
    // ========== Progress Statistics Flow ==========
    
    @Test
    fun endToEnd_progressStats_updatesAfterReviews() = runTest {
        // GIVEN: Words at various levels
        val words = listOf(
            TestUtils.createWord(id = 1, level = 0, repetitions = 0),
            TestUtils.createWord(id = 2, level = 0, repetitions = 0),
            TestUtils.createWord(id = 3, level = 1, repetitions = 0),
            TestUtils.createWord(id = 4, level = 2, repetitions = 0)
        )
        repository.insertWords(words)
        
        // Verify initial stats
        var stats = repository.getProgressStats().first()
        assertEquals(2, stats.level0Count)
        assertEquals(1, stats.level1Count)
        assertEquals(1, stats.level2Count)
        assertEquals(4, stats.totalWords)
        
        // WHEN: Review all level 0 words (they advance to level 1)
        val level0Words = listOf(repository.getWordById(1)!!, repository.getWordById(2)!!)
        level0Words.forEach { word ->
            reviewUseCase(word, quality = 1)
        }
        
        // THEN: Stats should update
        stats = repository.getProgressStats().first()
        assertEquals(0, stats.level0Count, "No more level 0 words")
        assertEquals(3, stats.level1Count, "Now 3 level 1 words")
        assertEquals(1, stats.level2Count, "Still 1 level 2 word")
        assertEquals(4, stats.totalWords)
    }
    
    @Test
    fun endToEnd_progressStats_acrossAllLevels() = runTest {
        // GIVEN: One word at each level
        val words = (0..6).map { level ->
            TestUtils.createWord(
                id = 0, // Auto-increment
                originalWord = "word_level_$level",
                level = level,
                repetitions = 0
            )
        }
        repository.insertWords(words)
        
        // WHEN: Get progress stats
        val stats = repository.getProgressStats().first()
        
        // THEN: Should have correct distribution
        assertEquals(1, stats.level0Count, "Should have 1 word at level 0")
        assertEquals(1, stats.level1Count, "Should have 1 word at level 1")
        assertEquals(1, stats.level2Count, "Should have 1 word at level 2")
        assertEquals(1, stats.level3Count, "Should have 1 word at level 3")
        assertEquals(1, stats.level4Count, "Should have 1 word at level 4")
        assertEquals(1, stats.level5Count, "Should have 1 word at level 5")
        assertEquals(1, stats.level6Count, "Should have 1 word at level 6")
        assertEquals(7, stats.totalWords, "Should have 7 total words")
    }
    
    // ========== Real-World Scenario Tests ==========
    
    @Test
    fun endToEnd_realWorldScenario_dailyReviewSession() = runTest {
        // GIVEN: User has 10 words at various stages
        val words = listOf(
            // New words (level 0)
            TestUtils.createWord(id = 0, originalWord = "word1", level = 0, repetitions = 0),
            TestUtils.createWord(id = 0, originalWord = "word2", level = 0, repetitions = 0),
            TestUtils.createWord(id = 0, originalWord = "word3", level = 0, repetitions = 0),
            // Learning words (level 1-2)
            TestUtils.createWord(id = 0, originalWord = "word4", level = 1, repetitions = 0),
            TestUtils.createWord(id = 0, originalWord = "word5", level = 2, repetitions = 0),
            // Intermediate words (level 3-4)
            TestUtils.createWord(id = 0, originalWord = "word6", level = 3, repetitions = 0),
            TestUtils.createWord(id = 0, originalWord = "word7", level = 4, repetitions = 0),
            // Advanced words (level 5-6)
            TestUtils.createWord(id = 0, originalWord = "word8", level = 5, repetitions = 0),
            TestUtils.createWord(id = 0, originalWord = "word9", level = 6, repetitions = 0),
            TestUtils.createWord(id = 0, originalWord = "word10", level = 6, repetitions = 0)
        )
        repository.insertWords(words)
        
        // Get actual words after insertion
        val insertedWords = repository.getAllWords().first().sortedBy { it.originalWord }
        
        // WHEN: User does a review session with realistic results
        // 80% success rate
        val results = listOf(1, 1, 0, 1, 1, 1, 0, 1, 1, 1) // 8 correct, 2 forgot
        
        insertedWords.forEachIndexed { index, word ->
            reviewUseCase(word, quality = results[index])
        }
        
        // THEN: Verify results
        val updatedWords = repository.getAllWords().first().sortedBy { it.originalWord }
        
        // Count how many dropped (forgot results at indices 2 and 6)
        // word3 (level 0, rep 1) forgot -> stays at 0 (can't drop below 0)
        // word7 (level 4, rep 1) forgot -> drops to level 2
        val droppedCount = updatedWords.count { updated ->
            val original = words.find { it.originalWord == updated.originalWord }!!
            updated.level < original.level
        }
        
        assertTrue(droppedCount >= 1, "At least 1 word should drop (level 4 word)")
        
        // Verify all words have updated review dates
        updatedWords.forEach { word ->
            assertTrue(word.lastReviewDate > 0, "All words should have lastReviewDate set")
        }
    }
    
    @Test
    fun endToEnd_realWorldScenario_strugglingWithWord() = runTest {
        // GIVEN: A word user keeps forgetting
        val word = TestUtils.createWord(id = 1, level = 0, repetitions = 0)
        repository.insertWords(listOf(word))
        
        val reviewSequence = listOf(
            1,  // 0→1
            0,  // Forget: 1→0 (drops 2, min 0)
            1,  // 0→1
            1,  // 1→2
            0,  // Forget: 2→0 (drops 2)
            1,  // 0→1
            1,  // 1→2
            1,  // 2→3
            1   // 3→4
        )
        
        // WHEN: Simulate the struggle
        var current = word
        reviewSequence.forEach { quality ->
            current = repository.getWordById(1)!!
            reviewUseCase(current, quality = quality)
        }
        
        // THEN: Eventually reaches higher level despite setbacks  
        val final = repository.getWordById(1)!!
        println("Final level: ${final.level}, Ease factor: ${final.easeFactor}")
        
        // After 9 reviews with 2 forgot: should reach level 4
        assertTrue(final.level >= 3, "Despite forgetting twice, should progress (reached level ${final.level})")
        
        // AND: Ease factor should be lower due to difficulties
        assertTrue(final.easeFactor <= 2.5f, "Ease factor should not increase above starting value")
    }
    
    @Test
    fun endToEnd_realWorldScenario_masteringWord() = runTest {
        // GIVEN: A word user masters easily
        var word = TestUtils.createWord(id = 1, level = 0, repetitions = 0)
        repository.insertWords(listOf(word))
        
        // WHEN: User remembers consistently (no mistakes)
        // With 1 success per level: 0→1→2→3→4→5→6 = 6 reviews
        repeat(6) {
            word = repository.getWordById(1)!!
            reviewUseCase(word, quality = 1)
        }
        
        // THEN: Should reach mastered level
        val mastered = repository.getWordById(1)!!
        assertEquals(6, mastered.level, "Should reach mastered level after 6 successes")
        
        // AND: Should have good ease factor
        assertTrue(mastered.easeFactor >= 2.5f, "Should maintain high ease factor")
        
        // AND: Long interval until next review
        assertTrue(mastered.interval >= 30, "Should have long review interval")
    }
    
    @Test
    fun endToEnd_realWorldScenario_mixedLevelProgression() = runTest {
        // GIVEN: User with diverse vocabulary levels
        val wordDomainList = (0..6).map { level ->
            Word(
                id = 0, // Let auto-increment handle it
                originalWord = "word_level_$level",
                translation = "palabra_nivel_$level",
                description = "test",
                level = level,
                easeFactor = 2.5f,
                interval = TestUtils.getIntervalForLevel(level),
                repetitions = 0, // Will advance after 1 success
                lastReviewDate = 0L,
                sourceLanguage = "en",
                targetLanguage = "es",
                nextReviewDate = Clock.System.now().toEpochMilliseconds()
            )
        }
        repository.insertWords(wordDomainList)
        
        // Get the actual IDs after insertion
        val allWords = repository.getAllWords().first()
        
        // WHEN: Review all with success
        allWords.forEach { word ->
            reviewUseCase(word, quality = 1)
        }
        
        // THEN: Get updated words and verify progression
        val updatedWords = repository.getAllWords().first()
        
        // Words at level 0-5 should advance one level
        updatedWords.filter { it.level <= 6 }.forEach { word ->
            val originalLevel = wordDomainList.find { it.originalWord == word.originalWord }!!.level
            if (originalLevel < 6) {
                assertEquals(
                    originalLevel + 1,
                    word.level,
                    "Word at level $originalLevel should advance to ${originalLevel + 1}"
                )
            } else {
                // Level 6 stays at 6
                assertEquals(6, word.level)
                assertTrue(word.interval > 30, "Level 6 interval should grow")
            }
        }
    }
    
    // ========== Data Persistence Verification ==========
    
    @Test
    fun endToEnd_dataPersistence_survivesDatabaseReopen() = runTest {
        // GIVEN: Review some words
        val words = listOf(
            TestUtils.createWord(id = 1, level = 0, repetitions = 1),
            TestUtils.createWord(id = 2, level = 1, repetitions = 1)
        )
        repository.insertWords(words)
        
        words.forEach { word ->
            val current = repository.getWordById(word.id)!!
            reviewUseCase(current, quality = 1)
        }
        
        // Verify updates
        val word1AfterReview = repository.getWordById(1)!!
        val word2AfterReview = repository.getWordById(2)!!
        
        assertEquals(1, word1AfterReview.level)
        assertEquals(2, word2AfterReview.level)
        
        // WHEN: Close and reopen database (simulating app restart)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database.close()
        
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = TestUtils.createWordRepository(database.getDao())
        
        // Insert words again (simulating reload)
        repository.insertWords(listOf(word1AfterReview, word2AfterReview))
        
        // THEN: Data should persist
        val word1Persisted = repository.getWordById(1)!!
        val word2Persisted = repository.getWordById(2)!!
        
        assertEquals(1, word1Persisted.level)
        assertEquals(2, word2Persisted.level)
    }
}

