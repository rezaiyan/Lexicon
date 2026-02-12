package com.alirezaiyan.vokab.test.bug

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import data.core.database.AppDatabase
import com.alirezaiyan.vokab.test.utils.TestUtils
import com.alirezaiyan.vokab.test.utils.createTestReviewSettingsUseCase
import data.word.repository.WordRepositoryImpl
import domain.word.usecase.ReviewWordUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Test to reproduce and verify the bucket progression bug
 * 
 * BUG DESCRIPTION:
 * When a user reviews a word, the word is correctly updated in the database,
 * but the UI doesn't reflect the change immediately. The word appears to stay
 * in the same bucket instead of moving to the next bucket.
 * 
 * ROOT CAUSE:
 * VocabularyViewModel takes a snapshot of words and cancels the Flow collection,
 * so database updates don't propagate to the UI.
 * 
 * EXPECTED BEHAVIOR:
 * After reviewing a word twice (2 successes), it should:
 * 1. Advance to the next level in the database
 * 2. Appear in the next bucket in the UI
 * 3. Be visible immediately without manual refresh
 */
@RunWith(AndroidJUnit4::class)
class BucketProgressionBugTest {
    
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
    
    @Test
    fun bugRepro_wordAppearsinNextBucket_afterOneSuccess() = runTest {
        // GIVEN: A new word at level 0
        val word = TestUtils.createWord(
            id = 0,
            originalWord = "hello",
            translation = "hola",
            level = 0,
            repetitions = 0
        )
        repository.insertWords(listOf(word))
        
        // Get the actual inserted word
        val insertedWord = repository.getAllWords().first().first()
        assertEquals(0, insertedWord.level, "Word starts at level 0")
        assertEquals(0, insertedWord.repetitions, "Word starts with 0 repetitions")
        
        // WHEN: User reviews the word successfully
        println(">>> First review (REMEMBERED)")
        reviewUseCase(insertedWord, quality = 1)
        
        // THEN: Word should advance to level 1 immediately
        val afterReview = repository.getWordById(insertedWord.id)!!
        assertEquals(1, afterReview.level, "Should advance to level 1 after 1 success")
        assertEquals(0, afterReview.repetitions, "Repetitions reset for new level")
        
        // VERIFY: The word's level actually changed in the database
        assertNotEquals(
            insertedWord.level,
            afterReview.level,
            "Level should have changed from 0 to 1"
        )
        
        println("✅ Database updates correctly!")
        println("✅ Word advances after just 1 REMEMBERED response")
    }
    
    @Test
    fun bugRepro_multipleWords_allAdvance() = runTest {
        // GIVEN: Three words at level 0, all with 0 repetitions
        val words = listOf(
            TestUtils.createWord(id = 0, originalWord = "word1", level = 0, repetitions = 0),
            TestUtils.createWord(id = 0, originalWord = "word2", level = 0, repetitions = 0),
            TestUtils.createWord(id = 0, originalWord = "word3", level = 0, repetitions = 0)
        )
        repository.insertWords(words)
        
        val insertedWords = repository.getAllWords().first().sortedBy { it.originalWord }
        
        // WHEN: Review all words successfully
        insertedWords.forEach { word ->
            reviewUseCase(word, quality = 1)
        }
        
        // THEN: All words should advance to level 1
        val updatedWords = repository.getAllWords().first().sortedBy { it.originalWord }
        
        // All advance after 1 success
        assertEquals(1, updatedWords[0].level, "word1 should advance to level 1")
        assertEquals(0, updatedWords[0].repetitions, "word1 reps reset")
        
        assertEquals(1, updatedWords[1].level, "word2 should advance to level 1")
        assertEquals(0, updatedWords[1].repetitions, "word2 reps reset")
        
        assertEquals(1, updatedWords[2].level, "word3 should advance to level 1")
        assertEquals(0, updatedWords[2].repetitions, "word3 reps reset")
        
        println("✅ All words advanced after 1 REMEMBERED - correct behavior!")
    }
    
    @Test
    fun bugRepro_wordMovesThrough_MultipleBuckets() = runTest {
        // GIVEN: A word at level 0
        val word = TestUtils.createWord(id = 0, level = 0, repetitions = 0)
        repository.insertWords(listOf(word))
        
        var currentWord = repository.getAllWords().first().first()
        val startLevel = currentWord.level
        
        // WHEN: Review successfully 6 times (should reach level 6 with 1-success advancement)
        repeat(6) { reviewNumber ->
            println("Review ${reviewNumber + 1}: Level ${currentWord.level}, Reps ${currentWord.repetitions}")
            
            reviewUseCase(currentWord, quality = 1)
            currentWord = repository.getWordById(currentWord.id)!!
        }
        
        // THEN: Word should have reached level 6
        val finalLevel = currentWord.level
        println("Final state: Level $finalLevel")
        
        // Should reach level 6 after 6 successful reviews (0→1→2→3→4→5→6)
        assertEquals(6, finalLevel, "Should reach level 6 after 6 successful reviews")
        assertTrue(finalLevel > startLevel, "Level should have increased from $startLevel to $finalLevel")
        
        println("✅ Word correctly progressed through all buckets (0→6)")
        println("✅ Only 6 reviews needed (1 success per level)")
    }
    
    @Test
    fun bugRepro_forgotResponse_dropsLevels() = runTest {
        // GIVEN: A word at level 4
        val word = TestUtils.createWord(id = 0, level = 4, repetitions = 0)
        repository.insertWords(listOf(word))
        
        val insertedWord = repository.getAllWords().first().first()
        assertEquals(4, insertedWord.level)
        
        // WHEN: User forgets the word
        reviewUseCase(insertedWord, quality = 0)
        
        // THEN: Word should drop 2 levels to level 2
        val afterForgetting = repository.getWordById(insertedWord.id)!!
        assertEquals(2, afterForgetting.level, "Should drop from level 4 to level 2")
        assertEquals(0, afterForgetting.repetitions, "Repetitions should reset")
        
        println("✅ FORGOT correctly drops word 2 levels")
    }
}

